package net.superiorstate.ams.data.util;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class ApiClient {

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final HttpClient redirectClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final Gson gson = new Gson();

    /**
     * POST JSON to a URL. Returns the HTTP status code and body.
     * Throws RuntimeException on connection failure.
     */
    public static ApiResponse postJson(String url, Map<String, String> payload) {
        return postJson(url, payload, null);
    }

    /**
     * POST any object as JSON to a URL. Use for complex/nested payloads.
     */
    public static ApiResponse postJsonObject(String url, Object payload, String bearerToken) {
        try {
            String json = gson.toJson(payload);
            return postJsonString(url, json, bearerToken);
        } catch (Exception e) {
            throw new RuntimeException("API call failed to " + url + ": " + e.getMessage(), e);
        }
    }

    public static ApiResponse postJson(String url, Map<String, String> payload, String bearerToken) {
        return postJsonString(url, gson.toJson(payload), bearerToken);
    }

    private static ApiResponse postJsonString(String url, String json, String bearerToken) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(json));

            if (bearerToken != null) {
                builder.header("Authorization", "Bearer " + bearerToken);
            }

            HttpResponse<String> response = client.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString());

            return new ApiResponse(response.statusCode(), response.body());
        } catch (Exception e) {
            throw new RuntimeException("API call failed to " + url + ": " + e.getMessage(), e);
        }
    }

    /**
     * GET JSON from a URL. Uses redirect-following client with 5s timeout.
     * On any exception, returns ApiResponse with statusCode = -1 and empty body.
     */
    public static ApiResponse getJson(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = redirectClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            return new ApiResponse(response.statusCode(), response.body());
        } catch (Exception e) {
            return new ApiResponse(-1, "");
        }
    }

    /**
     * Read request body as a Map from JSON.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> readJsonBody(HttpServletRequest request) {
        try {
            String body = new String(request.getInputStream().readAllBytes());
            return gson.fromJson(body, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    public static class ApiResponse {
        public final int statusCode;
        public final String body;

        public ApiResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
    }
}
