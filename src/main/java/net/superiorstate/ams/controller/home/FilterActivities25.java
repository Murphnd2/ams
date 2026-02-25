package net.superiorstate.ams.controller.home;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.ActivityFilter;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.general.UserFilterPreset;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "FilterActivities25", value = "/FilterActivities25")
public class FilterActivities25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setFilterItems(request);
        goToPage(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setFilterItems(request);
        goToPage(request, response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewHome25");
        dispatcher.forward(request, response);
    }

    private void setFilterItems(HttpServletRequest request) {
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        ActivityFilter af = local.getActivityFilter();

        // ── Preset shortcut: ?preset=1|2|3 ──
        String presetParam = request.getParameter("preset");
        if (presetParam != null) {
            try {
                int slot = Integer.parseInt(presetParam);
                List<UserFilterPreset> presets = local.getFilterPresets();
                if (presets != null) {
                    for (UserFilterPreset p : presets) {
                        if (p.getSlotNumber() == slot) {
                            local.applyPresetToFilter(p);
                            // Preserve opportunity visibility for sales roles
                            boolean isPspSales = Boolean.TRUE.equals(request.getSession().getAttribute("isPspSales"));
                            boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
                            if (isPspSales || isPspAdmin) {
                                af.setViewOpportunity(af.isViewOpportunity());
                            }
                            break;
                        }
                    }
                }
            } catch (NumberFormatException ignored) {}
        } else {
            // ── Manual filter form submission ──
            String rn = request.getParameter("vRenew");
            af.setViewRenewal(rn != null);

            String st = request.getParameter("vSetup");
            af.setViewSetup(st != null);

            String tk = request.getParameter("vTicket");
            af.setViewTicket(tk != null);

            String op = request.getParameter("vOpp");
            af.setViewOpportunity(op != null);

            String attn = request.getParameter("attentionFilter");
            try {
                af.setAttentionFilter(Integer.parseInt(attn));
            } catch (Exception e) {
                af.setAttentionFilter(0);
            }

            String whose = request.getParameter("whoFilter");
            try {
                af.setOwnershipFilter(Integer.parseInt(whose));
            } catch (Exception e) {
                af.setOwnershipFilter(1);
            }

            String fAlpha = request.getParameter("fAlpha");
            af.setSortAlphabetically(fAlpha != null && fAlpha.equals("1"));
        }

        local.setActivityFilter(af);
        local.setFilteredActivityList(local.filterActivityListing());
        request.getSession().setAttribute("local", local);
    }
}
