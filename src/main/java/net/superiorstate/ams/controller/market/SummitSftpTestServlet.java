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
 * second half) two controlled write actions, each reachable only at its own {@code action=}
 * value and standing apart from the other. PSP-admin only, reachable only by URL -- no nav
 * entry, no menu link. The default (no {@code action} parameter) behaviour is unchanged:
 * {@link SummitSftpService#probe()} and {@link SummitSftpService#list(String)} against a
 * directory chosen by {@code dir=} / {@code SUMMIT_SFTP_REMOTE_DIR} / {@code "/"}.
 * {@code ?action=writetest} (S40-A) targets only a fixed config-driven directory
 * ({@code SUMMIT_SFTP_TEST_DIR}) and refuses outright if that directory resolves anywhere
 * under {@code ImportFiles}. {@code ?action=importdrop} (S40-C) is the deliberate, separately
 * gated opposite: it targets only a fixed config-driven directory
 * ({@code SUMMIT_SFTP_IMPORT_DIR}) whose final path segment must be exactly {@code ImportFiles},
 * requires an exact confirmation token on the request, and never falls back to
 * {@link #handleWriteTest}. {@code ?action=importdropdemo} (S40-D) is a third, independently
 * gated path onto the same {@code ImportFiles} directory, isolating the filename as the only
 * variable against S40-C's probe: a Summit-recognized filename prefix carrying content that
 * cannot import under any real employer. It has its own literal confirmation token and calls
 * into neither of the other two write methods. Wiring delivery into {@code SummitExportServlet}
 * is a later increment, not this one.
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

        if ("importdrop".equals(request.getParameter("action"))) {
            handleImportDrop(request, out);
            return;
        }

        if ("importdropdemo".equals(request.getParameter("action"))) {
            handleImportDropDemo(request, out);
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

    /**
     * T226 / SDX-15 (S40-C). The deliberate, explicitly-authorized counterpart to
     * {@link #handleWriteTest}: writes a fixed, code-generated, deliberately non-conforming
     * probe file directly into the live Summit {@code ImportFiles} folder, to settle whether an
     * SFTP-delivered file is processed the same way as a Summit web-UI upload. Reachable only
     * at {@code ?action=importdrop}, and only once both gates below pass, in order -- neither
     * is reachable by accident and neither alone is sufficient. Never calls
     * {@link #handleWriteTest}; the two actions are separate paths by design. No path fragment,
     * directory, or filename is ever read from the request -- the confirmation token is the
     * only request input this method reads.
     */
    private void handleImportDrop(HttpServletRequest request, PrintWriter out) {
        // Gate 1: the target directory is config-resolved only. Unset -> refuse before ever
        // touching the request.
        String importDir = AppConfig.get("SUMMIT_SFTP_IMPORT_DIR");
        if (importDir == null || importDir.isBlank()) {
            out.println("Missing required configuration key: SUMMIT_SFTP_IMPORT_DIR");
            out.println("Set this in ssa.properties ({catalina.base}/conf/ssa.properties) and"
                    + " restart Tomcat before using the import drop. No connection attempted.");
            return;
        }

        // Gate 2: an exact, case-sensitive confirmation token. The expected value is never
        // echoed back on failure.
        String confirm = request.getParameter("confirm");
        if (!"SEND-TO-SUMMIT-IMPORTFILES".equals(confirm)) {
            out.println("Confirmation token required. No connection attempted.");
            return;
        }

        // Inverse guard: the mirror of handleWriteTest's refusal. This action may write ONLY to
        // ImportFiles -- the resolved directory's final path segment must be exactly
        // "ImportFiles", not merely contain it, so the ResponseFiles derivation below is safe.
        String normalized = importDir.endsWith("/") ? importDir.substring(0, importDir.length() - 1) : importDir;
        int lastSlash = normalized.lastIndexOf('/');
        String finalSegment = lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
        if (!"ImportFiles".equals(finalSegment)) {
            out.println("Refused: target directory's final path segment is not \"ImportFiles\""
                    + " (found \"" + finalSegment + "\"). This action can only write to the"
                    + " Summit import folder. No connection attempted.");
            return;
        }

        out.println("*** LIVE SUMMIT IMPORT DROP ***");
        out.println("This writes directly to the live Summit ImportFiles folder. Summit may begin"
                + " processing whatever lands there. This is not a test directory.");
        out.println();

        String responseFilesDir = normalized.substring(0, lastSlash + 1) + "ResponseFiles";
        out.println("ResponseFiles path (derived): " + responseFilesDir);

        SummitSftpService service = new SummitSftpService();

        out.println("Listing " + responseFilesDir + " BEFORE upload (baseline):");
        try {
            List<SftpEntry> before = service.list(responseFilesDir);
            if (before.isEmpty()) {
                out.println("(empty)");
            }
            for (SftpEntry entry : before) {
                out.println((entry.isDirectory() ? "[DIR]  " : "[FILE] ")
                        + entry.getName() + "  " + entry.getSize());
            }
        } catch (SftpTransportException e) {
            out.println("List FAILED: " + e.getMessage());
        }
        out.println();

        String filename = "AMS_SFTP_IMPORT_PROBE_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ".txt";
        String content = "This is a transport probe sent by AMS (Superior State Administration)"
                + " to test the Summit SFTP import path.\n"
                + "Generated automatically at " + LocalDateTime.now() + ".\n"
                + "This is not a Summit import file of any kind and should be discarded if found"
                + " in this folder.\n";

        try {
            service.upload(importDir, filename, content.getBytes(StandardCharsets.UTF_8));
        } catch (SftpTransportException e) {
            out.println("Upload FAILED: " + e.getMessage());
            return;
        }

        out.println("Listing " + importDir + " after upload:");
        try {
            List<SftpEntry> after = service.list(importDir);
            if (after.isEmpty()) {
                out.println("(empty)");
            }
            for (SftpEntry entry : after) {
                out.println((entry.isDirectory() ? "[DIR]  " : "[FILE] ")
                        + entry.getName() + "  " + entry.getSize());
            }
        } catch (SftpTransportException e) {
            out.println("List FAILED: " + e.getMessage());
        }

        out.println();
        String remotePath = importDir.endsWith("/") ? importDir + filename : importDir + "/" + filename;
        out.println("Remote path written: " + remotePath);
        out.println("Filename: " + filename);
        out.println("Payload content:");
        out.println(content);
    }

    /**
     * T226 / SDX-15, sweep hypothesis (S40-D). A third, independently gated path onto the same
     * {@code ImportFiles} directory as {@link #handleImportDrop}, deliberately not unified with
     * it -- each drop action carries its own literal filename and its own literal confirmation
     * token. This one uses a Summit-recognized filename prefix ({@code ZZ_TEST_DEMO_}) so that
     * a difference in outcome from {@link #handleImportDrop}'s probe isolates the filename as
     * the variable. The payload is three pipe-delimited Demographics-shaped rows keyed to a
     * deliberately nonexistent employer TPA custom ID, so nothing can import from it under any
     * real employer or participant on this live tenant. No path fragment, directory, or
     * filename is ever read from the request -- the confirmation token is the only request
     * input this method reads.
     */
    private void handleImportDropDemo(HttpServletRequest request, PrintWriter out) {
        // Gate 1: same config key as handleImportDrop -- the target directory is config-resolved
        // only. Unset -> refuse before ever touching the request.
        String importDir = AppConfig.get("SUMMIT_SFTP_IMPORT_DIR");
        if (importDir == null || importDir.isBlank()) {
            out.println("Missing required configuration key: SUMMIT_SFTP_IMPORT_DIR");
            out.println("Set this in ssa.properties ({catalina.base}/conf/ssa.properties) and"
                    + " restart Tomcat before using the import drop. No connection attempted.");
            return;
        }

        // Gate 2: a different exact, case-sensitive confirmation token from handleImportDrop's,
        // on purpose -- a URL already in Kevin's browser history for the other action must not
        // be able to fire this one. The expected value is never echoed back on failure.
        String confirm = request.getParameter("confirm");
        if (!"SEND-NAMED-DEMO-TO-IMPORTFILES".equals(confirm)) {
            out.println("Confirmation token required. No connection attempted.");
            return;
        }

        // Inverse guard: same shape as handleImportDrop's. This action may write ONLY to
        // ImportFiles -- the resolved directory's final path segment must be exactly
        // "ImportFiles", not merely contain it, so the ResponseFiles derivation below is safe.
        String normalized = importDir.endsWith("/") ? importDir.substring(0, importDir.length() - 1) : importDir;
        int lastSlash = normalized.lastIndexOf('/');
        String finalSegment = lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
        if (!"ImportFiles".equals(finalSegment)) {
            out.println("Refused: target directory's final path segment is not \"ImportFiles\""
                    + " (found \"" + finalSegment + "\"). This action can only write to the"
                    + " Summit import folder. No connection attempted.");
            return;
        }

        out.println("*** LIVE SUMMIT IMPORT DROP -- NAME-CONFORMING DEMO ***");
        out.println("This writes a filename-conforming file to the live Summit ImportFiles"
                + " folder. The content is deliberately invalid -- keyed to a nonexistent"
                + " employer -- so nothing can import from it.");
        out.println();

        String responseFilesDir = normalized.substring(0, lastSlash + 1) + "ResponseFiles";
        out.println("ResponseFiles path (derived): " + responseFilesDir);

        SummitSftpService service = new SummitSftpService();

        out.println("Listing " + responseFilesDir + " BEFORE upload (baseline):");
        try {
            List<SftpEntry> before = service.list(responseFilesDir);
            if (before.isEmpty()) {
                out.println("(empty)");
            }
            for (SftpEntry entry : before) {
                out.println((entry.isDirectory() ? "[DIR]  " : "[FILE] ")
                        + entry.getName() + "  " + entry.getSize());
            }
        } catch (SftpTransportException e) {
            out.println("List FAILED: " + e.getMessage());
        }
        out.println();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String filename = "ZZ_TEST_DEMO_" + timestamp + ".txt";
        // S40-H -- the participant ID carries the same timestamp as the filename, so the content
        // is unique per run (not just the filename) and can no longer be ignored by content
        // dedupe. Reused, not recomputed, so file and rows correlate to the second.
        String content =
                "ZZZ-NO-SUCH-EMPLOYER|ZZZ-P-" + timestamp + "-1|Probe|Notarealperson|1 Nonexistent Way|Marinette|WI|54143|20260930|probe.notarealperson@invalid.example||AMS\n"
              + "ZZZ-NO-SUCH-EMPLOYER|ZZZ-P-" + timestamp + "-2|Probe|Notarealpersontwo|1 Nonexistent Way|Marinette|WI|54143|20260930|probe.notarealpersontwo@invalid.example||AMS\n"
              + "ZZZ-NO-SUCH-EMPLOYER|ZZZ-P-" + timestamp + "-3|Probe|Notarealpersonthree|1 Nonexistent Way|Marinette|WI|54143|20260930|probe.notarealpersonthree@invalid.example||AMS\n";

        try {
            service.upload(importDir, filename, content.getBytes(StandardCharsets.UTF_8));
        } catch (SftpTransportException e) {
            out.println("Upload FAILED: " + e.getMessage());
            return;
        }

        out.println("Listing " + importDir + " after upload:");
        try {
            List<SftpEntry> after = service.list(importDir);
            if (after.isEmpty()) {
                out.println("(empty)");
            }
            for (SftpEntry entry : after) {
                out.println((entry.isDirectory() ? "[DIR]  " : "[FILE] ")
                        + entry.getName() + "  " + entry.getSize());
            }
        } catch (SftpTransportException e) {
            out.println("List FAILED: " + e.getMessage());
        }

        out.println();
        String remotePath = importDir.endsWith("/") ? importDir + filename : importDir + "/" + filename;
        out.println("Remote path written: " + remotePath);
        out.println("Filename: " + filename);
        out.println("Payload content:");
        out.println(content);
    }
}
