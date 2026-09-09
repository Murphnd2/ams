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
import java.util.ArrayList;
import java.util.List;

/**
 * S39-A -- read-only connectivity check for Summit's MOVEit-hosted SFTP endpoint. PSP-admin
 * only, reachable only by URL -- no nav entry, no menu link. Calls {@link SummitSftpService}'s
 * {@code probe()} and {@code list()} and prints the results as plain text. This increment never
 * writes to the remote server: no put, no mkdir, no rename, no delete. Wiring delivery into
 * {@code SummitExportServlet} is a later increment, not this one.
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

        String remoteDir = AppConfig.get("SUMMIT_SFTP_REMOTE_DIR", "/");
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
}
