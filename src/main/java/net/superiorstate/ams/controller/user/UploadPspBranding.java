package net.superiorstate.ams.controller.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.Constant;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * PSP Branding Upload — allows PSP Admins to upload custom navbar logo, login logo, and favicon.
 *
 * GET  → forwards to branding management page
 * POST → processes file uploads, validates dimensions, saves to /images/psp/, updates DB constants
 *
 * Files are saved to the webapp's images/psp/ directory so they're served as static resources.
 */
@WebServlet(name = "UploadPspBranding", value = "/UploadPspBranding")
@MultipartConfig(
        fileSizeThreshold = 1024 * 512,          // 512 KB
        maxFileSize = 1024 * 1024 * 2,           // 2 MB per file
        maxRequestSize = 1024 * 1024 * 5         // 5 MB total
)
public class UploadPspBranding extends HttpServlet {

    // Navbar logo constraints
    private static final int NAVBAR_MAX_WIDTH = 300;
    private static final int NAVBAR_MAX_HEIGHT = 80;

    // Login logo constraints
    private static final int LOGIN_MAX_WIDTH = 800;
    private static final int LOGIN_MAX_HEIGHT = 400;

    // Favicon constraints
    private static final int FAVICON_MAX_SIZE = 32;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Gate to PSP Admin only
        if (!isPspAdmin(request)) {
            response.sendRedirect("ViewHome25");
            return;
        }

        // Load current values for display
        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        request.setAttribute("currentNavbarLogo", global.getLogoNavbar());
        request.setAttribute("currentLoginLogo", global.getLogoLogin());
        request.setAttribute("currentFavicon", global.getFavicon());

        request.getRequestDispatcher("/WEB-INF/view/user/pspBranding25.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        if (!isPspAdmin(request)) {
            response.sendRedirect("ViewHome25");
            return;
        }
        // Use memory cache for ImageIO (avoids temp dir permission issues)
        javax.imageio.ImageIO.setUseCache(false);

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        StringBuilder errors = new StringBuilder();
        boolean anyUpdated = false;

        try {
            // Resolve the webapp's images/psp/ directory on disk
            String webappRoot = getServletContext().getRealPath("/");
            Path pspDir = Paths.get(webappRoot, "images", "psp");
            Files.createDirectories(pspDir);

            // --- Navbar Logo ---
            Part navbarPart = request.getPart("navbarLogo");
            if (navbarPart != null && navbarPart.getSize() > 0) {
                String result = processImageUpload(navbarPart, pspDir, "logo-navbar.png",
                        NAVBAR_MAX_WIDTH, NAVBAR_MAX_HEIGHT, "Navbar logo");
                if (result == null) {
                    updateConstant(em, "LOGO_NAVBAR", "/images/psp/logo-navbar.png");
                    anyUpdated = true;
                } else {
                    errors.append(result).append(" ");
                }
            }

            // --- Login Logo ---
            Part loginPart = request.getPart("loginLogo");
            if (loginPart != null && loginPart.getSize() > 0) {
                String result = processImageUpload(loginPart, pspDir, "logo-login.png",
                        LOGIN_MAX_WIDTH, LOGIN_MAX_HEIGHT, "Login logo");
                if (result == null) {
                    updateConstant(em, "LOGO_LOGIN", "/images/psp/logo-login.png");
                    anyUpdated = true;
                } else {
                    errors.append(result).append(" ");
                }
            }

            // --- Favicon ---
            Part faviconPart = request.getPart("favicon");
            if (faviconPart != null && faviconPart.getSize() > 0) {
                String result = processFaviconUpload(faviconPart, pspDir);
                if (result == null) {
                    updateConstant(em, "FAVICON", "/images/psp/favicon.ico");
                    anyUpdated = true;
                } else {
                    errors.append(result).append(" ");
                }
            }

            // Refresh AmsDataGlobal so changes take effect immediately
            if (anyUpdated) {
                AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
                EntityManager em2 = emf.createEntityManager();
                try {
                    global.initializeGlobalData(em2);
                    getServletContext().setAttribute("global", global);
                } finally {
                    em2.close();
                }
            }

        } finally {
            if (em.isOpen()) em.close();
        }

        // Redirect back with status
        if (errors.length() > 0) {
            response.sendRedirect("UploadPspBranding?error=" + java.net.URLEncoder.encode(errors.toString().trim(), "UTF-8"));
        } else if (anyUpdated) {
            response.sendRedirect("UploadPspBranding?success=true");
        } else {
            response.sendRedirect("UploadPspBranding");
        }
    }

    /**
     * Validates and saves a PNG image upload.
     * @return null on success, error message string on failure
     */
    private String processImageUpload(Part part, Path targetDir, String filename,
                                       int maxWidth, int maxHeight, String label) throws IOException {
        // Validate PNG
        String contentType = part.getContentType();
        if (contentType == null || !contentType.equalsIgnoreCase("image/png")) {
            return label + " must be a PNG file.";
        }

        // Read image and validate dimensions
        BufferedImage img;
        try (InputStream is = part.getInputStream()) {
            img = ImageIO.read(is);
        }
        if (img == null) {
            return label + " file could not be read as an image.";
        }
        if (img.getWidth() > maxWidth || img.getHeight() > maxHeight) {
            return label + " exceeds maximum dimensions (" + maxWidth + "×" + maxHeight + "px). "
                    + "Uploaded: " + img.getWidth() + "×" + img.getHeight() + "px.";
        }

        // Save to disk (overwrites previous)
        Path target = targetDir.resolve(filename);
        ImageIO.write(img, "png", target.toFile());
        System.out.println("✅ Saved branding file: " + target);
        return null;
    }

    /**
     * Validates and saves a favicon upload (ICO or PNG, max 32×32).
     * @return null on success, error message string on failure
     */
    private String processFaviconUpload(Part part, Path targetDir) throws IOException {
        String contentType = part.getContentType();
        String filename = part.getSubmittedFileName();
        String ext = (filename != null && filename.contains("."))
                ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase() : "";

        // Accept .ico files (content type varies by browser) or small PNG
        boolean isIco = "ico".equals(ext) || "image/x-icon".equalsIgnoreCase(contentType)
                || "image/vnd.microsoft.icon".equalsIgnoreCase(contentType);
        boolean isPng = "image/png".equalsIgnoreCase(contentType);

        if (!isIco && !isPng) {
            return "Favicon must be an ICO or PNG file.";
        }

        if (isPng) {
            // Validate dimensions for PNG favicon
            BufferedImage img;
            try (InputStream is = part.getInputStream()) {
                img = ImageIO.read(is);
            }
            if (img == null) {
                return "Favicon file could not be read as an image.";
            }
            if (img.getWidth() > FAVICON_MAX_SIZE || img.getHeight() > FAVICON_MAX_SIZE) {
                return "Favicon PNG exceeds " + FAVICON_MAX_SIZE + "×" + FAVICON_MAX_SIZE + "px. "
                        + "Uploaded: " + img.getWidth() + "×" + img.getHeight() + "px.";
            }
            Path target = targetDir.resolve("favicon.ico");
            ImageIO.write(img, "png", target.toFile());
        } else {
            // ICO — validate file size only (can't easily read ICO dimensions in Java)
            if (part.getSize() > 100 * 1024) {
                return "Favicon ICO file must be under 100KB.";
            }
            Path target = targetDir.resolve("favicon.ico");
            try (InputStream is = part.getInputStream()) {
                Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        System.out.println("✅ Saved favicon: " + targetDir.resolve("favicon.ico"));
        return null;
    }

    /**
     * Updates or inserts a constant value in the database.
     */
    private void updateConstant(EntityManager em, String name, String value) {
        em.getTransaction().begin();
        try {
            Query q = em.createQuery("SELECT c FROM Constant c WHERE c.name = :name");
            q.setParameter("name", name);
            Constant c = (Constant) q.getSingleResult();
            c.setValue(value);
            em.persist(c);
        } catch (Exception e) {
            // Constant doesn't exist yet — create it
            Constant c = new Constant();
            c.setName(name);
            c.setValue(value);
            em.persist(c);
        }
        em.getTransaction().commit();
    }

    private boolean isPspAdmin(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
    }
}
