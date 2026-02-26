package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.AppConfig;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

/**
 * Wasabi/S3-compatible object storage utility.
 * Reads credentials from ssa.properties via AppConfig: S3_ENDPOINT, S3_BUCKET, S3_ACCESS_KEY, S3_SECRET_KEY.
 * Files are stored under a PSP-specific prefix (slugified PSP name).
 *
 * NOTE: The EntityManager parameter is retained on public methods for caller compatibility
 * but is no longer used internally. S3 config now comes from ssa.properties, not the DB.
 */
public abstract class StorageDAO {

    /**
     * Uploads a file to S3/Wasabi storage.
     *
     * @param em          EntityManager (retained for caller compatibility — not used internally)
     * @param pspName     PSP full name (will be slugified for the folder prefix)
     * @param objectKey   the file name (e.g., UUID.extension)
     * @param displayName the human-readable file name for downloads (e.g., "Benefits_Summary.pdf")
     * @param inputStream file content
     * @param contentLength file size in bytes
     * @param contentType MIME type (e.g., "application/pdf")
     */
    public static void uploadFile(EntityManager em, String pspName, String objectKey, String displayName,
                                  InputStream inputStream, long contentLength, String contentType) {

        String fullKey = slugify(pspName) + "/" + objectKey;
        String bucket = getConfig("S3_BUCKET");

        // Content-Disposition tells the browser what filename to use on download
        String disposition = "attachment; filename=\"" + sanitizeFileName(displayName) + "\"";

        S3Client s3 = buildClient();
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fullKey)
                    .contentType(contentType)
                    .contentDisposition(disposition)
                    .build();

            s3.putObject(putRequest, RequestBody.fromInputStream(inputStream, contentLength));
            System.out.println("[StorageDAO] Uploaded: " + fullKey);
        } finally {
            s3.close();
        }
    }

    /**
     * Generates a pre-signed download URL for a stored file.
     * URL is valid for the specified duration.
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

        S3Presigner presigner = buildPresigner();
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(fullKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(duration)
                    .getObjectRequest(getRequest)
                    .build();

            PresignedGetObjectRequest presigned = presigner.presignGetObject(presignRequest);
            String url = presigned.url().toString();
            System.out.println("[StorageDAO] Pre-signed URL generated for: " + fullKey);
            return url;
        } finally {
            presigner.close();
        }
    }

    /**
     * Convenience overload — generates a download URL valid for 1 hour.
     */
    public static String getDownloadUrl(EntityManager em, String pspName, String objectKey) {
        return getDownloadUrl(em, pspName, objectKey, Duration.ofHours(1));
    }

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

        S3Client s3 = buildClient();
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(fullKey)
                    .build();

            s3.deleteObject(deleteRequest);
            System.out.println("[StorageDAO] Deleted: " + fullKey);
        } finally {
            s3.close();
        }
    }

    // ----------------------------- internal helpers -----------------------------

    private static S3Client buildClient() {
        return S3Client.builder()
                .endpointOverride(URI.create(getConfig("S3_ENDPOINT")))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                getConfig("S3_ACCESS_KEY"),
                                getConfig("S3_SECRET_KEY"))))
                .build();
    }

    private static S3Presigner buildPresigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(getConfig("S3_ENDPOINT")))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                getConfig("S3_ACCESS_KEY"),
                                getConfig("S3_SECRET_KEY"))))
                .build();
    }

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
     * "Superior State Administrators" → "superior-state-administrators"
     */
    static String slugify(String name) {
        if (name == null || name.isBlank()) return "default";
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    /**
     * Sanitizes a display filename for use in Content-Disposition header.
     * Removes characters that could cause issues in HTTP headers.
     */
    private static String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) return "download";
        return name.replaceAll("[\"\\\\/:*?<>|]", "_");
    }
}
