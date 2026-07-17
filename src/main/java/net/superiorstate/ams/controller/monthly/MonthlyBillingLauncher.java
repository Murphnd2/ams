package net.superiorstate.ams.controller.monthly;

import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import net.superiorstate.ams.data.service.BillingRunService;
import net.superiorstate.ams.data.service.MonthlyBillingPreflight;
import net.superiorstate.ams.data.service.MonthlyBillingPreflight.PreflightResult;
import net.superiorstate.ams.data.util.PathUtil;
import net.superiorstate.ams.model.billing.BillingRun;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Monthly Billing Launcher — pre-flight checklist page + HSA upload.
 * The launch itself (POST to LaunchMonthlyBilling) is wired in increment 9b.
 */
@WebServlet(name = "MonthlyBillingLauncher", value = "/MonthlyBillingLauncher")
@MultipartConfig(
        maxFileSize = 1024 * 1024 * 20,    // 20 MB per file
        maxRequestSize = 1024 * 1024 * 50   // 50 MB total
)
public class MonthlyBillingLauncher extends HttpServlet {

    private static final SimpleDateFormat TS = new SimpleDateFormat("yyyyMMdd_HHmmss");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");

        try {
            PreflightResult result = MonthlyBillingPreflight.check(emf);
            request.setAttribute("preflight", result);
        } catch (IOException e) {
            request.setAttribute("preflightError", e.getMessage());
        }

        BillingRun latest = BillingRunService.findLatestRun(emf);
        if (latest != null) {
            request.setAttribute("latestRunId", latest.getId());
            request.setAttribute("latestRunStatus", latest.getStatus());
        }

        String flash = (String) request.getSession().getAttribute("flash");
        if (flash != null) {
            request.setAttribute("flash", flash);
            request.getSession().removeAttribute("flash");
        }

        request.getRequestDispatcher("/WEB-INF/view/a/general/monthlyBilling/launcher.jsp")
                .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Part hsaPart = request.getPart("hsaFile");
        if (hsaPart == null || hsaPart.getSize() == 0) {
            request.getSession().setAttribute("flash", "Please choose an HSA CSV file to upload.");
            redirectBack(request, response);
            return;
        }

        String submittedName = safeSubmittedName(hsaPart);
        if (submittedName == null || !submittedName.toLowerCase().endsWith(".csv")) {
            request.getSession().setAttribute("flash", "HSA file must be a .csv export.");
            redirectBack(request, response);
            return;
        }

        Path uploadDir = PathUtil.resolveAndEnsureDir(
                getServletContext(), "AMS_UPLOAD_DIR", "AMS_UPLOAD_DIR", "ams.upload.dir", "work/ams-uploads");

        deleteExistingHsaFiles(uploadDir);

        String targetName = "Life Count Detail_processed_" + TS.format(new Date()) + ".csv";
        Path target = uploadDir.resolve(targetName);
        Path tmp = uploadDir.resolve("." + targetName + ".part");

        try (InputStream in = hsaPart.getInputStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);

        redirectBack(request, response);
    }

    /**
     * Deletes every existing file matching the live HSA lookup predicate
     * (Importer.findFileWithPrefix(dir, "Life")): name starts with "Life" (case-sensitive)
     * and ends ".csv" (case-insensitive). Guarantees at most one such file remains
     * after this save, matching findFileWithPrefix's "exactly one match" requirement.
     */
    private void deleteExistingHsaFiles(Path uploadDir) throws IOException {
        try (var stream = Files.list(uploadDir)) {
            for (Path p : stream.toList()) {
                if (!Files.isRegularFile(p)) continue;
                String name = p.getFileName().toString();
                if (name.startsWith("Life") && name.toLowerCase().endsWith(".csv")) {
                    Files.deleteIfExists(p);
                }
            }
        }
    }

    private void redirectBack(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(request.getContextPath() + "/MonthlyBillingLauncher");
    }

    private String safeSubmittedName(Part part) {
        String s = part.getSubmittedFileName();
        if (s != null && !s.isBlank()) {
            return Paths.get(s).getFileName().toString();
        }
        return null;
    }

    private boolean isAdmin(HttpServletRequest request) {
        Boolean isPspAdmin = (Boolean) request.getSession().getAttribute("isPspAdmin");
        return isPspAdmin != null && isPspAdmin;
    }
}
