package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.AppConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

/**
 * Wasabi/S3-compatible object storage utility.
 * Reads credentials from ssa.properties via AppConfig: S3_ENDPOINT, S3_BUCKET, S3_ACCESS_KEY, S3_SECRET_KEY.
 * Files are stored under a PSP-specific prefix (slugified PSP name).
 *
 * <p>Uses a singleton S3Client and S3Presigner (both thread-safe) with configured
 * timeouts and retry policy. Clients are lazily initialized on first use.</p>
 *
 * NOTE: The EntityManager parameter is retained on public methods for caller compatibility
 * but is no longer used internally. S3 config now comes from ssa.properties, not the DB.
 */
public abstract class StorageDAO {

    private static final Logger log = LogManager.getLogger(StorageDAO.class);

    // ── Singleton holders (lazy, thread-safe) ──────────────────────────────

    private static volatile S3Client s3Client;
    private static volatile S3Presigner s3Presigner;
    private static final Object CLIENT_LOCK = new Object();
    private static final Object PRESIGNER_LOCK = new Object();

    // ── Upload result ──────────────────────────────────────────────────────

    /**
     * Result of an upload operation. Use {@link #uploadFileSafe} to get this
     * instead of an exception on failure.
     */
    public static class UploadResult {
        public final boolean success;
        public final String errorMessage;
        public final long elapsedMillis;

        private UploadResult(boolean success, String errorMessage, long elapsedMillis) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.elapsedMillis = elapsedMillis;
        }

        public static UploadResult ok(long elapsedMillis) {
            return new UploadResult(true, null, elapsedMillis);
        }

        public static UploadResult error(String message, long elapsedMillis) {
            return new UploadResult(false, message, elapsedMillis);
        }
    }

    // ── Public upload methods ──────────────────────────────────────────────

    /**
     * Uploads a file to S3/Wasabi storage.
     * Throws on failure (preserves existing caller behavior).
     *
     * @param em          EntityManager (retained for caller compatibility — not used internally)
     * @param pspName     PSP full name (will be slugified for the folder prefix)
     * @param objectKey   the file name (e.g., UUID.extension)
     * @param displayName the human-readable file name for downloads
     * @param inputStream file content
     * @param contentLength file size in bytes
     * @param contentType MIME type (e.g., "application/pdf")
     */
    public static void uploadFile(EntityManager em, String pspName, String objectKey, String displayName,
                                  InputStream inputStream, long contentLength, String contentType) {
        UploadResult result = uploadFileSafe(em, pspName, objectKey, displayName,
                inputStream, contentLength, contentType);
        if (!result.success) {
            throw new RuntimeException("S3 upload failed: " + result.errorMessage);
        }
    }

    /**
     * Uploads a file to S3/Wasabi storage, returning an {@link UploadResult}
     * instead of throwing on failure. Use this for callers that want to handle
     * errors gracefully.
     *
     * @param em          EntityManager (retained for caller compatibility — not used internally)
     * @param pspName     PSP full name (will be slugified for the folder prefix)
     * @param objectKey   the file name (e.g., UUID.extension)
     * @param displayName the human-readable file name for downloads
     * @param inputStream file content
     * @param contentLength file size in bytes
     * @param contentType MIME type (e.g., "application/pdf")
     * @return UploadResult indicating success or failure with details
     */
    public static UploadResult uploadFileSafe(EntityManager em, String pspName, String objectKey,
                                              String displayName, InputStream inputStream,
                                              long contentLength, String contentType) {
        String fullKey = slugify(pspName) + "/" + objectKey;
        String bucket = getConfig("S3_BUCKET");
        String disposition = "attachment; filename=\"" + sanitizeFileName(displayName) + "\"";

        log.info("Upload starting: key={}, size={} bytes, type={}", fullKey, contentLength, contentType);
        long start = System.currentTimeMillis();

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fullKey)
                    .contentType(contentType)
                    .contentDisposition(disposition)
                    .build();

            // Read into byte array to avoid Expect: 100-continue issues with S3-compatible services
            byte[] data = inputStream.readAllBytes();
            getClient().putObject(putRequest, RequestBody.fromBytes(data));

            long elapsed = System.currentTimeMillis() - start;
            log.info("Upload complete: key={}, elapsed={}ms", fullKey, elapsed);
            return UploadResult.ok(elapsed);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("Upload failed: key={}, elapsed={}ms — {}", fullKey, elapsed, e.getMessage(), e);
            // Reset singleton on connection/timeout failures so next call gets a fresh client
            resetClientOnConnectionError(e);
            return UploadResult.error(e.getMessage(), elapsed);
        }
    }

    // ── Pre-signed URL methods ─────────────────────────────────────────────

    /**
     * Generates a pre-signed download URL for a stored file.
     *
     * @param em        EntityManager (retained for caller compatibility — not used internally)
     * @param pspName   PSP full name (will be slugified for the folder prefix)
     * @param objectKey the file name (e.g., UUID.extension)
     * @param duration  how long the URL is valid
     * @return pre-signed URL string
     */
    public static String getDownloadUrl(EntityManager em, String pspName, String objectKey, Duration duration) {
        String fullKey = slugify(pspName) + "/" + objectKey;
        String bucket = getConfig("S3_BUCKET");

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(fullKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(getRequest)
                .build();

        PresignedGetObjectRequest presigned = getPresigner().presignGetObject(presignRequest);
        String url = presigned.url().toString();
        log.debug("Pre-signed URL generated for: {}", fullKey);
        return url;
    }

    /**
     * Convenience overload — generates a download URL valid for 1 hour.
     */
    public static String getDownloadUrl(EntityManager em, String pspName, String objectKey) {
        return getDownloadUrl(em, pspName, objectKey, Duration.ofHours(1));
    }

    // ── Delete ─────────────────────────────────────────────────────────────

    /**
     * Deletes a file from S3/Wasabi storage.
     *
     * @param em        EntityManager (retained for caller compatibility — not used internally)
     * @param pspName   PSP full name (will be slugified for the folder prefix)
     * @param objectKey the file name (e.g., UUID.extension)
     */
    public static void deleteFile(EntityManager em, String pspName, String objectKey) {
        String fullKey = slugify(pspName) + "/" + objectKey;
        String bucket = getConfig("S3_BUCKET");

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(fullKey)
                .build();

        getClient().deleteObject(deleteRequest);
        log.info("Deleted: {}", fullKey);
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    /**
     * Shuts down the singleton S3Client and S3Presigner.
     * Call from a {@code ServletContextListener} on app shutdown, or let the
     * JVM handle cleanup naturally.
     */
    public static void shutdown() {
        synchronized (CLIENT_LOCK) {
            if (s3Client != null) {
                try { s3Client.close(); } catch (Exception ignored) {}
                s3Client = null;
                log.info("S3Client shut down");
            }
        }
        synchronized (PRESIGNER_LOCK) {
            if (s3Presigner != null) {
                try { s3Presigner.close(); } catch (Exception ignored) {}
                s3Presigner = null;
                log.info("S3Presigner shut down");
            }
        }
    }

    // ── Internal: error recovery ────────────────────────────────────────────

    /**
     * If the exception indicates a connection-level or timeout failure,
     * reset the singleton client so the next call builds a fresh one.
     */
    private static void resetClientOnConnectionError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        boolean isConnectionError = e instanceof java.net.ConnectException
                || e instanceof java.net.SocketTimeoutException
                || e instanceof software.amazon.awssdk.core.exception.ApiCallTimeoutException
                || e instanceof software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException
                || msg.contains("connection") || msg.contains("timed out") || msg.contains("timeout");
        if (isConnectionError) {
            log.warn("Resetting S3Client singleton after connection error");
            synchronized (CLIENT_LOCK) {
                if (s3Client != null) {
                    try { s3Client.close(); } catch (Exception ignored) {}
                    s3Client = null;
                }
            }
        }
    }

    // ── Internal: singleton accessors ──────────────────────────────────────

    private static S3Client getClient() {
        S3Client client = s3Client;
        if (client == null) {
            synchronized (CLIENT_LOCK) {
                client = s3Client;
                if (client == null) {
                    client = buildClient();
                    s3Client = client;
                    log.info("S3Client initialized (endpoint={})", getConfig("S3_ENDPOINT"));
                }
            }
        }
        return client;
    }

    private static S3Presigner getPresigner() {
        S3Presigner presigner = s3Presigner;
        if (presigner == null) {
            synchronized (PRESIGNER_LOCK) {
                presigner = s3Presigner;
                if (presigner == null) {
                    presigner = buildPresigner();
                    s3Presigner = presigner;
                    log.info("S3Presigner initialized");
                }
            }
        }
        return presigner;
    }

    // ── Internal: builders ─────────────────────────────────────────────────

    private static S3Client buildClient() {
        return S3Client.builder()
                .endpointOverride(URI.create(getConfig("S3_ENDPOINT")))
                .region(Region.US_EAST_1)
                .forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                getConfig("S3_ACCESS_KEY"),
                                getConfig("S3_SECRET_KEY"))))
                .overrideConfiguration(c -> c
                        .apiCallTimeout(Duration.ofSeconds(120)))
                .httpClientBuilder(software.amazon.awssdk.http.apache.ApacheHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(10))
                        .socketTimeout(Duration.ofSeconds(60))
                        .maxConnections(10)
                        .connectionTimeToLive(Duration.ofMinutes(5))
                        .tcpKeepAlive(true))
                .build();
    }

    private static S3Presigner buildPresigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(getConfig("S3_ENDPOINT")))
                .region(Region.US_EAST_1)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                getConfig("S3_ACCESS_KEY"),
                                getConfig("S3_SECRET_KEY"))))
                .build();
    }

    // ── Internal: config + helpers ─────────────────────────────────────────

    /**
     * Reads an S3 config value from ssa.properties via AppConfig.
     * Throws IllegalStateException if not configured.
     */
    private static String getConfig(String name) {
        String value = AppConfig.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing ssa.properties config: " + name
                    + " — add it to your ssa.properties file");
        }
        return value;
    }

    /**
     * Converts a PSP name to a URL-safe folder prefix.
     * "Superior State Administrators" -> "superior-state-administrators"
     */
    static String slugify(String name) {
        if (name == null || name.isBlank()) return "default";
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    /**
     * Sanitizes a display filename for use in Content-Disposition header.
     */
    private static String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) return "download";
        return name.replaceAll("[\"\\\\/:*?<>|]", "_");
    }
}
