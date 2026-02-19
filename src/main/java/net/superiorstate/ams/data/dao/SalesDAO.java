package net.superiorstate.ams.data.dao;

import jakarta.persistence.*;
import jakarta.servlet.http.HttpServletRequest;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.UserRole;
import net.superiorstate.ams.model.sales.agency.*;
import net.superiorstate.ams.model.sales.offering.LOS;
import net.superiorstate.ams.model.sales.offering.ServiceItem;
import net.superiorstate.ams.model.sales.offering.ServiceModule;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class SalesDAO {

    private static String generatePasswordHash(String passwordToHash, String salt) throws NoSuchAlgorithmException{
        String generatedPassword = null;
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(salt.getBytes());
        byte[] bytes = md.digest(passwordToHash.getBytes());
        StringBuilder sb = new StringBuilder();
        for (int i=0;i < bytes.length; i++){
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        generatedPassword = sb.toString();
        return generatedPassword;
    }
    public static List<Person> getPendingAgents(EntityManager em, PSP psp){
        Query query = em.createQuery("SELECT u FROM User u INNER JOIN FETCH u.userRoleList ur WHERE u.person.psp.id = :psp_id AND ur.id =:role_id");
        query.setParameter("psp_id",psp.getId());
        query.setParameter("role_id",6);
        List<Person> potentialAgentList = (List<Person>) query.getResultList();
        Collections.sort(potentialAgentList);
        return potentialAgentList;
    }

    public static String getJotFormParameterString(EntityManager em, Proposal proposal){
        Query q = em.createQuery("SELECT p FROM Proposal p INNER JOIN FETCH p.losList ll WHERE p.id = :proposal_id");
        q.setParameter("proposal_id",proposal.getId());
        Proposal p = (Proposal) q.getSingleResult();
        String pString = "https://form.jotform.com/210265720255044?";
        pString += "?pop=" + getYesOrNo(em,p,"POP");
        pString += "&fsa=" + getYesOrNo(em,p,"FSA");
        pString += "&hra=" + getYesOrNo(em,p,"HRA/MERP");
        pString += "&cob=" + getYesOrNo(em,p,"COBRA");
        pString += "&hsa=" + getYesOrNo(em,p,"HSA");
        pString += "&tra=" + getYesOrNo(em,p,"TRANSIT");
        pString += "&qid=" + p.getApplicationGUID();
        return pString;
    }

    private static String getYesOrNo(EntityManager em, Proposal proposal, String shortText){
        String result = "N";
        LOS los = getLosByPspAndShortText(em,shortText);
        for(LOS l: proposal.getLosList()){
            if(l==los) {
                result = "Y";
                break;
            }
        }
        return result;
    }

    public static LOS getLosByPspAndShortText(EntityManager em, String shortText){
        Query q = em.createQuery("SELECT l FROM LOS l WHERE l.shortText = :short_text");
        q.setParameter("short_text",shortText);
        return (LOS) q.getSingleResult();
    }
    public static List<Rate> getRateList(EntityManager em, int pspID){
        Query q = em.createQuery("SELECT r FROM Rate r WHERE r.psp.id = :psp_id AND r.isSuppressed = false");
        q.setParameter("psp_id",pspID);
        return (List<Rate>) q.getResultList();
    }

    public static List<Agency> getAgencyList(EntityManager em, int pspID){
        Query q = em.createQuery("SELECT a FROM Agency a WHERE a.psp.id = :psp_id");
        q.setParameter("psp_id",pspID);
        return (List<Agency>) q.getResultList();
    }
    public static List<ServiceModule> getServiceModuleList(EntityManager em, int pspID){
        Query q = em.createQuery("SELECT sm FROM ServiceModule sm WHERE sm.psp.id = :psp_id");
        q.setParameter("psp_id",pspID);
        return (List<ServiceModule>) q.getResultList();
    }
    public static List<PriceItem> getPriceItemList(EntityManager em, int pspID){
        Query q = em.createQuery("SELECT pi FROM PriceItem pi WHERE pi.psp.id = :psp_id");
        q.setParameter("psp_id",pspID);
        return (List<PriceItem>) q.getResultList();
    }
    public static List<ServiceItem> getServiceItemList(EntityManager em, int pspID){
        Query q = em.createQuery("SELECT si FROM ServiceItem si WHERE si.psp.id = :psp_id");
        q.setParameter("psp_id",pspID);
        return (List<ServiceItem>) q.getResultList();
    }

    public static List<RateTable> getRateTableList(EntityManager em, long rateId){
        Query q = em.createQuery("SELECT rt FROM RateTable rt WHERE rt.rate.id = :rate_id ORDER BY rt.module.sortOrder,rt.priceItem.sortOrder");
        q.setParameter("rate_id",rateId);
        return (List<RateTable>) q.getResultList();
    }

    public static List<Agency> getAgenciesAssignedToRate(EntityManager em, long rateId){
        Query q = em.createQuery("SELECT r FROM Rate r INNER JOIN FETCH r.listOfAgenciesWithThisRate a WHERE r.id = :rate_id");
        q.setParameter("rate_id",rateId);
        Rate r = (Rate) q.getSingleResult();
        return r.getListOfAgenciesWithThisRate();
    }

    public static LOS getLosFull(EntityManager em, long losId){
        Query q = em.createQuery("SELECT DISTINCT l FROM LOS l INNER JOIN FETCH l.serviceModuleList sm INNER JOIN FETCH sm.serviceItemList si WHERE l.id = :los_id");
        q.setParameter("los_id",losId);
        return (LOS) q.getSingleResult();
    }

    public static ServiceModule getModuleFull(EntityManager em, long moduleId){
        Query q = em.createQuery("SELECT DISTINCT sm FROM ServiceModule sm INNER JOIN FETCH sm.serviceItemList si WHERE sm.id = :module_id");
        q.setParameter("module_id",moduleId);
        return (ServiceModule) q.getSingleResult();
    }
    public static UserRole getUserRoleById(EntityManager em, int roleId){
        Query q = em.createQuery("SELECT ur FROM UserRole ur WHERE ur.id = :role_id");
        q.setParameter("role_id",roleId);
        return (UserRole) q.getSingleResult();
    }
    public static List<Prospect> getAgencyProspects(EntityManager em, Agency agency){
        Query q = em.createQuery("SELECT a FROM Agency a INNER JOIN FETCH a.agentList ag INNER JOIN FETCH ag.prospectList pl WHERE a.id = :agency_id");
        q.setParameter("agency_id",agency.getId());
        Agency fullAgency = (Agency) q.getSingleResult();
        List<Prospect> prospectList = new ArrayList<>();
        List<Person> agentList = fullAgency.getAgentList();
        for (Person agent:agentList) {
            List<Prospect> agentProspectList = agent.getProspectList();
            for (Prospect prospect: agentProspectList) {
                prospectList.add(prospect);
            }
        }
        return prospectList;
    }
    
    public static List<Prospect> getProspectsByPsp(EntityManager em, int pspId) {
        Query q = em.createQuery(
                "SELECT p FROM Prospect p WHERE p.agent.psp.id = :psp_id ORDER BY p.name");
        q.setParameter("psp_id", (long) pspId);
        return (List<Prospect>) q.getResultList();
    }

    public static Agency getAgencyFull(EntityManager em, long agencyId){
        Query q = em.createQuery("SELECT DISTINCT a FROM Agency a INNER JOIN FETCH a.agencyRateList r WHERE a.id = :agency_id");
        q.setParameter("agency_id",agencyId);
        return (Agency) q.getSingleResult();
    }

    public static List<Person> getAgencyAgents(EntityManager em, long agencyId){
        Query q = em.createQuery("SELECT a FROM Agency a INNER JOIN FETCH a.agentList agent WHERE a.id = :agency_id");
        q.setParameter("agency_id", agencyId);
        Agency agency = (Agency) q.getSingleResult();
        List<Person> agentList = agency.getAgentList();
        Collections.sort(agentList);
        return agentList;
    }


    public static Person getAgentWithAddress(EntityManager em, long id){
        Query q = em.createQuery("SELECT p FROM Person p INNER JOIN FETCH p.address pl WHERE p.id = :id");
        q.setParameter("id",id);
        return (Person) q.getSingleResult();
    }
    public static List<Prospect> getAgentProspects(EntityManager em, Person agent){
        Query q = em.createQuery("SELECT p FROM Prospect p WHERE p.agent.id = :agent_id");
        q.setParameter("agent_id",agent.getId());
        return (List<Prospect>) q.getResultList();
    }



    public static List<Proposal> getProposalListFull(EntityManager em, Prospect prospect){
        Query q = em.createQuery("SELECT p FROM Proposal p INNER JOIN FETCH p.losList los WHERE p.prospect.id = :prospect_id");
        q.setParameter("prospect_id",prospect.getId());
        return (List<Proposal>) q.getResultList();
    }
    public static List<Proposal> getProposalList(EntityManager em, Prospect prospect){
        Query q = em.createQuery("SELECT p FROM Proposal p WHERE p.prospect.id = :prospect_id");
        q.setParameter("prospect_id",prospect.getId());
        return (List<Proposal>) q.getResultList();
    }

    public static List<Rate> getRatesByAgency(EntityManager em, Agency agency){
        Query q = em.createQuery("Select r FROM Rate r INNER JOIN FETCH r.listOfAgenciesWithThisRate a WHERE r.isSuppressed = false AND a.id = :agency_id");
        q.setParameter("agency_id",agency.getId());
        return (List<Rate>) q.getResultList();
    }

    public static ServiceModule getServModByShortText(EntityManager em, String shortText){
        Query q = em.createQuery("SELECT sm FROM ServiceModule sm WHERE sm.shortText = :short_text");
        q.setParameter("short_text",shortText);
        return (ServiceModule) q.getSingleResult();
    }
    public static PriceItem getPriceItemBySortOrder(EntityManager em, int sortOrder){
        Query q = em.createQuery("SELECT pi FROM PriceItem pi WHERE pi.sortOrder = :sort_order");
        q.setParameter("sort_order",sortOrder);
        List<PriceItem> priceItemList = (List<PriceItem>) q.getResultList();
        return priceItemList.get(0);
    }

    public static LOS getLosByShortText(EntityManager em, String shortText){
        Query q = em.createQuery("SELECT l FROM LOS l WHERE l.shortText = :short_text");
        q.setParameter("short_text",shortText);
        return (LOS) q.getSingleResult();
    }

    public static Proposal getProposalByGuid(EntityManager em, String guid){
        Query q = em.createQuery("SELECT p FROM Proposal p WHERE p.applicationGUID = :guid_id");
        q.setParameter("guid_id",guid);
        return (Proposal) q.getSingleResult();
    }


    public static List<RateTable> getPricing(EntityManager em, Proposal p){
        List<ServiceModule> serviceModuleList = getDistinctListOfServiceModulesForThisProposal(p);
        Rate rate = p.getRate();
        List<RateTable> rateTableList = new ArrayList<>();
        for(ServiceModule sm: serviceModuleList){
            Query q = em.createQuery("SELECT rt FROM RateTable rt WHERE rt.rate.id = :rate_id AND rt.module.id = :module_id");
            q.setParameter("rate_id",rate.getId());
            q.setParameter("module_id",sm.getId());
            List<RateTable> tempList = (List<RateTable>) q.getResultList();
            for(RateTable rt: tempList)
                rateTableList.add(rt);
        }
        return rateTableList;
    }

    public static List<ServiceModule> getDistinctListOfServiceModulesForThisProposal(Proposal proposal){
        List<LOS> quotedServices = proposal.getLosList();
        List<ServiceModule> serviceModuleList = new ArrayList<>();
        for(LOS los: quotedServices){
            for(ServiceModule sm: los.getServiceModuleList())
                serviceModuleList.add(sm);
        }
        List<ServiceModule> distinctServiceModuleList = serviceModuleList.stream().distinct().collect(Collectors.toList());
        Collections.sort(distinctServiceModuleList);
        return distinctServiceModuleList;
    }
    public static void setAgencyAccordion(HttpServletRequest request, int panelNumberToShow){
        String collapseIt = "collapsed";
        String unCollapse = "";
        String showIt = "show";
        String pOneA =collapseIt;
        String pTwoA =collapseIt;
        String pThreeA=collapseIt;
        String pFourA=collapseIt;
        String pOneB="";
        String pTwoB="";
        String pThreeB="";
        String pFourB="";
        if(panelNumberToShow ==1){
            pOneA = unCollapse;
            pOneB = showIt;
        } else if (panelNumberToShow ==2) {
            pTwoA = unCollapse;
            pTwoB = showIt;
        } else if (panelNumberToShow == 3){
            pThreeA = unCollapse;
            pThreeB = showIt;
        } else if (panelNumberToShow ==4){
            pFourA = unCollapse;
            pFourB = showIt;
        }
        request.getSession().setAttribute("pOneA",pOneA);
        request.getSession().setAttribute("pOneB",pOneB);
        request.getSession().setAttribute("pTwoA",pTwoA);
        request.getSession().setAttribute("pTwoB",pTwoB);

        request.getSession().setAttribute("pThreeA",pThreeA);
        request.getSession().setAttribute("pThreeB",pThreeB);
        request.getSession().setAttribute("pFourA",pFourA);
        request.getSession().setAttribute("pFourB",pFourB);

    }
}
