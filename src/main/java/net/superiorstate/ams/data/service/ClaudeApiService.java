package net.superiorstate.ams.data.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.persistence.EntityManager;
import net.superiorstate.ams.AppConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Sends requests to the Anthropic Messages API and returns the response text.
 * Uses java.net.http.HttpClient (built-in) and Gson for JSON.
 *
 * API key is read from ssa.properties via AppConfig (ANTHROPIC_API_KEY).
 * The EntityManager parameter is retained on ask() for caller compatibility but is no longer used.
 */
public class ClaudeApiService {

    private static final Logger log = LogManager.getLogger(ClaudeApiService.class);
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";
    private static final String DEFAULT_MODEL = "claude-haiku-4-5-20251001";
    private static final int DEFAULT_MAX_TOKENS = 1024;
    private static final int TIMEOUT_SECONDS = 30;

    private static final Gson gson = new Gson();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .build();

    /**
     * Sends a question with context chunks to Claude and returns the response text.
     *
     * @param em           EntityManager (retained for caller compatibility — not used internally)
     * @param systemPrompt the system prompt instructing Claude's behavior
     * @param userMessage  the user's question combined with context chunks
     * @return Claude's response text, or an error message if the call fails
     */
    public static String ask(EntityManager em, String systemPrompt, String userMessage) {
        String apiKey = AppConfig.get("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank() || "FILL_ME_IN".equals(apiKey)) {
            log.error("ANTHROPIC_API_KEY not configured in ssa.properties");
            return "The AI assistant is not configured. Please contact an administrator.";
        }

        try {
            // Build request body
            JsonObject body = new JsonObject();
            body.addProperty("model", DEFAULT_MODEL);
            body.addProperty("max_tokens", DEFAULT_MAX_TOKENS);
            body.addProperty("system", systemPrompt);

            JsonArray messages = new JsonArray();
            JsonObject msg = new JsonObject();
            msg.addProperty("role", "user");
            msg.addProperty("content", userMessage);
            messages.add(msg);
            body.add("messages", messages);

            String json = gson.toJson(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", API_VERSION)
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return extractResponseText(response.body());
            } else {
                log.error("Claude API returned status {}: {}", response.statusCode(), response.body());
                return "Sorry, I'm having trouble connecting right now. Please try again in a moment.";
            }

        } catch (Exception e) {
            log.error("Error calling Claude API", e);
            return "Sorry, something went wrong. Please try again.";
        }
    }

    /**
     * Extracts the text content from the Anthropic API response JSON.
     * Response format: { "content": [ { "type": "text", "text": "..." } ] }
     */
    private static String extractResponseText(String responseBody) {
        try {
            JsonObject resp = gson.fromJson(responseBody, JsonObject.class);
            JsonArray content = resp.getAsJsonArray("content");
            if (content != null && content.size() > 0) {
                JsonObject first = content.get(0).getAsJsonObject();
                if ("text".equals(first.get("type").getAsString())) {
                    return first.get("text").getAsString();
                }
            }
            log.warn("Unexpected response structure: {}", responseBody);
            return "I received a response but couldn't process it. Please try again.";
        } catch (Exception e) {
            log.error("Error parsing Claude API response", e);
            return "Sorry, I couldn't understand the response. Please try again.";
        }
    }
}
