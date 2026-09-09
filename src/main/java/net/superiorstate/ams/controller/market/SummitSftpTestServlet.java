package net.superiorstate.ams.controller.market;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.service.SummitSftpService;
import net.superiorstate.ams.data.service.SummitSftpService.SftpEntry;
import net.superiorstate.ams.data.service.SummitSftpService.SftpProbeResult;
import net.superiorstate.ams.data.service.SummitSftpService.SftpTransportException;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * S39-A -- read-only connectivity check for Summit's MOVEit-hosted SFTP endpoint, plus (T226,
 * second half, S40-A) a controlled write test reachable only at {@code ?action=writetest}.
 * PSP-admin only, reachable only by URL -- no nav entry, no menu link. The default (no
 * {@code action} parameter) behaviour is unchanged: {@link SummitSftpService#probe()} and
 * {@link SummitSftpService#list(String)} against a directory chosen by {@code dir=} /
 * {@code SUMMIT_SFTP_REMOTE_DIR} / {@code "/"}. The write test targets only a fixed
 * config-driven directory ({@code SUMMIT_SFTP_TEST_DIR}) with a fixed generated filename and
 * fixed generated content -- never anything taken from the request -- and refuses outright if
 * that directory resolves anywhere under {@code ImportFiles}, which is Summit's processing
 * trigger. Wiring delivery into {@code SummitExportServlet} is a later increment, not this one.
 */
@WebServlet(name = "SummitSftpTestServlet", value = "/SummitSftpTest")
public class SummitSftpTestServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
        if (!isPspAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        List<String> missing = new ArrayList<>();
        String host = AppConfig.get("SUMMIT_SFTP_HOST");
        String user = AppConfig.get("SUMMIT_SFTP_USER");
        String password = AppConfig.get("SUMMIT_SFTP_PASSWORD");
        if (host == null || host.isBlank()) missing.add("SUMMIT_SFTP_HOST");
        if (user == null || user.isBlank()) missing.add("SUMMIT_SFTP_USER");
        if (password == null || password.isBlank()) missing.add("SUMMIT_SFTP_PASSWORD");

        if (!missing.isEmpty()) {
            out.println("Missing required configuration key(s): " + String.join(", ", missing));
            out.println("Set these in ssa.properties ({catalina.base}/conf/ssa.properties) and"
                    + " restart Tomcat before using this test endpoint. No connection attempted.");
            return;
        }

        if ("writetest".equals(request.getParameter("action"))) {
            handleWriteTest(out);
            return;
        }

        SummitSftpService service = new SummitSftpService();
        SftpProbeResult probeResult = service.probe();

        out.println("Host: " + probeResult.getHost());
        out.println("Port: " + probeResult.getPort());
        out.println("User: " + probeResult.getUser());

        if (!probeResult.isSuccess()) {
            out.println("Probe FAILED: " + probeResult.getErrorMessage());
            return;
        }

        out.println("Server identification: " + probeResult.getServerVersion());
        out.println("Host key fingerprint: " + probeResult.getHostKeyFingerprint());
        out.println("Host key mode: " + (probeResult.isHostKeyPinned()
                ? "PINNED (strict -- verified against SUMMIT_SFTP_HOST_KEY)"
                : "UNPINNED (host-key checking off -- set SUMMIT_SFTP_HOST_KEY to this"
                        + " fingerprint to pin it)"));
        out.println();

        // S39-D -- a dir= override so browsing the remote tree costs a URL edit, not a
        // ssa.properties edit plus a Tomcat restart per level. Precedence: dir parameter (when
        // present and non-blank after trimming), else SUMMIT_SFTP_REMOTE_DIR, else "/". The value
        // is passed through unmodified beyond trimming outer whitespace -- the account directory
        // contains internal spaces, and getParameter has already URL-decoded it.
        String dirParam = request.getParameter("dir");
        String remoteDir;
        String directorySource;
        if (dirParam != null && !dirParam.trim().isEmpty()) {
            String trimmed = dirParam.trim();
            if (trimmed.indexOf('\r') >= 0 || trimmed.indexOf('\n') >= 0) {
                out.println("Invalid dir parameter: carriage return or line feed not allowed."
                        + " No connection attempted.");
                return;
            }
            remoteDir = trimmed;
            directorySource = "dir parameter";
        } else {
            String configuredDir = AppConfig.get("SUMMIT_SFTP_REMOTE_DIR");
            if (configuredDir != null && !configuredDir.isBlank()) {
                remoteDir = configuredDir;
                directorySource = "SUMMIT_SFTP_REMOTE_DIR";
            } else {
                remoteDir = "/";
                directorySource = "default (/)";
            }
        }

        out.println("Directory source: " + directorySource);
        out.println("Listing: " + remoteDir);
        try {
            List<SftpEntry> entries = service.list(remoteDir);
            if (entries.isEmpty()) {
                out.println("(empty)");
            }
            for (SftpEntry entry : entries) {
                out.println((entry.isDirectory() ? "[DIR]  " : "[FILE] ")
                        + entry.getName() + "  " + entry.getSize());
            }
        } catch (SftpTransportException e) {
            out.println("List FAILED: " + e.getMessage());
        }
    }

    /**
     * T226, second half (S40-A). Writes a fixed, code-generated file to a fixed,
     * config-resolved directory -- no request-supplied path, filename, or content anywhere in
     * this method. Refuses outright if the resolved directory is {@code ImportFiles}, Summit's
     * processing trigger, in any casing.
     */
    private void handleWriteTest(PrintWriter out) {
        String testDir = AppConfig.get("SUMMIT_SFTP_TEST_DIR");
        if (testDir == null || testDir.isBlank()) {
            out.println("Missing required configuration key: SUMMIT_SFTP_TEST_DIR");
            out.println("Set this in ssa.properties ({catalina.base}/conf/ssa.properties) and"
                    + " restart Tomcat before using the write test. No connection attempted.");
            return;
        }

        if (testDir.toLowerCase().contains("importfiles")) {
            out.println("Refused: target directory contains \"ImportFiles\", which triggers Summit"
                    + " import processing on this tenant. This servlet deliberately cannot write"
                    + " there. No connection attempted.");
            return;
        }

        String filename = "AMS_SFTP_WRITE_TEST_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ".txt";
        String content = "AMS SFTP write test\n"
                + "Generated: " + LocalDateTime.now() + "\n"
                + "Source: SummitSftpTestServlet?action=writetest\n";

        SummitSftpService service = new SummitSftpService();
        try {
            service.mkdir(testDir);
            service.upload(testDir, filename, content.getBytes(StandardCharsets.UTF_8));
        } catch (SftpTransportException e) {
            out.println("Write test FAILED: " + e.getMessage());
            return;
        }

        out.println("Listing " + testDir + " after upload:");
        try {
            List<SftpEntry> entries = service.list(testDir);
            if (entries.isEmpty()) {
                out.println("(empty)");
            }
            for (SftpEntry entry : entries) {
                out.println((entry.isDirectory() ? "[DIR]  " : "[FILE] ")
                        + entry.getName() + "  " + entry.getSize());
            }
        } catch (SftpTransportException e) {
            out.println("List FAILED: " + e.getMessage());
        }

        out.println();
        String remotePath = testDir.endsWith("/") ? testDir + filename : testDir + "/" + filename;
        out.println("Remote path written: " + remotePath);
        out.println("Filename: " + filename);
    }
}
