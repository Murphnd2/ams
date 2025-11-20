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

    // Your Google Apps Script Web App URL
    private static final String GOOGLE_SCRIPT_URL =
            "https://script.google.com/macros/s/AKfycbzoFCW5OYxBVQN57HEvv4iv257lTIQUMETbsb684bQ3W9pBaoZWDV-OZF8ug_KuuylA/exec";

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        // 1) Collect all form parameters and URL-encode them for POST to Google
        String body = buildFormBody(request.getParameterMap());

        // 2) POST to Google Apps Script
        int status = 0;
        try {
            URL url = new URL(GOOGLE_SCRIPT_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded; charset=UTF-8");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            status = conn.getResponseCode();
            // Optional: read conn.getInputStream() or conn.getErrorStream() for debug

        } catch (Exception ex) {
            // Log the error; you can also set an attribute and forward to an error page
            ex.printStackTrace();
        }

        // 3) For now, regardless of status, redirect to success page
        //    (If you want, you can branch on 'status' later.)
        String context = request.getContextPath(); // "" or "/beta", etc.
        response.sendRedirect(context + "/125eligibilitySuccess.jsp");
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
