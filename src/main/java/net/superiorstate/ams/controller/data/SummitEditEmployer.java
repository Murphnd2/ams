package net.superiorstate.ams.controller.data;

import jakarta.servlet.annotation.WebServlet;

@WebServlet(name = "SummitEditEmployer", value = "/SummitEditEmployer")
public class SummitEditEmployer extends AbstractSummitEmployerRedirect {
    @Override
    protected String buildSummitUrl(String summitPath, String tpaGuid, int employerAltId) {
        return summitPath + "/EmployerModule/EditEmployer.aspx?tpaGuid=" + tpaGuid
                + "&employerId=" + employerAltId;
    }
}
