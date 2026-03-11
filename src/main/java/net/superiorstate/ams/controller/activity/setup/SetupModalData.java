package net.superiorstate.ams.controller.activity.setup;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.sales.agency.Prospect;
import net.superiorstate.ams.model.sales.agency.Rate;
import net.superiorstate.ams.model.sales.offering.Enhancement;
import net.superiorstate.ams.model.sales.offering.LOS;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * JSON endpoint returning all setup modal data from AmsDataGlobal.
 * Called via AJAX when the Setup tab is shown in the Add Activity modal.
 * Reads from the already-cached AmsDataGlobal — does NOT query the database.
 */
@WebServlet(name = "SetupModalData", value = "/SetupModalData")
public class SetupModalData extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");

        if (global == null || local == null) {
            response.setStatus(500);
            response.getWriter().print("{\"error\":\"Not initialized\"}");
            return;
        }

        PrintWriter out = response.getWriter();
        StringBuilder json = new StringBuilder(1024);
        json.append("{");

        // homeAgencyId
        json.append("\"homeAgencyId\":");
        json.append(global.getPspHomeAgencyId() != null ? global.getPspHomeAgencyId() : "null");

        // currentPersonId
        json.append(",\"currentPersonId\":").append(local.getCurrentPerson().getId());

        // agencies array
        json.append(",\"agencies\":[");
        List<Agency> agencies = global.getAgencies();
        if (agencies != null) {
            for (int i = 0; i < agencies.size(); i++) {
                if (i > 0) json.append(",");
                Agency a = agencies.get(i);
                json.append("{\"id\":").append(a.getId());
                json.append(",\"name\":\"").append(escapeJson(a.getName())).append("\"");
                json.append(",\"rateIds\":\"").append(escapeJson(global.getAgencyRateIds(a.getId()))).append("\"");
                json.append("}");
            }
        }
        json.append("]");

        // prospects array
        json.append(",\"prospects\":[");
        List<Prospect> prospects = global.getProspects();
        if (prospects != null) {
            for (int i = 0; i < prospects.size(); i++) {
                if (i > 0) json.append(",");
                Prospect p = prospects.get(i);
                json.append("{\"id\":").append(p.getId());
                json.append(",\"name\":\"").append(escapeJson(p.getName())).append("\"");
                json.append(",\"agencyIds\":\"").append(escapeJson(global.getProspectAgencyIds(p.getId()))).append("\"");
                json.append("}");
            }
        }
        json.append("]");

        // agents array
        json.append(",\"agents\":[");
        List<AmsDataGlobal.AgentInfo> agents = global.getSetupAgents();
        if (agents != null) {
            for (int i = 0; i < agents.size(); i++) {
                if (i > 0) json.append(",");
                AmsDataGlobal.AgentInfo ag = agents.get(i);
                json.append("{\"id\":").append(ag.getId());
                json.append(",\"name\":\"").append(escapeJson(ag.getName())).append("\"");
                json.append(",\"agencyIds\":\"").append(escapeJson(ag.getAgencyIds())).append("\"");
                json.append("}");
            }
        }
        json.append("]");

        // rates array
        json.append(",\"rates\":[");
        List<Rate> rates = global.getRateList();
        if (rates != null) {
            for (int i = 0; i < rates.size(); i++) {
                if (i > 0) json.append(",");
                Rate r = rates.get(i);
                json.append("{\"id\":").append(r.getId());
                json.append(",\"description\":\"").append(escapeJson(r.getDescription())).append("\"");
                json.append("}");
            }
        }
        json.append("]");

        // losList array
        json.append(",\"losList\":[");
        List<LOS> losList = global.getLosList();
        if (losList != null) {
            for (int i = 0; i < losList.size(); i++) {
                if (i > 0) json.append(",");
                LOS los = losList.get(i);
                json.append("{\"id\":").append(los.getId());
                json.append(",\"description\":\"").append(escapeJson(los.getDescription())).append("\"");
                json.append("}");
            }
        }
        json.append("]");

        // enhancements array
        json.append(",\"enhancements\":[");
        List<Enhancement> enhancements = global.getEnhancementList();
        if (enhancements != null) {
            for (int i = 0; i < enhancements.size(); i++) {
                if (i > 0) json.append(",");
                Enhancement enh = enhancements.get(i);
                json.append("{\"id\":").append(enh.getId());
                json.append(",\"description\":\"").append(escapeJson(enh.getDescription())).append("\"");
                json.append(",\"serviceItemId\":");
                json.append(enh.getServiceItem() != null ? enh.getServiceItem().getId() : "null");
                json.append("}");
            }
        }
        json.append("]");

        // rateLosMap object — { "rateId": [losId, losId, ...], ... }
        json.append(",\"rateLosMap\":{");
        Map<Long, List<Long>> rateLosMap = global.getRateLosMap();
        if (rateLosMap != null) {
            boolean first = true;
            for (Map.Entry<Long, List<Long>> entry : rateLosMap.entrySet()) {
                if (!first) json.append(",");
                first = false;
                json.append("\"").append(entry.getKey()).append("\":[");
                List<Long> ids = entry.getValue();
                for (int i = 0; i < ids.size(); i++) {
                    if (i > 0) json.append(",");
                    json.append(ids.get(i));
                }
                json.append("]");
            }
        }
        json.append("}");

        // rateExtraMap object — { "rateId": [serviceItemId, ...], ... }
        json.append(",\"rateExtraMap\":{");
        Map<Long, List<Integer>> rateExtraMap = global.getRateExtraMap();
        if (rateExtraMap != null) {
            boolean first = true;
            for (Map.Entry<Long, List<Integer>> entry : rateExtraMap.entrySet()) {
                if (!first) json.append(",");
                first = false;
                json.append("\"").append(entry.getKey()).append("\":[");
                List<Integer> ids = entry.getValue();
                for (int i = 0; i < ids.size(); i++) {
                    if (i > 0) json.append(",");
                    json.append(ids.get(i));
                }
                json.append("]");
            }
        }
        json.append("}");

        // enhLosMap object — { "enhId": [losId, ...], ... }
        // Maps each enhancement to its parent LOS IDs for LOS→Enhancement cascade
        json.append(",\"enhLosMap\":{");
        Map<Long, List<Long>> enhLosMap = global.getEnhLosMap();
        if (enhLosMap != null) {
            boolean first = true;
            for (Map.Entry<Long, List<Long>> entry : enhLosMap.entrySet()) {
                if (!first) json.append(",");
                first = false;
                json.append("\"").append(entry.getKey()).append("\":[");
                List<Long> losIds = entry.getValue();
                for (int i = 0; i < losIds.size(); i++) {
                    if (i > 0) json.append(",");
                    json.append(losIds.get(i));
                }
                json.append("]");
            }
        }
        json.append("}");

        json.append("}");
        out.print(json);
        out.flush();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
