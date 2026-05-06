package net.superiorstate.ams.data.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import net.superiorstate.ams.data.dao.KnowledgeBaseDAO;
import net.superiorstate.ams.data.dao.KnowledgeChunkDAO;
import net.superiorstate.ams.model.general.KnowledgeBase;
import net.superiorstate.ams.model.general.KnowledgeBaseReloadStrategy;
import net.superiorstate.ams.model.general.KnowledgeBaseSource;
import net.superiorstate.ams.model.general.KnowledgeChunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads and searches knowledge bases driven by the DB registry (knowledge_base table).
 * Each registry row specifies either DB-sourced chunks (knowledge_chunk table) or a
 * JSON classpath file. Loaded content is cached in-memory after initialize() for
 * fast in-process search without per-request DB hits.
 *
 * Thread-safe after initialization — all data is read-only until reload() is called.
 * The reload() method swaps the cache atomically under synchronization.
 */
public class KnowledgeSearchService {

    private static final Logger log = LogManager.getLogger(KnowledgeSearchService.class);
    private static final Gson gson = new Gson();
    private static final int MAX_RESULTS = 8;

    private static final int WEIGHT_KEYWORD = 5;
    private static final int WEIGHT_TITLE   = 3;
    private static final int WEIGHT_SECTION = 2;
    private static final int WEIGHT_CONTENT = 1;

    private EntityManagerFactory emf;
    private volatile boolean initialized = false;
    private volatile List<KBConfig> configs = new ArrayList<>();
    private volatile Map<String, List<Chunk>> chunksByKB = new HashMap<>();

    // ── Inner classes ──────────────────────────────────────────────

    /**
     * Lightweight view of a knowledge_base registry row.
     * Populated from DB at initialize/reload time; keyed by kb_key.
     */
    public static class KBConfig {
        String id;              // = kb_key
        String file;            // = json_filename (null for source=DB)
        String label;
        List<String> keywords = new ArrayList<>();  // unused — KB-level routing keywords not in DB schema
        boolean enabled;        // = is_active
        KnowledgeBaseReloadStrategy reloadStrategy;

        public String getId() { return id; }
        public String getLabel() { return label; }
        public List<String> getKeywords() { return keywords; }
        public boolean isEnabled() { return enabled; }
    }

    /** One knowledge chunk — populated from either a DB entity or a JSON file. */
    public static class Chunk {
        String kbId;
        String kbLabel;
        String title;
        String url;
        String content;
        String section;
        String category;
        List<String> keywords;

        public String getKbId()    { return kbId; }
        public String getKbLabel() { return kbLabel; }
        public String getTitle()   { return title; }
        public String getUrl()     { return url; }
        public String getContent() { return content; }
        public String getSection() { return section; }
    }

    /** A chunk paired with its relevance score. */
    public static class ScoredChunk implements Comparable<ScoredChunk> {
        Chunk chunk;
        int score;

        public ScoredChunk(Chunk chunk, int score) {
            this.chunk = chunk;
            this.score = score;
        }

        public Chunk getChunk() { return chunk; }
        public int getScore()   { return score; }

        @Override
        public int compareTo(ScoredChunk other) {
            return Integer.compare(other.score, this.score); // descending
        }
    }

    // ── Initialization ─────────────────────────────────────────────

    /**
     * Initializes the service from the DB registry.
     * DB-source KBs are loaded via JPA; JSON-source KBs are loaded from the classpath.
     * The supplied EMF is stored so that reload() can re-read without an additional argument.
     *
     * @param emf the application EntityManagerFactory from the servlet context
     */
    public void initialize(EntityManagerFactory emf) {
        this.emf = emf;
        List<KBConfig> newConfigs = new ArrayList<>();
        Map<String, List<Chunk>> newChunks = new HashMap<>();
        loadFromSourcesInto(emf, newConfigs, newChunks);
        this.configs   = newConfigs;
        this.chunksByKB = newChunks;
        this.initialized = true;
        log.info("KnowledgeSearchService ready: {} KBs loaded, {} total chunks",
                newChunks.size(),
                newChunks.values().stream().mapToInt(List::size).sum());
    }

    /**
     * Backward-compatible no-arg overload.
     * Marks the service initialized with an empty registry.
     * Callers should prefer initialize(EntityManagerFactory) for DB loading.
     */
    public void initialize() {
        initialize(null);
    }

    /**
     * Re-reads all knowledge from DB and JSON sources, replacing the in-memory cache.
     * Atomic swap — readers in flight see either the old or the new cache, never a partial state.
     */
    public synchronized void reload() {
        if (emf == null) {
            log.warn("KnowledgeSearchService.reload(): no EMF stored — nothing to reload");
            return;
        }
        List<KBConfig> newConfigs = new ArrayList<>();
        Map<String, List<Chunk>> newChunks = new HashMap<>();
        loadFromSourcesInto(emf, newConfigs, newChunks);
        this.configs   = newConfigs;
        this.chunksByKB = newChunks;
        log.info("KnowledgeSearchService reloaded: {} KBs, {} total chunks",
                newChunks.size(),
                newChunks.values().stream().mapToInt(List::size).sum());
    }

    // ── Loading internals ──────────────────────────────────────────

    private void loadFromSourcesInto(EntityManagerFactory emf,
                                     List<KBConfig> outConfigs,
                                     Map<String, List<Chunk>> outChunks) {
        if (emf == null) {
            log.warn("KnowledgeSearchService: no EntityManagerFactory provided — starting with empty registry");
            return;
        }
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            List<KnowledgeBase> kbs = KnowledgeBaseDAO.getAllActive(em);

            for (KnowledgeBase kb : kbs) {
                KBConfig cfg = toConfig(kb);
                outConfigs.add(cfg);

                List<Chunk> chunks;
                if (kb.getSource() == KnowledgeBaseSource.DB) {
                    List<KnowledgeChunk> dbChunks = KnowledgeChunkDAO.getActiveByKb(em, kb.getId());
                    chunks = toChunks(dbChunks, kb);
                    log.info("Loaded KB '{}' (DB): {} chunks", kb.getLabel(), chunks.size());
                } else {
                    chunks = loadJsonChunks(kb.getJsonFilename(), cfg);
                    if (chunks == null) chunks = Collections.emptyList();
                    log.info("Loaded KB '{}' (JSON): {} chunks", kb.getLabel(), chunks.size());
                }
                outChunks.put(kb.getKbKey(), chunks);
            }
        } catch (Exception e) {
            log.warn("KnowledgeSearchService: DB load failed — starting with empty registry. Cause: {}",
                    e.getMessage());
        } finally {
            if (em != null && em.isOpen()) {
                try { em.close(); } catch (Exception ignored) {}
            }
        }
    }

    private KBConfig toConfig(KnowledgeBase kb) {
        KBConfig cfg = new KBConfig();
        cfg.id             = kb.getKbKey();
        cfg.file           = kb.getJsonFilename();
        cfg.label          = kb.getLabel();
        cfg.enabled        = kb.isActive();
        cfg.reloadStrategy = kb.getReloadStrategy();
        // cfg.keywords stays empty — KB-level routing disabled (falls through to all eligible)
        return cfg;
    }

    private List<Chunk> toChunks(List<KnowledgeChunk> dbChunks, KnowledgeBase kb) {
        List<Chunk> result = new ArrayList<>();
        for (KnowledgeChunk kc : dbChunks) {
            Chunk c = new Chunk();
            c.kbId    = kb.getKbKey();
            c.kbLabel = kb.getLabel();
            c.title   = kc.getTitle()   != null ? kc.getTitle()   : "";
            c.url     = "";
            c.content = kc.getContent() != null ? kc.getContent() : "";
            c.section = kc.getSection() != null ? kc.getSection() : "";
            c.category = kc.getAccountType() != null ? kc.getAccountType() : "";
            c.keywords = new ArrayList<>();
            if (kc.getKeywords() != null && !kc.getKeywords().isBlank()) {
                for (String kw : kc.getKeywords().split(",")) {
                    String t = kw.trim().toLowerCase();
                    if (!t.isEmpty()) c.keywords.add(t);
                }
            }
            result.add(c);
        }
        return result;
    }

    private List<Chunk> loadJsonChunks(String filename, KBConfig cfg) {
        if (filename == null || filename.isBlank()) {
            log.warn("KB '{}' has source=JSON but no json_filename configured", cfg.id);
            return null;
        }
        String path = "knowledge/" + filename;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                log.warn("KB JSON file not found on classpath: {}", path);
                return null;
            }
            JsonObject root = gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
            JsonArray chunksArray = root.getAsJsonArray("chunks");
            List<Chunk> chunks = new ArrayList<>();
            for (JsonElement el : chunksArray) {
                JsonObject obj = el.getAsJsonObject();
                Chunk c = new Chunk();
                c.kbId     = cfg.id;
                c.kbLabel  = cfg.label;
                c.title    = getStr(obj, "title");
                c.url      = getStr(obj, "url");
                c.content  = getStr(obj, "content");
                c.section  = getStr(obj, "section");
                c.category = getStr(obj, "category");
                c.keywords = new ArrayList<>();
                if (obj.has("keywords") && obj.get("keywords").isJsonArray()) {
                    for (JsonElement kw : obj.getAsJsonArray("keywords")) {
                        c.keywords.add(kw.getAsString().toLowerCase());
                    }
                }
                chunks.add(c);
            }
            return chunks;
        } catch (Exception e) {
            log.error("Error loading KB JSON file: {}", path, e);
            return null;
        }
    }

    private String getStr(JsonObject obj, String field) {
        return obj.has(field) && !obj.get(field).isJsonNull() ? obj.get(field).getAsString() : "";
    }

    // ── New public methods ─────────────────────────────────────────

    /**
     * Returns all active chunks from KBs whose reload_strategy = ALWAYS_LOAD,
     * filtered to the supplied eligible KB key set (role-based access).
     * Used by email-drafter prompts that must inject style/voice rules into every prompt.
     */
    public List<Chunk> getAlwaysLoadChunks(Set<String> eligibleKbKeys) {
        List<Chunk> result = new ArrayList<>();
        for (KBConfig cfg : configs) {
            if (!cfg.enabled) continue;
            if (cfg.reloadStrategy != KnowledgeBaseReloadStrategy.ALWAYS_LOAD) continue;
            if (!eligibleKbKeys.contains(cfg.id)) continue;
            List<Chunk> chunks = chunksByKB.get(cfg.id);
            if (chunks != null) result.addAll(chunks);
        }
        return result;
    }

    // ── Existing public API (signatures unchanged) ─────────────────

    /**
     * Searches eligible knowledge bases for chunks relevant to the question.
     *
     * @param question    the user's question
     * @param eligibleKBs set of KB keys the user has access to (from getEligibleKBs)
     * @return top-ranked chunks, up to MAX_RESULTS
     */
    public List<ScoredChunk> search(String question, Set<String> eligibleKBs) {
        Set<String> queryWords = tokenize(question);
        if (queryWords.isEmpty()) return Collections.emptyList();

        Set<String> targetKBs = routeToKBs(queryWords, eligibleKBs);

        List<ScoredChunk> allScored = new ArrayList<>();
        for (String kbId : targetKBs) {
            List<Chunk> chunks = chunksByKB.get(kbId);
            if (chunks == null) continue;
            for (Chunk c : chunks) {
                int score = scoreChunk(c, queryWords);
                if (score > 0) allScored.add(new ScoredChunk(c, score));
            }
        }

        Collections.sort(allScored);
        return allScored.stream().limit(MAX_RESULTS).collect(Collectors.toList());
    }

    /**
     * Returns eligible KB keys for the given role.
     *
     * Non-admins: summit_official + summit_supplemental (the public-facing Summit content).
     * Admins: all active KBs.
     *
     * Note: email-assistant KBs (style_voice, federal_rules, ssa_business) are admin-only
     * in v1 — they contain SSA-internal operational content.
     */
    public Set<String> getEligibleKBs(boolean isAdmin) {
        Set<String> eligible = new HashSet<>();
        for (KBConfig cfg : configs) {
            if (!cfg.enabled) continue;
            if (isAdmin) {
                eligible.add(cfg.id);
            } else {
                if ("summit_official".equals(cfg.id) || "summit_supplemental".equals(cfg.id)) {
                    eligible.add(cfg.id);
                }
            }
        }
        return eligible;
    }

    /**
     * Builds the context string to inject into a Claude prompt from search results.
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
        return initialized;
    }

    // ── Search internals ───────────────────────────────────────────

    /**
     * Routes to relevant KBs based on KB-level keyword matching.
     * KB-level keywords are empty in the current DB registry (not stored in schema),
     * so this always falls through to "search all eligible KBs".
     */
    private Set<String> routeToKBs(Set<String> queryWords, Set<String> eligibleKBs) {
        Map<String, Integer> kbScores = new HashMap<>();
        for (KBConfig cfg : configs) {
            if (!cfg.enabled || !eligibleKBs.contains(cfg.id)) continue;
            int score = 0;
            for (String kw : cfg.keywords) {
                if (queryWords.contains(kw)) score++;
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
            return eligibleKBs.stream()
                    .filter(id -> chunksByKB.containsKey(id))
                    .collect(Collectors.toSet());
        }
        return kbScores.keySet();
    }

    private int scoreChunk(Chunk c, Set<String> queryWords) {
        int score = 0;

        if (c.keywords != null) {
            for (String kw : c.keywords) {
                if (queryWords.contains(kw)) score += WEIGHT_KEYWORD;
            }
        }

        Set<String> titleWords = tokenize(c.title);
        for (String tw : titleWords) {
            if (queryWords.contains(tw)) score += WEIGHT_TITLE;
        }

        Set<String> sectionWords = tokenize(c.section);
        for (String sw : sectionWords) {
            if (queryWords.contains(sw)) score += WEIGHT_SECTION;
        }

        Set<String> contentWords = tokenize(c.content);
        for (String cw : contentWords) {
            if (queryWords.contains(cw)) score += WEIGHT_CONTENT;
        }

        return score;
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Collections.emptySet();
        String[] words = text.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+");
        Set<String> result = new HashSet<>();
        for (String w : words) {
            if (w.length() > 1 && !STOP_WORDS.contains(w)) result.add(w);
        }
        return result;
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
