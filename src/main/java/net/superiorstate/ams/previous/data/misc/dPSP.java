package net.superiorstate.ams.previous.data.misc;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.previous.data.QueryPair;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.previous.model.activity.ticket.Ticket;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.UserRole;
import net.superiorstate.ams.previous.model.sales.agency.*;
import net.superiorstate.ams.previous.model.sales.offering.LOS;
import net.superiorstate.ams.previous.model.sales.offering.ServiceItem;
import net.superiorstate.ams.previous.model.sales.offering.ServiceModule;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

import java.util.ArrayList;
import java.util.List;

public abstract class dPSP {

    public static List<Employer> getPspEmployerList(EntityManager em){
        Query q = em.createQuery("SELECT e FROM Employer e WHERE e.isActive=TRUE order by e.employerName");
        List<Employer> employerList;
        try{
            employerList = (List<Employer>) q.getResultList();
        } catch (NoResultException e){
            return new ArrayList<>();
        }
        return employerList;
    }
    public static List<Person> getPspStaff(PSP psp){
        return getPersonList(psp,1);
    }

    public static List<Person> getPspAgents(PSP psp){
        return getPersonList(psp, 2);
    }

    public static List<Person> getPspAdmins(PSP psp){
        return getPersonList(psp, 3);
    }
    private static List<Person> getPersonList(PSP psp, int roleID){
        UserRole userRoles = dM.getUserRoleById(roleID); //Staff is PSP User Role ID = 1
        List<Person> staffList = new ArrayList<>();
        for(int i = 0; i< userRoles.getUserList().size();i++){
            Person user = userRoles.getUserList().get(i).getPerson();
            if(user.getPsp().getId()==psp.getId())
                staffList.add(user);
        }
        return staffList;
    }

    public static List<RateTable> getRateTable(Rate rate){
        QueryPair qp = new QueryPair("rate_id",rate.getId());
        return (List<RateTable>) dGen.getList("RateTable.getByRate",qp);
    }

    public static List<Setup> getOpenSetups(PSP psp){
        QueryPair qp1 = new QueryPair("psp_id",psp.getId());
        return (List<Setup>) dGen.getList("Setup.getOpenByPsp",qp1);
    }

    public static List<Setup> getAllSetups(PSP psp){
        QueryPair qp1 = new QueryPair("psp_id",psp.getId());
        return (List<Setup>) dGen.getList("Setup.getAllByPsp",qp1);
    }

    public static List<Renewal> getAllRenewals(PSP psp){
        QueryPair qp1 = new QueryPair("psp_id",psp.getId());
        return (List<Renewal>) dGen.getList("Renewal.getAllByPsp",qp1);
    }

    public static List<Renewal> getOpenRenewals(PSP psp){
        QueryPair qp1 = new QueryPair("psp_id",psp.getId());
        return (List<Renewal>) dGen.getList("Renewal.getOpenByPsp",qp1);
    }

    public static List<Ticket> getOpenTickets(PSP psp){
        QueryPair qp1 = new QueryPair("psp_id",psp.getId());
        return (List<Ticket>) dGen.getList("Ticket.getOpenTicketsByPsp",qp1);
    }

    public static List<Ticket> getAllTickets(PSP psp){
        QueryPair qp1 = new QueryPair("psp_id",psp.getId());
        return (List<Ticket>) dGen.getList("Ticket.getAllTicketsByPsp",qp1);
    }

    public static List<ServiceModule> getServiceModules(PSP psp){
        QueryPair qp1 = new QueryPair("psp_id",psp.getId());
        return (List<ServiceModule>) dGen.getList("ServiceModule.getByPsp",qp1);
    }
    public static List<ServiceItem> getServiceItems(PSP psp){
        QueryPair qp1 = new QueryPair("psp_id",psp.getId());
        return (List<ServiceItem>) dGen.getList("ServiceItem.getByPsp",qp1);
    }

    public static List<LOS> getLOS(PSP psp){
        QueryPair qp1 = new QueryPair("psp_id",psp.getId());
        return (List<LOS>) dGen.getList("LOS.getByPsp",qp1);
    }
    public static List<Agency> getAgencies(PSP psp) {
        QueryPair qp1 = new QueryPair("psp_id",psp.getId());
        return (List<Agency>) dGen.getList("Agency.getByPsp",qp1);
    }
    public static List<PriceItem> getPriceItems(PSP psp){
        QueryPair qp1 = new QueryPair("psp_id",psp.getId());
        return (List<PriceItem>) dGen.getList("PriceItem.getByPsp",qp1);
    }

    public static List<Prospect> getProspects(PSP psp){
        QueryPair qp1 = new QueryPair("psp_id",psp.getId());
        return (List<Prospect>) dGen.getList("Prospect.getByPsp",qp1);
    }

    public static List<Rate> getRates(PSP psp) {
        QueryPair qp1 = new QueryPair("psp_id", psp.getId());
        return (List<Rate>) dGen.getList("Rate.getByPsp", qp1);
    }

}
