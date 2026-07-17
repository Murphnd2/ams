package net.superiorstate.ams.controller.data;

import jakarta.servlet.annotation.WebServlet;

@WebServlet(name = "SummitBenefitOrder", value = "/SummitBenefitOrder")
public class SummitBenefitOrder extends AbstractSummitEmployerRedirect {
    @Override
    protected String buildSummitUrl(String summitPath, String tpaGuid, int employerAltId) {
        return summitPath + "/Area/BenefitOrder/EmployerLevel?employerId=" + employerAltId;
    }
}
