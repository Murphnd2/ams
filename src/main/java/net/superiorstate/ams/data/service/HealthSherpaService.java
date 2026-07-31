package net.superiorstate.ams.data.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.superiorstate.ams.AppConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Sends single-applicant rate quote requests to the HealthSherpa ICHRA Partner API.
 * Uses java.net.http.HttpClient (built-in) and Gson for JSON, following the same shape
 * as ClaudeApiService — but with x-api-key auth (no bearer token, no anthropic-version
 * header) and retry-with-backoff, which ClaudeApiService does not have.
 * <p>
 * API key resolved via AppConfig.getHealthSherpaApiKey() (DB constant first, ssa.properties
 * fallback). Base URL resolved via AppConfig.getHealthSherpaBaseUrl() (DB constant first,
 * ssa.properties fallback, no default — null if neither is configured, in which case this
 * service refuses the call rather than guessing an environment).
 * <p>
 * Called from RateCacheWarmService (warmCounty), which warms the rating-area rate cache.
 */
public class HealthSherpaService {

    private static final Logger log = LogManager.getLogger(HealthSherpaService.class);

    private static final int CONNECT_TIMEOUT_SECONDS = 10;
    private static final int REQUEST_TIMEOUT_SECONDS = 30;
    private static final int MAX_ATTEMPTS = 3;
    private static final long[] BACKOFF_MILLIS = {1000L, 4000L};
    private static final long MAX_RETRY_AFTER_SECONDS = 60L;

    /** Page size requested from HealthSherpa. The API defaults to 20 and does not
     *  disclose a total in the response, so a single unpaginated call silently
     *  truncates. See MAX_PAGES. */
    private static final int PER_PAGE = 100;

    /** Safety cap on pagination. 20 pages x 100 = 2000 plans, far beyond any
     *  real county market. Hitting this means something is wrong, not that a
     *  market is unusually large — it is treated as a failure, not a stop. */
    private static final int MAX_PAGES = 20;

    private static final Gson gson = new Gson();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
            .build();

    /**
     * Requests a single-applicant rate quote from HealthSherpa.
     * <p>
     * Field names in the outbound request are HealthSherpa's own and are intentionally
     * counter-intuitive: {@code fip_code} (not {@code fips_code}), {@code smoker}
     * (not {@code uses_tobacco}). Do not "correct" these.
     *
     * @param zipCode     worksite ZIP code
     * @param fipCode     county FIPS code
     * @param state       two-letter state code
     * @param planYear    plan year
     * @param age         applicant age
     * @param smoker      tobacco use
     * @param offExchange true for off-exchange plans
     * @return a HealthSherpaQuoteResponse — check isSuccess() before reading plans
     */
    public static HealthSherpaQuoteResponse quoteSingleApplicant(
            String zipCode, String fipCode, String state,
            int planYear, int age, boolean smoker, boolean offExchange) {

        String apiKey = AppConfig.getHealthSherpaApiKey();
        if (apiKey == null) {
            log.error("HEALTHSHERPA_API_KEY not configured");
            return HealthSherpaQuoteResponse.failure("HealthSherpa is not configured.");
        }

        String baseUrl = AppConfig.getHealthSherpaBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            log.error("HEALTHSHERPA_BASE_URL not configured");
            return HealthSherpaQuoteResponse.failure("HealthSherpa base URL is not configured.");
        }

        String url = baseUrl + "/api/v1/quotes";

        List<PlanSummary> allPlans = new ArrayList<>();
        for (int currentPage = 1; currentPage <= MAX_PAGES; currentPage++) {
            String json = buildRequestJson(zipCode, fipCode, state, planYear, age, smoker, offExchange, currentPage);
            SendResult sendResult = sendWithRetry(url, apiKey, json);
            if (sendResult.errorMessage != null) {
                log.error("HealthSherpa API failed on page {} for fip {} age {}: {}",
                        currentPage, fipCode, age, sendResult.errorMessage);
                return HealthSherpaQuoteResponse.failure(sendResult.errorMessage);
            }

            HealthSherpaQuoteResponse pageResponse = parseResponse(sendResult.response.body());
            if (!pageResponse.isSuccess()) {
                return pageResponse;
            }

            List<PlanSummary> pagePlans = pageResponse.getPlans();
            allPlans.addAll(pagePlans);

            if (pagePlans.size() < PER_PAGE) {
                log.info("HealthSherpa quote complete: fip={} age={} pages={} totalPlans={}",
                        fipCode, age, currentPage, allPlans.size());
                return HealthSherpaQuoteResponse.success(allPlans.size(), allPlans);
            }
        }

        log.error("HealthSherpa API pagination exceeded MAX_PAGES ({}) for fip {} age {} without a short page — treating as failure",
                MAX_PAGES, fipCode, age);
        return HealthSherpaQuoteResponse.failure("HealthSherpa API returned too many pages (possible pagination fault).");
    }

    /** Result of one HTTP attempt sequence for a single page. Exactly one of the
     *  two fields is non-null. */
    private static class SendResult {
        final HttpResponse<String> response;
        final String errorMessage;

        SendResult(HttpResponse<String> response, String errorMessage) {
            this.response = response;
            this.errorMessage = errorMessage;
        }
    }

    /** Sends one request with the existing retry/backoff policy. Returns the 200
     *  response, or an error message if all attempts were exhausted or the failure
     *  was non-retryable. */
    private static SendResult sendWithRetry(String url, String apiKey, String json) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException e) {
                if (attempt == MAX_ATTEMPTS) {
                    log.error("Error calling HealthSherpa API on attempt {}/{}: {}", attempt, MAX_ATTEMPTS, e.getMessage());
                    return new SendResult(null, "Connection to HealthSherpa failed.");
                }
                long waitMillis = BACKOFF_MILLIS[attempt - 1];
                log.warn("HealthSherpa API IOException on attempt {}/{} ({}) — retrying in {}ms",
                        attempt, MAX_ATTEMPTS, e.getMessage(), waitMillis);
                if (!sleep(waitMillis)) {
                    return new SendResult(null, "HealthSherpa request was interrupted.");
                }
                continue;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("HealthSherpa API call interrupted on attempt {}/{}", attempt, MAX_ATTEMPTS);
                return new SendResult(null, "HealthSherpa request was interrupted.");
            }

            int status = response.statusCode();
            if (status == 200) {
                return new SendResult(response, null);
            }

            boolean retryable = status == 429 || (status >= 500 && status <= 599);
            if (!retryable || attempt == MAX_ATTEMPTS) {
                log.error("HealthSherpa API returned status {} on attempt {}/{}", status, attempt, MAX_ATTEMPTS);
                return new SendResult(null, "HealthSherpa API returned status " + status);
            }

            long waitMillis = resolveBackoffMillis(attempt, response);
            log.warn("HealthSherpa API returned status {} on attempt {}/{} — retrying in {}ms",
                    status, attempt, MAX_ATTEMPTS, waitMillis);
            if (!sleep(waitMillis)) {
                return new SendResult(null, "HealthSherpa request was interrupted.");
            }
        }

        // Unreachable in practice — every branch above returns before the loop exhausts MAX_ATTEMPTS.
        return new SendResult(null, "HealthSherpa API failed after " + MAX_ATTEMPTS + " attempts.");
    }

    private static String buildRequestJson(String zipCode, String fipCode, String state,
                                            int planYear, int age, boolean smoker, boolean offExchange,
                                            int currentPage) {
        JsonObject applicant = new JsonObject();
        applicant.addProperty("age", age);
        applicant.addProperty("relationship", "primary");
        applicant.addProperty("smoker", smoker);

        JsonArray applicants = new JsonArray();
        applicants.add(applicant);

        JsonObject body = new JsonObject();
        body.addProperty("zip_code", zipCode);
        body.addProperty("fip_code", fipCode);
        body.addProperty("state", state);
        body.addProperty("plan_year", planYear);
        body.addProperty("off_ex", offExchange);
        body.addProperty("per_page", PER_PAGE);
        body.addProperty("current_page", currentPage);
        body.addProperty("sort", "premium_asc");
        body.add("applicants", applicants);

        return gson.toJson(body);
    }

    /**
     * On 429, honours a sane Retry-After header (in seconds, <= MAX_RETRY_AFTER_SECONDS)
     * instead of the fixed backoff. Falls back to the fixed backoff for 5xx or a missing/
     * unparsable/out-of-range Retry-After.
     */
    private static long resolveBackoffMillis(int attempt, HttpResponse<String> response) {
        if (response.statusCode() == 429) {
            Optional<String> retryAfter = response.headers().firstValue("Retry-After");
            if (retryAfter.isPresent()) {
                try {
                    long seconds = Long.parseLong(retryAfter.get().trim());
                    if (seconds > 0 && seconds <= MAX_RETRY_AFTER_SECONDS) {
                        return seconds * 1000L;
                    }
                } catch (NumberFormatException ignored) {
                    // not a numeric Retry-After — fall through to fixed backoff
                }
            }
        }
        return BACKOFF_MILLIS[attempt - 1];
    }

    /** Sleeps, swallowing InterruptedException after restoring the interrupt flag. Returns false if interrupted. */
    private static boolean sleep(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static HealthSherpaQuoteResponse parseResponse(String responseBody) {
        try {
            JsonObject resp = gson.fromJson(responseBody, JsonObject.class);
            List<PlanSummary> plans = new ArrayList<>();

            JsonArray plansArray = resp.has("plans") && resp.get("plans").isJsonArray()
                    ? resp.getAsJsonArray("plans") : new JsonArray();

            for (JsonElement el : plansArray) {
                if (el != null && el.isJsonObject()) {
                    plans.add(PlanSummary.fromJson(el.getAsJsonObject()));
                }
            }

            int resultCount = resp.has("result_count") && !resp.get("result_count").isJsonNull()
                    ? resp.get("result_count").getAsInt() : plans.size();

            return HealthSherpaQuoteResponse.success(resultCount, plans);
        } catch (Exception e) {
            log.error("Error parsing HealthSherpa API response", e);
            return HealthSherpaQuoteResponse.failure("Could not parse HealthSherpa response.");
        }
    }

    /** Result of a HealthSherpa quote request. Check isSuccess() before reading plans. */
    public static class HealthSherpaQuoteResponse {
        private final boolean success;
        private final String errorMessage;
        private final int resultCount;
        private final List<PlanSummary> plans;

        private HealthSherpaQuoteResponse(boolean success, String errorMessage, int resultCount, List<PlanSummary> plans) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.resultCount = resultCount;
            this.plans = plans;
        }

        static HealthSherpaQuoteResponse success(int resultCount, List<PlanSummary> plans) {
            return new HealthSherpaQuoteResponse(true, null, resultCount, plans);
        }

        static HealthSherpaQuoteResponse failure(String errorMessage) {
            return new HealthSherpaQuoteResponse(false, errorMessage, 0, new ArrayList<>());
        }

        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public int getResultCount() { return resultCount; }
        public List<PlanSummary> getPlans() { return plans; }
    }

    /**
     * Minimal plan fields needed for a rate illustration.
     * Every field except hiosId, name, premium, and metalLevel may be absent from the response.
     */
    public static class PlanSummary {
        private String hiosId;
        private String name;
        private String metalLevel;
        private Double grossPremium;
        private Double premium;
        private String issuerName;
        private Boolean hsaEligible;
        private Boolean ichraOnly;

        static PlanSummary fromJson(JsonObject p) {
            PlanSummary ps = new PlanSummary();
            ps.hiosId = stringOrNull(p, "hios_id");
            ps.name = stringOrNull(p, "name");
            ps.metalLevel = stringOrNull(p, "metal_level");
            ps.grossPremium = doubleOrNull(p, "gross_premium");
            ps.premium = doubleOrNull(p, "premium");
            ps.hsaEligible = boolOrNull(p, "hsa_eligible");
            ps.ichraOnly = boolOrNull(p, "ichra_only");

            // issuer.name (nested) preferred, legacy top-level issuer_name as fallback
            if (p.has("issuer") && p.get("issuer").isJsonObject()) {
                ps.issuerName = stringOrNull(p.getAsJsonObject("issuer"), "name");
            }
            if (ps.issuerName == null) {
                ps.issuerName = stringOrNull(p, "issuer_name");
            }
            return ps;
        }

        private static String stringOrNull(JsonObject o, String key) {
            return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
        }

        private static Double doubleOrNull(JsonObject o, String key) {
            return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsDouble() : null;
        }

        private static Boolean boolOrNull(JsonObject o, String key) {
            return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsBoolean() : null;
        }

        public String getHiosId() { return hiosId; }
        public String getName() { return name; }
        public String getMetalLevel() { return metalLevel; }
        public Double getGrossPremium() { return grossPremium; }
        public Double getPremium() { return premium; }
        public String getIssuerName() { return issuerName; }
        public Boolean getHsaEligible() { return hsaEligible; }
        public Boolean getIchraOnly() { return ichraOnly; }
    }
}
