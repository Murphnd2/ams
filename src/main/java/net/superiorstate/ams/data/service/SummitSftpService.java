package net.superiorstate.ams.data.service;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import net.superiorstate.ams.AppConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * S39-A -- connectivity probe, directory listing, and (T226, second half) a controlled write
 * path for Summit's MOVEit-hosted SFTP endpoint ({@code ftp1.dpath.com:22}, confirmed
 * SSH-based by banner probe 2026-09-09; see {@code docs/business/summit_data_exchange.md}).
 * Every method opens a connection, does one operation, disconnects. No delete, no rename --
 * wiring delivery into {@code SummitExportServlet} is a later increment, not this one.
 * <p>
 * Config keys ({@code SUMMIT_SFTP_HOST}, {@code SUMMIT_SFTP_PORT}, {@code SUMMIT_SFTP_USER},
 * {@code SUMMIT_SFTP_PASSWORD}, {@code SUMMIT_SFTP_HOST_KEY}, {@code SUMMIT_SFTP_REMOTE_DIR})
 * are read inline via {@link AppConfig#get(String)} at each call site below, matching the
 * existing {@code SUMMIT_TPA_ID_PREFIX} convention in {@code SummitExportServlet} -- there is
 * no constants class for these keys.
 */
public class SummitSftpService {

    private static final Logger log = LogManager.getLogger(SummitSftpService.class);

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int CHANNEL_TIMEOUT_MS = 15_000;

    /**
     * Opens a connection, captures the server identification string and the presented
     * host-key fingerprint, and disconnects. When {@code SUMMIT_SFTP_HOST_KEY} is set, the
     * presented fingerprint is verified against it and the probe is reported as failed on
     * mismatch.
     */
    public SftpProbeResult probe() {
        String host = AppConfig.get("SUMMIT_SFTP_HOST");
        int port = resolvePort(AppConfig.get("SUMMIT_SFTP_PORT"));
        String user = AppConfig.get("SUMMIT_SFTP_USER");
        String password = AppConfig.get("SUMMIT_SFTP_PASSWORD");
        String pinnedHostKey = AppConfig.get("SUMMIT_SFTP_HOST_KEY");
        boolean pinned = pinnedHostKey != null && !pinnedHostKey.isBlank();

        Session session = null;
        try {
            SftpSession conn = openSession(host, port, user, password);
            session = conn.session;

            String serverVersion = session.getServerVersion();
            HostKey hostKey = session.getHostKey();
            String fingerprint = hostKey != null ? hostKey.getFingerPrint(conn.jsch) : null;

            if (pinned && (fingerprint == null || !fingerprint.equalsIgnoreCase(pinnedHostKey.trim()))) {
                log.warn("[SUMMIT-SFTP] Host key fingerprint mismatch for {}:{}", host, port);
                return SftpProbeResult.failure(host, port, user,
                        "Host key fingerprint mismatch. Presented: " + fingerprint
                                + " -- pinned SUMMIT_SFTP_HOST_KEY does not match.", true);
            }

            return SftpProbeResult.success(host, port, user, serverVersion, fingerprint, pinned);
        } catch (Exception e) {
            String message = scrub(e.getMessage(), password);
            log.error("[SUMMIT-SFTP] Probe failed for {}:{} -- {}", host, port, message);
            return SftpProbeResult.failure(host, port, user, message, pinned);
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * Opens a connection, lists one remote directory, and disconnects. Throws when the
     * connection fails, the host key is pinned and mismatches, or the listing fails.
     */
    public List<SftpEntry> list(String remoteDir) throws SftpTransportException {
        String host = AppConfig.get("SUMMIT_SFTP_HOST");
        int port = resolvePort(AppConfig.get("SUMMIT_SFTP_PORT"));
        String user = AppConfig.get("SUMMIT_SFTP_USER");
        String password = AppConfig.get("SUMMIT_SFTP_PASSWORD");
        String pinnedHostKey = AppConfig.get("SUMMIT_SFTP_HOST_KEY");
        boolean pinned = pinnedHostKey != null && !pinnedHostKey.isBlank();
        String dir = (remoteDir == null || remoteDir.isBlank()) ? "/" : remoteDir;

        Session session = null;
        ChannelSftp channel = null;
        try {
            SftpSession conn = openSession(host, port, user, password);
            session = conn.session;

            if (pinned) {
                HostKey hostKey = session.getHostKey();
                String fingerprint = hostKey != null ? hostKey.getFingerPrint(conn.jsch) : null;
                if (fingerprint == null || !fingerprint.equalsIgnoreCase(pinnedHostKey.trim())) {
                    throw new SftpTransportException("Host key fingerprint mismatch. Presented: "
                            + fingerprint + " -- pinned SUMMIT_SFTP_HOST_KEY does not match.");
                }
            }

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(CHANNEL_TIMEOUT_MS);

            List<SftpEntry> results = new ArrayList<>();
            for (Object obj : channel.ls(dir)) {
                ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) obj;
                String name = entry.getFilename();
                if (".".equals(name) || "..".equals(name)) continue;
                results.add(new SftpEntry(name, entry.getAttrs().getSize(), entry.getAttrs().isDir()));
            }
            return results;
        } catch (SftpTransportException e) {
            throw e;
        } catch (Exception e) {
            String message = scrub(e.getMessage(), password);
            log.error("[SUMMIT-SFTP] List failed for {}:{} dir={} -- {}", host, port, dir, message);
            throw new SftpTransportException(message);
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * Creates {@code remoteDir} if it does not already exist. A second run against a
     * directory this method already created is not an error -- existence is checked with a
     * {@code stat} before attempting the create, so no exception is thrown either way.
     */
    public void mkdir(String remoteDir) throws SftpTransportException {
        String host = AppConfig.get("SUMMIT_SFTP_HOST");
        int port = resolvePort(AppConfig.get("SUMMIT_SFTP_PORT"));
        String user = AppConfig.get("SUMMIT_SFTP_USER");
        String password = AppConfig.get("SUMMIT_SFTP_PASSWORD");
        String pinnedHostKey = AppConfig.get("SUMMIT_SFTP_HOST_KEY");
        boolean pinned = pinnedHostKey != null && !pinnedHostKey.isBlank();

        Session session = null;
        ChannelSftp channel = null;
        try {
            SftpSession conn = openSession(host, port, user, password);
            session = conn.session;

            if (pinned) {
                HostKey hostKey = session.getHostKey();
                String fingerprint = hostKey != null ? hostKey.getFingerPrint(conn.jsch) : null;
                if (fingerprint == null || !fingerprint.equalsIgnoreCase(pinnedHostKey.trim())) {
                    throw new SftpTransportException("Host key fingerprint mismatch. Presented: "
                            + fingerprint + " -- pinned SUMMIT_SFTP_HOST_KEY does not match.");
                }
            }

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(CHANNEL_TIMEOUT_MS);

            boolean exists;
            try {
                exists = channel.stat(remoteDir).isDir();
            } catch (SftpException e) {
                exists = false;
            }
            if (!exists) {
                channel.mkdir(remoteDir);
            }
        } catch (SftpTransportException e) {
            throw e;
        } catch (Exception e) {
            String message = scrub(e.getMessage(), password);
            log.error("[SUMMIT-SFTP] mkdir failed for {}:{} dir={} -- {}", host, port, remoteDir, message);
            throw new SftpTransportException(message);
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * Uploads {@code content} to {@code remoteDir}/{@code filename}, overwriting any existing
     * file of that name. No delete, no rename -- removal is a manual Summit-UI action.
     */
    public void upload(String remoteDir, String filename, byte[] content) throws SftpTransportException {
        String host = AppConfig.get("SUMMIT_SFTP_HOST");
        int port = resolvePort(AppConfig.get("SUMMIT_SFTP_PORT"));
        String user = AppConfig.get("SUMMIT_SFTP_USER");
        String password = AppConfig.get("SUMMIT_SFTP_PASSWORD");
        String pinnedHostKey = AppConfig.get("SUMMIT_SFTP_HOST_KEY");
        boolean pinned = pinnedHostKey != null && !pinnedHostKey.isBlank();

        Session session = null;
        ChannelSftp channel = null;
        try {
            SftpSession conn = openSession(host, port, user, password);
            session = conn.session;

            if (pinned) {
                HostKey hostKey = session.getHostKey();
                String fingerprint = hostKey != null ? hostKey.getFingerPrint(conn.jsch) : null;
                if (fingerprint == null || !fingerprint.equalsIgnoreCase(pinnedHostKey.trim())) {
                    throw new SftpTransportException("Host key fingerprint mismatch. Presented: "
                            + fingerprint + " -- pinned SUMMIT_SFTP_HOST_KEY does not match.");
                }
            }

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(CHANNEL_TIMEOUT_MS);

            String remotePath = remoteDir.endsWith("/") ? remoteDir + filename : remoteDir + "/" + filename;
            try (ByteArrayInputStream in = new ByteArrayInputStream(content)) {
                channel.put(in, remotePath, ChannelSftp.OVERWRITE);
            }
        } catch (SftpTransportException e) {
            throw e;
        } catch (Exception e) {
            String message = scrub(e.getMessage(), password);
            log.error("[SUMMIT-SFTP] upload failed for {}:{} dir={} file={} -- {}",
                    host, port, remoteDir, filename, message);
            throw new SftpTransportException(message);
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * S39-A/S40-A session-setup helper -- the block that was duplicated across every method
     * before this run. Builds a {@link JSch} instance, opens a {@link Session} to
     * {@code host:port} as {@code user}, re-enables {@code ssh-dss} narrowly on this session
     * only (S39-B -- DataPath's MOVEit server offers it as its sole host-key algorithm; KEX,
     * cipher and MAC negotiation are unmodified), applies the shared connect timeout, and
     * connects. Behaviour is unchanged from the pre-extraction duplicates in {@code probe()}
     * and {@code list()}.
     */
    private SftpSession openSession(String host, int port, String user, String password) throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(user, host, port);
        session.setPassword(password);
        // No known_hosts on this installation yet -- the presented key is captured by callers
        // and compared manually against SUMMIT_SFTP_HOST_KEY when one is configured.
        session.setConfig("StrictHostKeyChecking", "no");
        session.setConfig("server_host_key", session.getConfig("server_host_key") + ",ssh-dss");
        session.setTimeout(CONNECT_TIMEOUT_MS);
        session.connect(CONNECT_TIMEOUT_MS);
        return new SftpSession(jsch, session);
    }

    private static int resolvePort(String raw) {
        if (raw == null || raw.isBlank()) return 22;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return 22;
        }
    }

    private static String scrub(String message, String password) {
        if (message == null) return "(no message)";
        if (password != null && !password.isEmpty()) {
            return message.replace(password, "***");
        }
        return message;
    }

    /** Dumb data carrier -- a connected session and the {@link JSch} instance that built it. */
    private static final class SftpSession {
        final JSch jsch;
        final Session session;

        SftpSession(JSch jsch, Session session) {
            this.jsch = jsch;
            this.session = session;
        }
    }

    /** Dumb data carrier -- result of a connect/disconnect probe. */
    public static final class SftpProbeResult {
        private final String host;
        private final int port;
        private final String user;
        private final boolean success;
        private final String serverVersion;
        private final String hostKeyFingerprint;
        private final boolean hostKeyPinned;
        private final String errorMessage;

        private SftpProbeResult(String host, int port, String user, boolean success, String serverVersion,
                                 String hostKeyFingerprint, boolean hostKeyPinned, String errorMessage) {
            this.host = host;
            this.port = port;
            this.user = user;
            this.success = success;
            this.serverVersion = serverVersion;
            this.hostKeyFingerprint = hostKeyFingerprint;
            this.hostKeyPinned = hostKeyPinned;
            this.errorMessage = errorMessage;
        }

        static SftpProbeResult success(String host, int port, String user, String serverVersion,
                                        String hostKeyFingerprint, boolean hostKeyPinned) {
            return new SftpProbeResult(host, port, user, true, serverVersion, hostKeyFingerprint, hostKeyPinned, null);
        }

        static SftpProbeResult failure(String host, int port, String user, String errorMessage, boolean hostKeyPinned) {
            return new SftpProbeResult(host, port, user, false, null, null, hostKeyPinned, errorMessage);
        }

        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getUser() { return user; }
        public boolean isSuccess() { return success; }
        public String getServerVersion() { return serverVersion; }
        public String getHostKeyFingerprint() { return hostKeyFingerprint; }
        public boolean isHostKeyPinned() { return hostKeyPinned; }
        public String getErrorMessage() { return errorMessage; }
    }

    /** Dumb data carrier -- one remote directory entry. */
    public static final class SftpEntry {
        private final String name;
        private final long size;
        private final boolean directory;

        public SftpEntry(String name, long size, boolean directory) {
            this.name = name;
            this.size = size;
            this.directory = directory;
        }

        public String getName() { return name; }
        public long getSize() { return size; }
        public boolean isDirectory() { return directory; }
    }

    /** Checked exception carrying a password-scrubbed failure message. */
    public static final class SftpTransportException extends Exception {
        public SftpTransportException(String message) {
            super(message);
        }
    }
}
