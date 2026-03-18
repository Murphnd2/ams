package net.superiorstate.ams.controller.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.model.general.VideoToken;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Public servlet for token-gated video streaming.
 *
 * Landing page:  GET /video?t={uuid}           → validates token, renders player JSP
 * Video stream:  GET /video?t={uuid}&stream=1   → streams MP4 with Range request support
 *
 * Tokens are single-use with a 2-hour grace period after first view
 * so that page refreshes and seeking don't burn the token.
 */
@WebServlet(name = "ServeVideo", value = "/video")
public class ServeVideo extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String tokenParam = request.getParameter("t");
        if (tokenParam == null || tokenParam.isBlank()) {
            forwardToExpired(request, response, "No video token provided.");
            return;
        }

        // Clean the token — only allow UUID characters
        tokenParam = tokenParam.trim();
        if (!tokenParam.matches("[0-9a-fA-F\\-]{36}")) {
            forwardToExpired(request, response, "Invalid video link.");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        if (emf == null) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        EntityManager em = emf.createEntityManager();
        try {
            // Look up the token
            VideoToken vt;
            try {
                vt = em.createQuery(
                        "SELECT vt FROM VideoToken vt JOIN FETCH vt.video WHERE vt.token = :token",
                        VideoToken.class
                ).setParameter("token", tokenParam).getSingleResult();
            } catch (NoResultException e) {
                forwardToExpired(request, response, "This video link is not valid.");
                return;
            }

            // Check if token is still valid
            if (!vt.isValid()) {
                forwardToExpired(request, response, "This video link has expired or has already been viewed.");
                return;
            }

            // Check video is active
            if (!vt.getVideo().isActive()) {
                forwardToExpired(request, response, "This video is no longer available.");
                return;
            }

            // Resolve video file
            AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
            String videoPath = (global != null) ? global.getVideoPath() : "/var/lib/tomcat10/videos/";
            String filename = vt.getVideo().getFilename();

            // Path traversal prevention
            if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            Path videoFile = Paths.get(videoPath, filename);
            if (!Files.exists(videoFile) || !Files.isRegularFile(videoFile)) {
                forwardToExpired(request, response, "Video file not found on server.");
                return;
            }

            // Is this a stream request or a landing page request?
            boolean isStream = "1".equals(request.getParameter("stream"));

            if (isStream) {
                // Stream the MP4 with Range request support
                streamVideo(request, response, videoFile);
            } else {
                // Landing page only — view is recorded later via POST when user clicks play
                request.setAttribute("videoToken", vt);
                request.setAttribute("videoTitle", vt.getVideo().getTitle());
                request.setAttribute("videoDescription", vt.getVideo().getDescription());
                request.setAttribute("tokenParam", tokenParam);
                request.getRequestDispatcher("/WEB-INF/view/general/videoPlayer.jsp").forward(request, response);
            }
        } finally {
            em.close();
        }
    }

    /**
     * Records a view when the user actually clicks play (AJAX from videoPlayer.jsp).
     * This prevents email link previews and bots from consuming tokens.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String tokenParam = request.getParameter("t");
        if (tokenParam == null || !tokenParam.matches("[0-9a-fA-F\\-]{36}")) {
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":\"invalid\"}");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        if (emf == null) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        EntityManager em = emf.createEntityManager();
        try {
            VideoToken vt;
            try {
                vt = em.createQuery(
                        "SELECT vt FROM VideoToken vt WHERE vt.token = :token",
                        VideoToken.class
                ).setParameter("token", tokenParam).getSingleResult();
            } catch (NoResultException e) {
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":\"invalid\"}");
                return;
            }

            // Only record if not yet viewed
            if (vt.getFirstViewedAt() == null) {
                em.getTransaction().begin();
                vt.recordView(getClientIp(request), request.getHeader("User-Agent"));
                em.merge(vt);
                em.getTransaction().commit();
            }

            response.setContentType("application/json");
            response.getWriter().write("{\"status\":\"ok\"}");
        } finally {
            em.close();
        }
    }

    /**
     * Streams the video file with HTTP Range request support.
     * This is critical for HTML5 video players to support seeking.
     */
    private void streamVideo(HttpServletRequest request, HttpServletResponse response, Path videoFile)
            throws IOException {

        long fileLength = Files.size(videoFile);
        response.setContentType("video/mp4");
        response.setHeader("Accept-Ranges", "bytes");

        String rangeHeader = request.getHeader("Range");
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            // Parse range: "bytes=START-END" or "bytes=START-"
            String rangeValue = rangeHeader.substring(6);
            String[] parts = rangeValue.split("-", 2);
            long start;
            long end;

            try {
                start = Long.parseLong(parts[0]);
                end = (parts.length > 1 && !parts[1].isEmpty())
                        ? Long.parseLong(parts[1])
                        : fileLength - 1;
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
            }

            if (start < 0 || end >= fileLength || start > end) {
                response.setHeader("Content-Range", "bytes */" + fileLength);
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
            }

            long contentLength = end - start + 1;
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
            response.setContentLengthLong(contentLength);

            try (RandomAccessFile raf = new RandomAccessFile(videoFile.toFile(), "r");
                 OutputStream out = response.getOutputStream()) {
                raf.seek(start);
                byte[] buffer = new byte[8192];
                long remaining = contentLength;
                while (remaining > 0) {
                    int toRead = (int) Math.min(buffer.length, remaining);
                    int read = raf.read(buffer, 0, toRead);
                    if (read == -1) break;
                    out.write(buffer, 0, read);
                    remaining -= read;
                }
                out.flush();
            }
        } else {
            // Full file request
            response.setContentLengthLong(fileLength);
            try (OutputStream out = response.getOutputStream()) {
                Files.copy(videoFile, out);
                out.flush();
            }
        }
    }

    private void forwardToExpired(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        request.setAttribute("errorMessage", message);
        request.getRequestDispatcher("/WEB-INF/view/general/videoExpired.jsp").forward(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isEmpty()) {
            return xForwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
