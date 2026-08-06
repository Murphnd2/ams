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
 * API key resolved via AppConfig.getAnthropicApiKey() (DB constant first, ssa.properties fallback).
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
        String apiKey = AppConfig.getAnthropicApiKey();
        if (apiKey == null) {
            log.error("ANTHROPIC_API_KEY not configured");
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
        String apiKey = AppConfig.getAnthropicApiKey();
        if (apiKey == null) {
            log.error("ANTHROPIC_API_KEY not configured");
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
     * S20-D — the result of {@link #askDetailed}: the same display-safe {@code answer} text
     * {@link #ask(String, List, String, int)} would return, plus the diagnostic detail that
     * method discards. {@code errorDetail} is {@code null} on success and is a bounded,
     * truncated excerpt otherwise (see {@link #askDetailed} for the bound and why) — callers
     * needing to show it to an end user MUST still gate on their own authorization, exactly as
     * {@code ClaudeApiService} has never made any authorization decision of its own.
     * <p>
     * {@code truncated} (S20-F) is true only when {@code ok} is also true and Anthropic's
     * {@code stop_reason} was {@code "max_tokens"} — {@code answer} already carries a visible
     * notice appended in that case (see {@link #askDetailed}); this field is the machine-
     * readable form of the same fact, for a future caller that wants to act on it without
     * scraping the text.
     */
    public static final class DetailedResult {
        public final String answer;
        public final boolean ok;
        public final Integer statusCode;
        public final String errorDetail;
        public final boolean truncated;

        private DetailedResult(String answer, boolean ok, Integer statusCode, String errorDetail, boolean truncated) {
            this.answer = answer;
            this.ok = ok;
            this.statusCode = statusCode;
            this.errorDetail = errorDetail;
            this.truncated = truncated;
        }
    }

    /** Anthropic error response bodies are small structured JSON, but bounded defensively — see {@link #askDetailed}. */
    private static final int ERROR_DETAIL_MAX_CHARS = 500;

    /**
     * S20-D — a variant of {@link #ask(String, List, String, int)} that returns the Anthropic
     * status code and a bounded excerpt of the response/exception detail alongside the same
     * display-safe answer text, instead of silently discarding everything but a generic
     * fallback string. Added as a NEW method rather than changing {@code ask}'s own behaviour:
     * every existing caller of any {@code ask*} overload in this class is untouched by this
     * commit — none of their method bodies changed by a single character.
     * <p>
     * {@code errorDetail} is truncated to {@link #ERROR_DETAIL_MAX_CHARS} characters. Anthropic
     * error bodies are small structured JSON (e.g. {@code {"type":"error","error":{"type":
     * "not_found_error","message":"model: ..."}}}) and never echo the request's API key — the
     * key is a request header only, never reflected in any Anthropic response — so the bound
     * here is defense against an unexpectedly large or malformed body reaching a rendered page,
     * not against credential leakage. The API key itself is never placed in {@code errorDetail},
     * {@code answer}, or any field of this class, under any branch.
     * <p>
     * Callers MUST NOT surface {@code errorDetail} to anyone but an authorized administrator —
     * this method makes no such decision itself, matching every other method in this class.
     */
    public static DetailedResult askDetailed(String systemPrompt, List<Map<String, String>> messages, String model, int maxTokens) {
        String apiKey = AppConfig.getAnthropicApiKey();
        if (apiKey == null) {
            log.error("ANTHROPIC_API_KEY not configured");
            return new DetailedResult("The AI assistant is not configured. Please contact an administrator.",
                    false, null, "API key not configured (checked DB constant, then ssa.properties)", false);
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
                // S20-F — extractText's own extracted flag decides ok, not the bare HTTP
                // status. When extraction hit its fallback path despite a 200, ok=false is the
                // honest read: extracted.text is one of extractText's two fallback strings,
                // never real content, so there is no working response being mischaracterized
                // here — errorDetail carries the same bounded raw-body excerpt log.warn/
                // log.error already captured inside extractText, via the same 500-char bound
                // truncateForDisplay applies to a non-200 body.
                ExtractedText extracted = extractText(response.body());
                if (extracted.extracted) {
                    // S20-F — usable content is never discarded on a max_tokens stop: the full
                    // extracted text is kept, with a visible notice appended so the admin knows
                    // it may be incomplete, rather than silently handing back cut-off HTML.
                    // ok stays true — this is a working response, not a failure.
                    String answerText = extracted.truncated
                            ? extracted.text + "\n\n---\n⚠️ This response was cut off — it hit the "
                                    + maxTokens + "-token output limit before finishing. Try a shorter "
                                    + "or simpler request, or ask for the rest to continue."
                            : extracted.text;
                    return new DetailedResult(answerText, true, 200, null, extracted.truncated);
                } else {
                    return new DetailedResult(extracted.text, false, 200, truncateForDisplay(response.body()), false);
                }
            } else {
                log.error("Claude API returned status {}: {}", response.statusCode(), response.body());
                String detail = truncateForDisplay(response.body());
                return new DetailedResult("Sorry, I'm having trouble connecting right now. Please try again in a moment.",
                        false, response.statusCode(), detail, false);
            }

        } catch (Exception e) {
            log.error("Error calling Claude API (multi-turn, detailed)", e);
            String detail = truncateForDisplay(e.getClass().getSimpleName() + ": " + e.getMessage());
            return new DetailedResult("Sorry, something went wrong. Please try again.", false, null, detail, false);
        }
    }

    /** Bounds a diagnostic string to {@link #ERROR_DETAIL_MAX_CHARS} before it is ever handed to a caller. */
    private static String truncateForDisplay(String raw) {
        if (raw == null) return null;
        return raw.length() > ERROR_DETAIL_MAX_CHARS
                ? raw.substring(0, ERROR_DETAIL_MAX_CHARS) + "… (truncated)"
                : raw;
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
        String apiKey = AppConfig.getAnthropicApiKey();
        if (apiKey == null) {
            log.error("ANTHROPIC_API_KEY not configured");
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
        String apiKey = AppConfig.getAnthropicApiKey();
        if (apiKey == null) {
            log.error("ANTHROPIC_API_KEY not configured");
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
     * Validates an Anthropic API key by making a minimal API call.
     *
     * @param apiKey the key to validate
     * @return null if valid, or an error message if invalid/failed
     */
    public static String validateApiKey(String apiKey) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", DEFAULT_MODEL);
            body.addProperty("max_tokens", 1);
            body.addProperty("system", "Respond with OK");

            JsonArray messages = new JsonArray();
            JsonObject msg = new JsonObject();
            msg.addProperty("role", "user");
            msg.addProperty("content", "test");
            messages.add(msg);
            body.add("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", API_VERSION)
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return null; // valid
            } else if (response.statusCode() == 401) {
                return "Invalid API key";
            } else {
                return "API returned status " + response.statusCode();
            }
        } catch (Exception e) {
            log.error("Error validating API key", e);
            return "Connection failed: " + e.getMessage();
        }
    }

    /**
     * S20-F — the result of {@link #extractText}: the display text (real content on success,
     * one of the two existing fallback strings on failure — unchanged wording either way) plus
     * whether a real text block was actually found. {@code extracted == false} is what lets
     * {@link #askDetailed} tell a genuine answer apart from a fallback string that merely
     * looks like one; every other caller only ever sees {@code text} via
     * {@link #extractResponseText}, exactly as before this commit.
     * <p>
     * {@code truncated} is true when {@code extracted} is true AND Anthropic's own
     * {@code stop_reason} was {@code "max_tokens"} — real, usable content that was cut off
     * before the model finished, as distinct from {@code extracted == false} (no usable
     * content was found at all). Always false when {@code extracted} is false: a response
     * with no text block has nothing to have been truncated.
     */
    private static final class ExtractedText {
        final String text;
        final boolean extracted;
        final boolean truncated;

        ExtractedText(String text, boolean extracted, boolean truncated) {
            this.text = text;
            this.extracted = extracted;
            this.truncated = truncated;
        }
    }

    /**
     * Extracts the text content from the Anthropic API response JSON.
     * Response format: { "content": [ { "type": "text", "text": "..." }, ... ], "stop_reason": "..." }
     * <p>
     * S20-F — scans every block in {@code content} for {@code type == "text"} rather than
     * assuming index 0 is the text block (S20-E: a non-text block ahead of the text block,
     * e.g. from a future response shape this code predates, broke index-based extraction
     * with no way to recover the actual answer). <b>Multiple text blocks are concatenated in
     * order</b>, not just the first returned: this class's callers include long HTML/CSS page
     * generation (see {@code ProposalAiBuilder}), and returning only the first block would
     * silently truncate output if Anthropic ever splits a long generation across more than
     * one text block. A single-block response (the shape every caller has produced to date)
     * concatenates to exactly that block's text, byte-identical to the prior
     * {@code content.get(0)} behavior.
     * <p>
     * Non-text blocks anywhere in the array (before, between, or after text blocks) are
     * skipped, not treated as errors — only their absence of any text block at all falls
     * through to the existing failure path, which is unchanged: same message, same
     * {@code log.warn} raw-body dump.
     * <p>
     * {@code stop_reason} (S20-F) is read once real text was found, to set
     * {@link ExtractedText#truncated}. This never changes which branch is taken or what text
     * is returned — a {@code max_tokens} stop still returns whatever text was actually
     * generated, unmodified; usable content is never discarded.
     */
    private static ExtractedText extractText(String responseBody) {
        try {
            JsonObject resp = gson.fromJson(responseBody, JsonObject.class);
            JsonArray content = resp.getAsJsonArray("content");
            if (content != null && content.size() > 0) {
                StringBuilder text = new StringBuilder();
                for (int i = 0; i < content.size(); i++) {
                    JsonObject block = content.get(i).getAsJsonObject();
                    if (block.has("type") && "text".equals(block.get("type").getAsString())
                            && block.has("text")) {
                        text.append(block.get("text").getAsString());
                    }
                }
                if (text.length() > 0) {
                    boolean truncated = resp.has("stop_reason") && !resp.get("stop_reason").isJsonNull()
                            && "max_tokens".equals(resp.get("stop_reason").getAsString());
                    return new ExtractedText(text.toString(), true, truncated);
                }
            }
            log.warn("Unexpected response structure: {}", responseBody);
            return new ExtractedText("I received a response but couldn't process it. Please try again.", false, false);
        } catch (Exception e) {
            log.error("Error parsing Claude API response", e);
            return new ExtractedText("Sorry, I couldn't understand the response. Please try again.", false, false);
        }
    }

    /**
     * Thin, behavior-preserving wrapper over {@link #extractText} for every caller that only
     * ever wanted the display string — {@code ask}, {@code askWithContent},
     * {@code askWithStructuredMessages}. Returns exactly {@link ExtractedText#text}; the
     * {@code extracted} flag is discarded here on purpose, matching this method's behavior
     * before S20-F added it.
     */
    private static String extractResponseText(String responseBody) {
        return extractText(responseBody).text;
    }
}
