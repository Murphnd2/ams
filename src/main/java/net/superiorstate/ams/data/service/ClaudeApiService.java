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
import java.util.List;
import java.util.Map;

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
    private static final int TIMEOUT_SECONDS = 60;

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
     * Multi-turn variant: sends a full conversation history to Claude.
     *
     * @param systemPrompt the system prompt
     * @param messages     list of maps with "role" and "content" keys
     * @return Claude's response text, or an error message if the call fails
     */
    public static String ask(String systemPrompt, List<Map<String, String>> messages) {
        return ask(systemPrompt, messages, DEFAULT_MODEL, DEFAULT_MAX_TOKENS);
    }

    /**
     * Multi-turn variant with explicit model and max_tokens override.
     * Use this for tasks requiring a more capable model (e.g., Sonnet for HTML generation).
     *
     * @param systemPrompt the system prompt
     * @param messages     list of maps with "role" and "content" keys
     * @param model        the Anthropic model ID (e.g., "claude-sonnet-4-5-20250514")
     * @param maxTokens    maximum tokens in the response
     * @return Claude's response text, or an error message if the call fails
     */
    public static String ask(String systemPrompt, List<Map<String, String>> messages, String model, int maxTokens) {
        String apiKey = AppConfig.get("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank() || "FILL_ME_IN".equals(apiKey)) {
            log.error("ANTHROPIC_API_KEY not configured in ssa.properties");
            return "The AI assistant is not configured. Please contact an administrator.";
        }

        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", model);
            body.addProperty("max_tokens", maxTokens);
            body.addProperty("system", systemPrompt);

            JsonArray msgArray = new JsonArray();
            for (Map<String, String> m : messages) {
                JsonObject msg = new JsonObject();
                msg.addProperty("role", m.get("role"));
                msg.addProperty("content", m.get("content"));
                msgArray.add(msg);
            }
            body.add("messages", msgArray);

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
            log.error("Error calling Claude API (multi-turn)", e);
            return "Sorry, something went wrong. Please try again.";
        }
    }

    /**
     * Sends a message with structured content blocks (text + documents) to Claude.
     * Used for PDF analysis where the user message contains both text and a base64 document.
     *
     * @param systemPrompt  the system prompt
     * @param contentBlocks a JsonArray of content blocks (text blocks and document blocks)
     * @param model         the model ID
     * @param maxTokens     max response tokens
     * @return Claude's response text
     */
    public static String askWithContent(String systemPrompt, JsonArray contentBlocks, String model, int maxTokens) {
        String apiKey = AppConfig.get("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank() || "FILL_ME_IN".equals(apiKey)) {
            log.error("ANTHROPIC_API_KEY not configured in ssa.properties");
            return "The AI assistant is not configured. Please contact an administrator.";
        }

        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", model);
            body.addProperty("max_tokens", maxTokens);
            body.addProperty("system", systemPrompt);

            JsonArray messages = new JsonArray();
            JsonObject msg = new JsonObject();
            msg.addProperty("role", "user");
            msg.add("content", contentBlocks);
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
            log.error("Error calling Claude API (structured content)", e);
            return "Sorry, something went wrong. Please try again.";
        }
    }

    /**
     * Convenience overload using default model and max tokens.
     */
    public static String askWithContent(String systemPrompt, JsonArray contentBlocks) {
        return askWithContent(systemPrompt, contentBlocks, DEFAULT_MODEL, DEFAULT_MAX_TOKENS);
    }

    /**
     * Multi-turn variant supporting structured content blocks in any message.
     * Each message map must have "role" (String). The "content" value can be:
     *   - a String (plain text message, same as existing ask())
     *   - a JsonArray (structured content blocks with text + documents)
     *
     * @param systemPrompt the system prompt
     * @param messages     list of maps with "role" and "content" keys
     * @param model        the model ID
     * @param maxTokens    max response tokens
     * @return Claude's response text
     */
    public static String askWithStructuredMessages(String systemPrompt, List<Map<String, Object>> messages, String model, int maxTokens) {
        String apiKey = AppConfig.get("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank() || "FILL_ME_IN".equals(apiKey)) {
            log.error("ANTHROPIC_API_KEY not configured in ssa.properties");
            return "The AI assistant is not configured. Please contact an administrator.";
        }

        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", model);
            body.addProperty("max_tokens", maxTokens);
            body.addProperty("system", systemPrompt);

            JsonArray msgArray = new JsonArray();
            for (Map<String, Object> m : messages) {
                JsonObject msg = new JsonObject();
                msg.addProperty("role", (String) m.get("role"));
                Object content = m.get("content");
                if (content instanceof JsonArray) {
                    msg.add("content", (JsonArray) content);
                } else {
                    msg.addProperty("content", content.toString());
                }
                msgArray.add(msg);
            }
            body.add("messages", msgArray);

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
            log.error("Error calling Claude API (structured messages)", e);
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
