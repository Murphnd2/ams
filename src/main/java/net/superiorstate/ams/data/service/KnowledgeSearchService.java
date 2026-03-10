package net.superiorstate.ams.data.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads knowledge base JSON files from the classpath at startup,
 * routes questions to relevant KBs, and ranks chunks by keyword relevance.
 *
 * Thread-safe after initialization — all data is read-only once loaded.
 */
public class KnowledgeSearchService {

    private static final Logger log = LogManager.getLogger(KnowledgeSearchService.class);
    private static final Gson gson = new Gson();
    private static final int MAX_RESULTS = 8;

    // Scoring weights
    private static final int WEIGHT_KEYWORD = 5;
    private static final int WEIGHT_TITLE = 3;
    private static final int WEIGHT_SECTION = 2;
    private static final int WEIGHT_CONTENT = 1;

    /** Loaded knowledge base configurations from knowledge-config.json */
    private List<KBConfig> configs = new ArrayList<>();

    /** Chunk data keyed by KB id */
    private Map<String, List<Chunk>> chunksByKB = new HashMap<>();

    // ── Inner classes ──────────────────────────────────────────────

    /** Mirrors a single entry in knowledge-config.json */
    public static class KBConfig {
        String id;
        String file;
        String label;
        List<String> keywords;
        boolean enabled;

        public String getId() { return id; }
        public String getLabel() { return label; }
        public List<String> getKeywords() { return keywords; }
        public boolean isEnabled() { return enabled; }
    }

    /** One chunk from a knowledge base JSON file */
    public static class Chunk {
        String kbId;
        String kbLabel;
        String title;
        String url;
        String content;
        String section;
        String category;
        List<String> keywords;

        public String getKbLabel() { return kbLabel; }
        public String getTitle() { return title; }
        public String getUrl() { return url; }
        public String getContent() { return content; }
        public String getSection() { return section; }
    }

    /** A chunk paired with its relevance score */
    public static class ScoredChunk implements Comparable<ScoredChunk> {
        Chunk chunk;
        int score;

        public ScoredChunk(Chunk chunk, int score) {
            this.chunk = chunk;
            this.score = score;
        }
        public Chunk getChunk() { return chunk; }
        public int getScore() { return score; }

        @Override
        public int compareTo(ScoredChunk other) {
            return Integer.compare(other.score, this.score); // descending
        }
    }

    // ── Initialization ─────────────────────────────────────────────

    /**
     * Loads knowledge-config.json and all referenced KB files from the classpath.
     * Call once at application startup (e.g., from a ServletContextListener or the first request).
     */
    public void initialize() {
        log.info("Initializing KnowledgeSearchService...");
        loadConfig();
        for (KBConfig cfg : configs) {
            if (cfg.enabled) {
                loadKnowledgeBase(cfg);
            }
        }
        int totalChunks = chunksByKB.values().stream().mapToInt(List::size).sum();
        log.info("KnowledgeSearchService ready: {} KBs loaded, {} total chunks", chunksByKB.size(), totalChunks);
    }

    private void loadConfig() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("knowledge/knowledge-config.json")) {
            if (is == null) {
                log.error("knowledge-config.json not found on classpath");
                return;
            }
            JsonObject root = gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
            JsonArray kbArray = root.getAsJsonArray("knowledgeBases");
            for (JsonElement el : kbArray) {
                JsonObject obj = el.getAsJsonObject();
                KBConfig cfg = new KBConfig();
                cfg.id = obj.get("id").getAsString();
                cfg.file = obj.get("file").getAsString();
                cfg.label = obj.get("label").getAsString();
                cfg.enabled = obj.has("enabled") && obj.get("enabled").getAsBoolean();
                cfg.keywords = new ArrayList<>();
                if (obj.has("keywords")) {
                    for (JsonElement kw : obj.getAsJsonArray("keywords")) {
                        cfg.keywords.add(kw.getAsString().toLowerCase());
                    }
                }
                configs.add(cfg);
            }
            log.info("Loaded {} KB configs", configs.size());
        } catch (Exception e) {
            log.error("Error loading knowledge-config.json", e);
        }
    }

    private void loadKnowledgeBase(KBConfig cfg) {
        String path = "knowledge/" + cfg.file;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                log.warn("KB file not found: {}", path);
                return;
            }
            JsonObject root = gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
            JsonArray chunksArray = root.getAsJsonArray("chunks");
            List<Chunk> chunks = new ArrayList<>();
            for (JsonElement el : chunksArray) {
                JsonObject obj = el.getAsJsonObject();
                Chunk c = new Chunk();
                c.kbId = cfg.id;
                c.kbLabel = cfg.label;
                c.title = getStr(obj, "title");
                c.url = getStr(obj, "url");
                c.content = getStr(obj, "content");
                c.section = getStr(obj, "section");
                c.category = getStr(obj, "category");
                c.keywords = new ArrayList<>();
                if (obj.has("keywords") && obj.get("keywords").isJsonArray()) {
                    for (JsonElement kw : obj.getAsJsonArray("keywords")) {
                        c.keywords.add(kw.getAsString().toLowerCase());
                    }
                }
                chunks.add(c);
            }
            chunksByKB.put(cfg.id, chunks);
            log.info("Loaded KB '{}': {} chunks", cfg.label, chunks.size());
        } catch (Exception e) {
            log.error("Error loading KB file: {}", path, e);
        }
    }

    private String getStr(JsonObject obj, String field) {
        return obj.has(field) && !obj.get(field).isJsonNull() ? obj.get(field).getAsString() : "";
    }

    // ── Search ─────────────────────────────────────────────────────

    /**
     * Searches eligible knowledge bases for chunks relevant to the question.
     *
     * @param question     the user's question
     * @param eligibleKBs  set of KB ids the user has access to (based on role)
     * @return top-ranked chunks with source labels, up to MAX_RESULTS
     */
    public List<ScoredChunk> search(String question, Set<String> eligibleKBs) {
        Set<String> queryWords = tokenize(question);
        if (queryWords.isEmpty()) return Collections.emptyList();

        // Step 1: Route — determine which KBs are relevant to the question
        Set<String> targetKBs = routeToKBs(queryWords, eligibleKBs);

        // Step 2: Score all chunks in target KBs
        List<ScoredChunk> allScored = new ArrayList<>();
        for (String kbId : targetKBs) {
            List<Chunk> chunks = chunksByKB.get(kbId);
            if (chunks == null) continue;
            for (Chunk c : chunks) {
                int score = scoreChunk(c, queryWords);
                if (score > 0) {
                    allScored.add(new ScoredChunk(c, score));
                }
            }
        }

        // Step 3: Sort descending by score, return top N
        Collections.sort(allScored);
        return allScored.stream().limit(MAX_RESULTS).collect(Collectors.toList());
    }

    /**
     * Determines which KBs to search based on question keywords matching KB-level keywords.
     * If no strong match, defaults to all eligible KBs.
     */
    private Set<String> routeToKBs(Set<String> queryWords, Set<String> eligibleKBs) {
        Map<String, Integer> kbScores = new HashMap<>();
        for (KBConfig cfg : configs) {
            if (!cfg.enabled || !eligibleKBs.contains(cfg.id)) continue;
            int score = 0;
            for (String kw : cfg.keywords) {
                if (queryWords.contains(kw)) score++;
                // Also check multi-word KB keywords against the raw query words
                if (kw.contains(" ")) {
                    String[] parts = kw.split("\\s+");
                    boolean allMatch = true;
                    for (String part : parts) {
                        if (!queryWords.contains(part)) { allMatch = false; break; }
                    }
                    if (allMatch) score += 2;
                }
            }
            if (score > 0) kbScores.put(cfg.id, score);
        }

        if (kbScores.isEmpty()) {
            // No strong match — search all eligible KBs
            return eligibleKBs.stream()
                    .filter(id -> chunksByKB.containsKey(id))
                    .collect(Collectors.toSet());
        }
        return kbScores.keySet();
    }

    /**
     * Scores a single chunk against the query words.
     * Keyword matches weighted highest, then title/section, then content.
     */
    private int scoreChunk(Chunk c, Set<String> queryWords) {
        int score = 0;

        // Keyword array matches (highest value)
        if (c.keywords != null) {
            for (String kw : c.keywords) {
                if (queryWords.contains(kw)) score += WEIGHT_KEYWORD;
            }
        }

        // Title matches
        Set<String> titleWords = tokenize(c.title);
        for (String tw : titleWords) {
            if (queryWords.contains(tw)) score += WEIGHT_TITLE;
        }

        // Section matches
        Set<String> sectionWords = tokenize(c.section);
        for (String sw : sectionWords) {
            if (queryWords.contains(sw)) score += WEIGHT_SECTION;
        }

        // Content matches (lower weight, but catches things the other fields miss)
        Set<String> contentWords = tokenize(c.content);
        for (String cw : contentWords) {
            if (queryWords.contains(cw)) score += WEIGHT_CONTENT;
        }

        return score;
    }

    /**
     * Tokenizes text into lowercase words, stripping punctuation and common stop words.
     */
    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Collections.emptySet();
        String[] words = text.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+");
        Set<String> result = new HashSet<>();
        for (String w : words) {
            if (w.length() > 1 && !STOP_WORDS.contains(w)) {
                result.add(w);
            }
        }
        return result;
    }

    /**
     * Returns the set of KB ids that are eligible for the given role.
     * "admin" sees all enabled KBs. "user" sees only KBs without an accessRole restriction
     * (in the current config, summit and summit_videos).
     *
     * @param isAdmin true if the user has admin role
     * @return set of eligible KB ids
     */
    public Set<String> getEligibleKBs(boolean isAdmin) {
        Set<String> eligible = new HashSet<>();
        for (KBConfig cfg : configs) {
            if (!cfg.enabled) continue;
            if (isAdmin) {
                eligible.add(cfg.id);
            } else {
                // Non-admin users get summit and summit_videos only.
                // Admin-only KBs: wave, business_continuity, backup_recovery, automation_email_builder
                if ("summit".equals(cfg.id) || "summit_videos".equals(cfg.id)) {
                    eligible.add(cfg.id);
                }
            }
        }
        return eligible;
    }

    /**
     * Builds the context string to send to Claude from the search results.
     * Each chunk is labeled with its source KB for citation purposes.
     */
    public String buildContext(List<ScoredChunk> results) {
        if (results.isEmpty()) return "No relevant information found in the knowledge bases.";

        StringBuilder sb = new StringBuilder();
        sb.append("=== KNOWLEDGE BASE CONTEXT ===\n\n");
        for (int i = 0; i < results.size(); i++) {
            Chunk c = results.get(i).getChunk();
            sb.append("--- Source: ").append(c.kbLabel);
            if (!c.title.isBlank()) sb.append(" | ").append(c.title);
            if (!c.section.isBlank()) sb.append(" > ").append(c.section);
            sb.append(" ---\n");
            sb.append(c.content).append("\n");
            if (!c.url.isBlank()) sb.append("Link: ").append(c.url).append("\n");
            sb.append("\n");
        }
        return sb.toString();
    }

    public boolean isInitialized() {
        return !chunksByKB.isEmpty();
    }

    // ── Stop words ─────────────────────────────────────────────────

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "is", "at", "which", "on", "a", "an", "and", "or", "but",
            "in", "with", "to", "for", "of", "not", "no", "can", "do", "does",
            "did", "has", "have", "had", "be", "been", "being", "was", "were",
            "am", "are", "it", "its", "this", "that", "these", "those",
            "my", "your", "his", "her", "our", "their", "what", "how", "when",
            "where", "who", "why", "if", "then", "so", "up", "out", "about",
            "into", "from", "by", "as", "will", "would", "should", "could",
            "may", "might", "shall", "must", "need", "me", "we", "you", "he",
            "she", "they", "them", "us", "im", "just", "also", "very", "too"
    );
}
