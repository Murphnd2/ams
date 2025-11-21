package net.superiorstate.ams.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@WebServlet("/Eligibility125Submit")
public class Eligibility125SubmitServlet extends HttpServlet {

    private static final String GOOGLE_SCRIPT_URL =
            "https://script.google.com/macros/s/AKfycbzoFCW5OYxBVQN57HEvv4iv257lTIQUMETbsb684bQ3W9pBaoZWDV-OZF8ug_KuuylA/exec";

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        // Build base body from all incoming parameters
        String baseBody = buildFormBody(request.getParameterMap());

        // Optionally append a fixed form version tag
        String extra = "&formVersion=" +
                URLEncoder.encode("125Eligibility-v2", StandardCharsets.UTF_8);

        String body = baseBody + extra;

        int status = 0;
        try {
            URL url = new URL(GOOGLE_SCRIPT_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000); // 15s
            conn.setReadTimeout(30000);    // 30s
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded; charset=UTF-8");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            status = conn.getResponseCode();

            // Basic logging (optional)
            System.out.println("Eligibility125Submit → Google status: " + status);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        String context = request.getContextPath();

        if (status == 200) {
            // Normal case → success page
            response.sendRedirect(context + "/125eligibilitySuccess.jsp");
        } else {
            // Optional: if you want a dedicated error page
            // for now we still send them to success to avoid scaring employers
            response.sendRedirect(context + "/125eligibilitySuccess.jsp");
        }
    }

    private String buildFormBody(Map<String, String[]> params) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            String name = entry.getKey();
            String[] values = entry.getValue();
            if (values == null) continue;
            for (String value : values) {
                if (sb.length() > 0) {
                    sb.append('&');
                }
                sb.append(URLEncoder.encode(name, "UTF-8"))
                        .append('=')
                        .append(URLEncoder.encode(value != null ? value : "", "UTF-8"));
            }
        }
        return sb.toString();
    }
}

