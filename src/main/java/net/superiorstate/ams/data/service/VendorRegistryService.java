package net.superiorstate.ams.data.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.persistence.EntityManager;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.data.util.ApiClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VendorRegistryService {

    private static final Gson gson = new Gson();

    /**
     * Fetches approved vendors from the master registry.
     * Returns a list of vendors on success (may be empty), or null on failure.
     * Never throws.
     */
    public static List<Map<String, String>> fetchApprovedVendors(EntityManager em) {
        try {
            if (!AppConfig.isPsp()) {
                return new ArrayList<>();
            }

            String masterUrl = AppConstantDAO.getConstantValue(em, "MASTER_REGISTRY_URL");
            if (masterUrl == null || masterUrl.isBlank()) {
                System.out.println("[VENDOR-REGISTRY] MASTER_REGISTRY_URL constant is not set");
                return null;
            }

            // Strip trailing slash
            if (masterUrl.endsWith("/")) {
                masterUrl = masterUrl.substring(0, masterUrl.length() - 1);
            }

            String url = masterUrl + "/api/v1/registry/vendors";
            ApiClient.ApiResponse resp = ApiClient.getJson(url);

            if (resp.statusCode != 200 || resp.body == null || resp.body.isBlank()) {
                System.out.println("[VENDOR-REGISTRY] Registry fetch failed: status=" + resp.statusCode + ", url=" + url);
                return null;
            }

            List<Map<String, String>> result = new ArrayList<>();
            JsonArray arr = gson.fromJson(resp.body, JsonArray.class);
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                Map<String, String> vendor = new LinkedHashMap<>();
                vendor.put("vendorName", obj.has("vendorName") ? obj.get("vendorName").getAsString() : "");
                vendor.put("vendorUrl", obj.has("vendorUrl") ? obj.get("vendorUrl").getAsString() : "");
                vendor.put("description", obj.has("description") && !obj.get("description").isJsonNull()
                        ? obj.get("description").getAsString() : "");
                result.add(vendor);
            }

            return result;
        } catch (Exception e) {
            System.err.println("[VENDOR-REGISTRY] Error fetching approved vendors: " + e.getMessage());
            return null;
        }
    }
}
