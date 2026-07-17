package net.superiorstate.ams.controller.data;

import jakarta.servlet.annotation.WebServlet;

@WebServlet(name = "SummitParticipants", value = "/SummitParticipants")
public class SummitParticipants extends AbstractSummitEmployerRedirect {
    @Override
    protected String buildSummitUrl(String summitPath, String tpaGuid, int employerAltId) {
        return summitPath + "/Area/Participant/ParticipantList?employerId=" + employerAltId;
    }
}
