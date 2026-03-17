package net.superiorstate.ams.controller.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.TrainingVideo;
import net.superiorstate.ams.model.general.VideoToken;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.List;

/**
 * PSP Admin — Training Video Management.
 * Add videos, generate single-use token links, view token status, revoke tokens.
 */
@WebServlet(name = "ManageVideos", value = "/ManageVideos")
public class ManageVideos extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        if (!isAuthorized(session)) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            List<TrainingVideo> videos = em.createQuery(
                    "SELECT v FROM TrainingVideo v ORDER BY v.active DESC, v.title",
                    TrainingVideo.class
            ).getResultList();

            // If a video is selected, load its tokens
            String selectedIdStr = request.getParameter("videoId");
            if (selectedIdStr != null) {
                try {
                    Long selectedId = Long.parseLong(selectedIdStr);
                    TrainingVideo selected = em.find(TrainingVideo.class, selectedId);
                    if (selected != null) {
                        List<VideoToken> tokens = em.createQuery(
                                "SELECT t FROM VideoToken t WHERE t.video.id = :videoId ORDER BY t.createdAt DESC",
                                VideoToken.class
                        ).setParameter("videoId", selectedId).getResultList();
                        request.setAttribute("selectedVideo", selected);
                        request.setAttribute("tokens", tokens);
                    }
                } catch (NumberFormatException ignored) {}
            }

            // Resolve the system URL for generating links
            String systemUrl = AppConfig.get("SYSTEM_URL");
            if (systemUrl == null || systemUrl.isBlank()) {
                systemUrl = request.getScheme() + "://" + request.getServerName();
                if (request.getServerPort() != 80 && request.getServerPort() != 443) {
                    systemUrl += ":" + request.getServerPort();
                }
                String ctx = request.getContextPath();
                if (ctx != null && !ctx.isEmpty()) systemUrl += ctx;
            }

            request.setAttribute("videos", videos);
            request.setAttribute("systemUrl", systemUrl);
            request.setAttribute("pageTitle", "Training Videos");
            request.setAttribute("pageIcon", "bi-camera-video");
            request.getRequestDispatcher("/WEB-INF/view/a/admin/manageVideos.jsp").forward(request, response);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        if (!isAuthorized(session)) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        String action = request.getParameter("action");
        if (action == null) {
            response.sendRedirect(request.getContextPath() + "/ManageVideos");
            return;
        }

        switch (action) {
            case "addVideo" -> handleAddVideo(request, response);
            case "toggleVideo" -> handleToggleVideo(request, response);
            case "generateToken" -> handleGenerateToken(request, response);
            case "revokeToken" -> handleRevokeToken(request, response);
            default -> response.sendRedirect(request.getContextPath() + "/ManageVideos");
        }
    }

    private void handleAddVideo(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String filename = request.getParameter("filename");
        String title = request.getParameter("title");
        String description = request.getParameter("description");
        String durationStr = request.getParameter("durationSeconds");

        if (filename == null || filename.isBlank() || title == null || title.isBlank()) {
            request.getSession().setAttribute("videoError", "Filename and title are required.");
            response.sendRedirect(request.getContextPath() + "/ManageVideos");
            return;
        }

        filename = filename.trim();
        // Security: no path traversal
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            request.getSession().setAttribute("videoError", "Invalid filename.");
            response.sendRedirect(request.getContextPath() + "/ManageVideos");
            return;
        }

        // Check file exists on disk
        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        String videoPath = (global != null) ? global.getVideoPath() : "/var/lib/tomcat10/videos/";
        if (!Files.exists(Paths.get(videoPath, filename))) {
            request.getSession().setAttribute("videoError",
                    "File '" + filename + "' not found in " + videoPath + ". Upload it via SCP first.");
            response.sendRedirect(request.getContextPath() + "/ManageVideos");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            TrainingVideo video = new TrainingVideo();
            video.setFilename(filename);
            video.setTitle(title.trim());
            video.setDescription(description != null ? description.trim() : null);
            if (durationStr != null && !durationStr.isBlank()) {
                try { video.setDurationSeconds(Integer.parseInt(durationStr.trim())); } catch (NumberFormatException ignored) {}
            }

            em.getTransaction().begin();
            em.persist(video);
            em.getTransaction().commit();
            request.getSession().setAttribute("videoMessage", "Video '" + title.trim() + "' added.");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            request.getSession().setAttribute("videoError", "Error adding video: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }
        response.sendRedirect(request.getContextPath() + "/ManageVideos");
    }

    private void handleToggleVideo(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String videoIdStr = request.getParameter("videoId");
        if (videoIdStr == null) {
            response.sendRedirect(request.getContextPath() + "/ManageVideos");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            Long videoId = Long.parseLong(videoIdStr);
            TrainingVideo video = em.find(TrainingVideo.class, videoId);
            if (video != null) {
                em.getTransaction().begin();
                video.setActive(!video.isActive());
                em.merge(video);
                em.getTransaction().commit();
                request.getSession().setAttribute("videoMessage",
                        "Video " + (video.isActive() ? "activated" : "deactivated") + ".");
            }
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            request.getSession().setAttribute("videoError", "Error: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }
        response.sendRedirect(request.getContextPath() + "/ManageVideos?videoId=" + videoIdStr);
    }

    private void handleGenerateToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String videoIdStr = request.getParameter("videoId");
        String recipientName = request.getParameter("recipientName");
        String recipientEmail = request.getParameter("recipientEmail");
        String maxViewsStr = request.getParameter("maxViews");
        String expiresIn = request.getParameter("expiresIn"); // days

        if (videoIdStr == null) {
            response.sendRedirect(request.getContextPath() + "/ManageVideos");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            Long videoId = Long.parseLong(videoIdStr);
            TrainingVideo video = em.find(TrainingVideo.class, videoId);
            if (video == null) {
                request.getSession().setAttribute("videoError", "Video not found.");
                response.sendRedirect(request.getContextPath() + "/ManageVideos");
                return;
            }

            // Get current user
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            Person creator = local.getCurrentPerson();

            VideoToken token = new VideoToken();
            token.setVideo(video);
            token.setCreatedBy(creator);
            token.setRecipientName(recipientName != null ? recipientName.trim() : null);
            token.setRecipientEmail(recipientEmail != null ? recipientEmail.trim() : null);

            // Max views (default 1)
            if (maxViewsStr != null && !maxViewsStr.isBlank()) {
                try {
                    int mv = Integer.parseInt(maxViewsStr.trim());
                    if (mv > 0) token.setMaxViews(mv);
                } catch (NumberFormatException ignored) {}
            }

            // Expiration
            if (expiresIn != null && !expiresIn.isBlank()) {
                try {
                    int days = Integer.parseInt(expiresIn.trim());
                    if (days > 0) {
                        token.setExpiresAt(new Timestamp(System.currentTimeMillis() + (long) days * 24 * 60 * 60 * 1000));
                    }
                } catch (NumberFormatException ignored) {}
            }

            em.getTransaction().begin();
            em.persist(token);
            em.getTransaction().commit();

            // Store the generated token so the JSP can show the link
            request.getSession().setAttribute("generatedToken", token.getToken());
            request.getSession().setAttribute("videoMessage", "Token generated for " +
                    (recipientName != null && !recipientName.isBlank() ? recipientName.trim() : "recipient") + ".");

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            request.getSession().setAttribute("videoError", "Error generating token: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }
        response.sendRedirect(request.getContextPath() + "/ManageVideos?videoId=" + videoIdStr);
    }

    private void handleRevokeToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String tokenIdStr = request.getParameter("tokenId");
        String videoIdStr = request.getParameter("videoId");

        if (tokenIdStr == null) {
            response.sendRedirect(request.getContextPath() + "/ManageVideos");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            Long tokenId = Long.parseLong(tokenIdStr);
            VideoToken token = em.find(VideoToken.class, tokenId);
            if (token != null) {
                em.getTransaction().begin();
                token.setExpiresAt(new Timestamp(System.currentTimeMillis())); // expire immediately
                em.merge(token);
                em.getTransaction().commit();
                request.getSession().setAttribute("videoMessage", "Token revoked.");
            }
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            request.getSession().setAttribute("videoError", "Error revoking token: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }
        response.sendRedirect(request.getContextPath() + "/ManageVideos" +
                (videoIdStr != null ? "?videoId=" + videoIdStr : ""));
    }

    private boolean isAuthorized(HttpSession session) {
        Object isPspAdmin = session.getAttribute("isPspAdmin");
        return Boolean.TRUE.equals(isPspAdmin);
    }
}
