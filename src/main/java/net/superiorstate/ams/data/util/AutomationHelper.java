package net.superiorstate.ams.data.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.EmailDAO;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.Opportunity;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.renewal.Renewal;
import net.superiorstate.ams.model.activity.ticket.Ticket;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.WebLink;
import net.superiorstate.ams.model.summit.archive.Employer;

import java.util.ArrayList;
import java.util.List;

public abstract class AutomationHelper {


    public static List<Person> getRecipientList(EntityManager em, AmsDataLocal local, String ccList){
        List<Person> rList = new ArrayList<>();
        List<String> emailList = new ArrayList<>();
        Activity a = local.getCurrentActivity().getActivity();
        if(local.getCurrentActivity().getPrimaryContact()!=null && local.getCurrentActivity().getPrimaryContact().getEmail()!=null && Validator.isValidEmail(local.getCurrentActivity().getPrimaryContact().getEmail())){
            rList.add(local.getCurrentActivity().getPrimaryContact());
            emailList.add(local.getCurrentActivity().getPrimaryContact().getEmail());
        }

        if(a.getPrimaryContact()!=null && a.getPrimaryContact().getEmail()!=null && Validator.isValidEmail(a.getPrimaryContact().getEmail())){
            rList.add(a.getPrimaryContact());
            emailList.add(a.getPrimaryContact().getEmail().trim().toLowerCase());
        }

        if(local.getCurrentActivity().getAdditionalContacts()!=null && local.getCurrentActivity().getAdditionalContacts().size()>0){
            for(Person p: local.getCurrentActivity().getAdditionalContacts()){
                if(p.getEmail()!=null && Validator.isValidEmail(p.getEmail()) && !emailList.contains(p.getEmail().trim().toLowerCase())) {
                    rList.add(p);
                    emailList.add(p.getEmail().trim().toLowerCase());
                }
            }
        }
        if(ccList!=null && !ccList.equals("")){
            String remainingText = ccList;
            String emailFound;
            while (remainingText.contains(";")){
                int scl = remainingText.indexOf(";");
                emailFound = remainingText.substring(0,scl);
                processLists(emailList,rList,emailFound,local, em);
                if(remainingText.length()<= scl+2)
                    remainingText = "";
                else remainingText = remainingText.substring(scl+1);
            }
            processLists(emailList,rList,remainingText,local,em);
        }
        return rList;
    }

    private static void processLists(List<String> emailList, List<Person> rList, String emailFound, AmsDataLocal local, EntityManager em){
        if(Validator.isValidEmail(emailFound)){
            if(!emailList.contains(emailFound.trim().toLowerCase())){
                Person p2 = EmailDAO.getPersonByEmail(em,emailFound,local.getCurrentPerson().getPsp());
                if(p2==null){
                    em.getTransaction().begin();
                    p2 = new Person();
                    p2.setEmail(emailFound);
                    p2.setFirstName("NEW");
                    p2.setLastName("PERSON");
                    p2.setPsp(local.getCurrentPerson().getPsp());
                    em.persist(p2);
                    em.getTransaction().commit();
                }
                rList.add(p2);
                emailList.add(emailFound);
            }
        }
    }

    public static List<String> getSubjectAndBody(String text){
        List<String> returnList = new ArrayList<>();
        String subject;
        String message;
        if(text.contains("<sbj>") && text.contains("</sbj>")){
            subject = text.substring(text.indexOf("<sbj>")+5,text.indexOf("</sbj>"));
            message = text.substring(0,text.indexOf("<sbj>"))+ text.substring(text.indexOf("</sbj>")+6);
        } else{
            subject = "";
            message = text;
        }
        returnList.add(subject);
        returnList.add(message);
        return returnList;
    }

    public static String processReferenceLinks(String formattedText,Activity a, EntityManager em) {
        String unprocessedText = formattedText;
        StringBuilder processedText = new StringBuilder();
        while (unprocessedText.contains("<rf>") && unprocessedText.contains("</rf>")) {
            String holder = unprocessedText;
            int locStart = unprocessedText.indexOf("<rf>");
            int locEnd = unprocessedText.indexOf("</rf>");
            processedText.append(unprocessedText, 0, locStart);
            String idString = unprocessedText.substring(locStart + 4, locEnd);
            int id = Integer.parseInt(idString);
            String linkString = getLinkString(em, id,a);
            processedText.append(linkString);
            unprocessedText = holder.substring(locEnd + 5);
        }
        processedText.append(unprocessedText);
        return processedText.toString();
    }
    private static String getLinkString(EntityManager em, int refId, Activity a){
        Query q = em.createQuery("SELECT w FROM WebLink w WHERE w.id = :id");
        q.setParameter("id",refId);
        WebLink webLink;
        try{
            webLink = (WebLink) q.getSingleResult();
        } catch (NoResultException e){
            webLink = null;
        }
        if(webLink == null)
            return "";
        String linkName = webLink.getPlainText();
        String linkPath = webLink.getLinkPath();
        if(linkName.contains("TEST:") || linkName.contains("JFORM:")) {
            String update = updateString(linkPath,a);
            linkPath = update;
        }
        return "<a target=\"_blank\" href=\"" + linkPath + "\">"+linkName+"</a>";
    }
    private static String updateString(String text, Activity a){
        String theFinal = text + "?testId=" + a.getId().toString();
        if(a.getClass().getSimpleName().equals("Ticket")){
            Ticket t = (Ticket) a;
            String erName;
            try{
                erName = t.getContact().getEmployee().getEmployer().getEmployerName().replace(" ","_");
                if(erName==null || erName.equals(""))
                    erName = a.getFullName().replace(" ","_");
            } catch (Exception e){
                erName = a.getFullName().replace(" ","_");
            }
            theFinal += "&ername=" + erName;
        } else {
            theFinal += "&ername=" + a.getFullName().replace(" ","_");
        }
        return theFinal;
    }
    public static String processBreaksAndNewLines(String textToFormat){
        String unformattedText = textToFormat;
        StringBuilder formattedText = new StringBuilder("<p>");
        while (unformattedText.contains("<br/>")) {
            int breakLoc = unformattedText.indexOf("<br/>");
            formattedText.append(unformattedText, 0, breakLoc);
            formattedText.append("</p><p>");
            String holdText = unformattedText;
            unformattedText = holdText.substring(breakLoc+5);
        }
        formattedText.append(unformattedText);
        formattedText.append("</p>");
        return formattedText.toString().replace("<nl>","<br/>");
    }
    public static Employer getEmployerForActivity(EntityManager em, Activity a){
        if(a.getClass().getSimpleName().equals("Renewal")){
            Renewal r = (Renewal) a;
            if(r.getEmployer()!=null)
                return r.getEmployer();
            return null;
        } else if (a.getClass().getSimpleName().equals("Ticket")){
            Ticket t = (Ticket) a;
            if(t.getPrimaryContact()!=null && t.getPrimaryContact().getEmployee()!=null && t.getPrimaryContact().getEmployee().getEmployer()!=null)
                return t.getPrimaryContact().getEmployee().getEmployer();
            return null;
        }
        return null;
    }
    /**
     * Resolves the employer/prospect name for any activity type.
     * Returns the name string if found, or null if resolution fails at any point.
     *
     * Resolution chain by type:
     *   Renewal     → employer.getEmployerName()
     *   Ticket      → contact.getEmployee().getEmployer().getEmployerName()
     *   Setup       → application.getProposal().getProspect().getName()
     *   Opportunity → prospect.getName()
     *   CheckList   → delegates to parent activity (renewal/ticket/setup)
     */
    public static String resolveErName(EntityManager em, Activity a) {
        try {
            if (a instanceof Renewal r) {
                if (r.getEmployer() != null)
                    return r.getEmployer().getEmployerName();
            } else if (a instanceof Ticket t) {
                if (t.getPrimaryContact() != null
                        && t.getPrimaryContact().getEmployee() != null
                        && t.getPrimaryContact().getEmployee().getEmployer() != null)
                    return t.getPrimaryContact().getEmployee().getEmployer().getEmployerName();
            } else if (a instanceof Setup s) {
                if (s.getApplication() != null
                        && s.getApplication().getProposal() != null
                        && s.getApplication().getProposal().getProspect() != null)
                    return s.getApplication().getProposal().getProspect().getName();
            } else if (a instanceof Opportunity o) {
                if (o.getProspect() != null)
                    return o.getProspect().getName();
            } else if (a instanceof CheckList cl) {
                // Delegate to the parent activity
                if (cl.getRenewal() != null) return resolveErName(em, cl.getRenewal());
                if (cl.getSetup() != null) return resolveErName(em, cl.getSetup());
                if (cl.getTicket() != null) return resolveErName(em, cl.getTicket());
            }
        } catch (Exception e) {
            // Any unexpected null or lazy-load failure — fall through to null
        }
        return null;
    }

    public static String processInputs(HttpServletRequest request, String remainingText){
        StringBuilder processedText = new StringBuilder();
        int j = 0;
        String iCode = getInputCode(j);
        while(remainingText.contains(iCode)){
            int count = j+1;
            System.out.println("********************* S T E P   " + count + " *********************************************");
            int codeLoc = remainingText.indexOf(iCode);

            processedText.append(remainingText, 0, codeLoc);
            System.out.println("1: " + processedText);

            String pString = getParameterString(request,j);
            System.out.println("2: " + pString);

            processedText.append(pString);
            System.out.println("3: " + processedText);

            String holdText = remainingText;
            int codeLength = getInputCode(j).length();
            remainingText = holdText.substring(codeLoc+codeLength);
            System.out.println("REMAINING TEXT ---------------------");
            System.out.println(remainingText);
            j+=1;
            iCode = getInputCode(j);
        }
        processedText.append(remainingText);
        return processedText.toString();
    }
    private static String getInputCode(int j){
        return "<[{" + j + "}]>";
    }
    private static String getParameterString(HttpServletRequest request,int i){
        List<String> inputTypes =(List<String>) request.getSession().getAttribute("a1inputTypes");
        boolean isLink = inputTypes.get(i).equals("LINK");
        boolean isCc = inputTypes.get(i).equals("CC");
        String parameterName = "aInput-" + i;
        String userInput = request.getParameter(parameterName);
        if(isLink)
            return wrapInput(userInput);
        else if(isCc) {
            request.getSession().setAttribute("ccList",userInput);
            return "";
        }

        return " " + userInput + " ";
    }
    private static String wrapInput(String input){
        return " <a target=\"_blank\" href=\"" + input + "\">" + input + "</a> ";
    }
}
