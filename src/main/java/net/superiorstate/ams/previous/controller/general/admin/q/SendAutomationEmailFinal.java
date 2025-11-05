package net.superiorstate.ams.previous.controller.general.admin.q;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.controller.general.admin.ViewSelectedActivity;
import net.superiorstate.ams.previous.data.SessionVar;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.note.ActivityStatus;
import net.superiorstate.ams.previous.model.activity.note.Email;
import net.superiorstate.ams.previous.model.activity.ticket.Ticket;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.WebLink;

import jakarta.mail.MessagingException;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "SendAutomationEmailFinal", value = "/SendAutomationEmailFinal")
public class SendAutomationEmailFinal extends HttpServlet {
    private final String whereTo = "GoAdminHome";
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            sendAutoEmail(request,response);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            sendAutoEmail(request,response);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher(whereTo);
        dispatcher.forward(request,response);
    }

    private void createTaskAndWebLink(HttpServletRequest request,EntityManager em, Activity a, Person user, String docName, String docPath){
        //Get Checklist for Activity
        Query q1 = em.createQuery("SELECT c FROM CheckList c WHERE c.assignedTo.id = :id");
        q1.setParameter("id",a.getId());
        CheckList c = (CheckList) q1.getSingleResult();

        //Create WebLink
        em.getTransaction().begin();
        WebLink w = new WebLink();
        w.setActive(false);
        w.setLinkPath(docPath);
        w.setPlainText(docName);
        w.setLinkType(dM.getLinkTypeById(em,2));
        em.persist(w);
        em.getTransaction().commit();

        //Create Task
        em.getTransaction().begin();
        Task t = new Task();
        t.setDescription("Link To: " + docName);
        t.setPsp(dM.getPspById(em,4L));
        t.setReUsable(false);
        t.setHasAutomation(false);
        em.persist(t);
        em.getTransaction().commit();

        //Assign WebLink
        Query q2 = em.createQuery("SELECT t FROM Task t WHERE t.id = :id");
        q2.setParameter("id", t.getId());
        Task task = (Task) q2.getSingleResult();
        em.getTransaction().begin();
        task.addWebLink(w);
        em.persist(task);
        em.getTransaction().commit();

        //Get ToDo List
        Query q3 = em.createQuery("SELECT t FROM ToDo t WHERE t.checkList.id = :id order by t.sortOrder desc ");
        q3.setParameter("id",c.getId());
        List<ToDo> toDoList = (List<ToDo>) q3.getResultList();
        int maxSort = toDoList.get(0).getSortOrder();

        //Create ToDo From Task
        em.getTransaction().begin();
        ToDo toDo = new ToDo();
        toDo.setTask(task);
        toDo.setSortOrder(maxSort+10);
        toDo.setCheckList(c);
        em.persist(toDo);
        em.getTransaction().commit();

        em.getTransaction().begin();
        c.getToDoList().add(toDo);
        em.persist(c);
        em.getTransaction().commit();
    }
    private void sendAutoEmail(HttpServletRequest request, HttpServletResponse response) throws MessagingException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        SessionVar sVar = (SessionVar) request.getSession().getAttribute("sVar");
        Activity a = (sVar != null && sVar.getCurrentActivity() != null)
                ? sVar.getCurrentActivity()
                : (Activity) request.getSession().getAttribute("currentActivity");

        Person user = (sVar != null && sVar.getCurrentPerson() != null)
                ? sVar.getCurrentPerson()
                : (Person) request.getSession().getAttribute("currentPerson");

        int isWebLink = Integer.parseInt(String.valueOf(request.getSession().getAttribute("webLinkTask")));

        if (isWebLink == 1) {
            String docName = request.getParameter("aInput-0");
            String docPath = request.getParameter("aInput-1");
            createTaskAndWebLink(request, em, a, user, docName, docPath);
        } else {
            setCurrentActivity(a);

            // Build subject/body from your template pipeline
            List<String> subjectAndBody = prepareMessageContent(request, em);
            String subject = subjectAndBody.get(0);
            if (subject == null || subject.isBlank()) {
                subject = String.valueOf(request.getSession().getAttribute("automationTitle"));
            }
            String messageHtml = subjectAndBody.get(1);

            // Persist Email entity (unchanged)
            ActivityStatus as = dM.getActivityStatusById(em, 1);
            Email email = StdAuto.createEmail(request, em, a, subject, messageHtml, as, user);

            // Re-load with relationships
            Email email1 = dM.getEmailById(em, email.getId());

            // Build final HTML (add “attachments” WebLinks section)
            String htmlBody = buildHtmlBody(email1, em);

            // Collect TO recipients from Email entity
            List<String> toWhoList = new ArrayList<>();
            if (email1.getRecipientList() != null) {
                for (Person r : email1.getRecipientList()) {
                    if (r != null && r.getEmail() != null && !r.getEmail().isBlank()) {
                        toWhoList.add(r.getEmail().trim());
                    }
                }
            }

            // CC from session if <cc> was used on the form
            List<String> ccList = collectCcList(request);

            // FromWho = human (Reply-To). SMTP “From/Envelope” comes from dbEmail (SMTP_FROM).
            String fromWho = (email1.getCreatedBy() != null) ? String.valueOf(email1.getCreatedBy().getEmail()) : "";

            try {
                System.out.println("[AutoEmail] about to send via SMTP2GO");
                dbEmail.sendEmail(fromWho, toWhoList, ccList, java.util.Collections.emptyList(),
                        subject, htmlBody, em);
                System.out.println("[AutoEmail] sent OK");

            } catch (MessagingException mex) {
                System.out.println("[SendAutomationEmailFinal] SMTP2GO send FAILED: " + mex.getMessage());
                mex.printStackTrace();
                throw mex; // keep your existing error handling / navigation
            }

            // Close task if <<close>> was set
            String shouldClose = String.valueOf(request.getSession().getAttribute("shouldClose"));
            processShouldClose(em, a, shouldClose, user);
        }

        ViewSelectedActivity.setActivityView(request, em, a);
        em.close();
    }
    private List<String> collectCcList(HttpServletRequest request) {
        Object useCc = request.getSession().getAttribute("useCcList");
        boolean wantCc = (useCc != null && Integer.parseInt(String.valueOf(useCc)) == 1);
        if (!wantCc) return java.util.Collections.emptyList();

        String ccCsv = (String) request.getSession().getAttribute("ccList");
        if (ccCsv == null || ccCsv.isBlank()) return java.util.Collections.emptyList();

        List<String> cc = new ArrayList<>();
        for (String s : ccCsv.split("[;,]")) {
            String addr = s.trim();
            if (!addr.isEmpty() && dbEmail.isValidEmail(addr)) cc.add(addr);
        }
        return cc;
    }

    private String buildHtmlBody(Email email, EntityManager em) {
        String html = (email.getDetail() != null) ? email.getDetail() : "";
        List<WebLink> links = email.getWebLinkList();
        if (links != null && !links.isEmpty()) {
            StringBuilder atts = new StringBuilder("<p><b><u>Attachments</u></b><br/><ul>");
            for (WebLink l : links) {
                atts.append("<li>").append(l.getExternalAnchorTag(em)).append("</li>");
            }
            atts.append("</ul></p>");
            html += atts;
        }
        return html;
    }


    private void processShouldClose(EntityManager em, Activity a, String shouldClose, Person currentPerson){
        int indexOfDash = shouldClose.indexOf("-");
        int shouldCloseInt = Integer.parseInt(shouldClose.substring(indexOfDash+1));
        if(shouldCloseInt!=1)
            return;

        //Get CheckList of the Activity
        Query q1 = em.createQuery("SELECT c FROM CheckList c WHERE c.assignedTo.id = :id");
        q1.setParameter("id",a.getId());
        CheckList c;
        try{
            c = (CheckList) q1.getSingleResult();
        } catch (Exception e){
            return;
        }

        //Does CheckList Have Todo with Task That is Open
        long taskId = Long.parseLong(shouldClose.substring(0,indexOfDash));
        Task task = dM.getTaskById(em,taskId);
        List<ToDo> toDoList;
        Query q2 = em.createQuery("SELECT t FROM ToDo t WHERE t.checkList.id = :id");
        q2.setParameter("id",c.getId());
        try{
            toDoList = (List<ToDo>) q2.getResultList();
        } catch (Exception e1){
            return;
        }
        long toDoId = -1L;
        for(ToDo t: toDoList){
            if(t.getTask().equals(task))
                toDoId = t.getId();
        }
        if(toDoId==-1L)
            return;
        em.getTransaction().begin();
        ToDo toDo = dM.getToDoById(em,toDoId);
        assert toDo != null;
        toDo.setComplete(true);
        toDo.setDateCompleted(Date.valueOf(LocalDate.now()));
        toDo.setCompletedBy(currentPerson);
        em.persist(toDo);
        em.getTransaction().commit();
    }

    public Activity getCurrentActivity() {
        return currentActivity;
    }

    public void setCurrentActivity(Activity currentActivity) {
        this.currentActivity = currentActivity;
    }

    private Activity currentActivity;




    private List<String> prepareMessageContent(HttpServletRequest request, EntityManager em){
        String processedText;
        String remainingText = (String) request.getSession().getAttribute("aContent");

        String numInputs = getNumberOfInputs(request);
        System.out.println("LABELED INPUTS: " + numInputs);
        int numInp = Integer.parseInt(numInputs);
        System.out.println("INTEGER INPUTS: " + numInp);

        if(numInp>0){
            processedText = processText(request, remainingText);
        } else{
            processedText = remainingText;
        }
        String standardText = standardizeText(processedText,em);

        String formattedText = formatText(standardText);

        String linksAddedText = addLinks(formattedText,em);

        String breaksAddedText = linksAddedText.replace("<nl>","<br/>");

        return getSubjectAndBody(breaksAddedText);
    }

    private String standardizeText(String text,EntityManager em){
        String fText = text;
        String holder = text;
        //Replace #erName
        if(fText.contains("<<#erName>>")){
            String erName;
            if(getCurrentActivity().getClass().getSimpleName().equals("Ticket")){
                try{
                    Ticket t = (Ticket) getCurrentActivity();
                    erName = t.getContact().getEmployee().getEmployer().getEmployerName();
                    if(erName==null || erName.equals(""))
                        erName = t.getFullName();
                } catch (Exception e){
                    erName = getCurrentActivity().getFullName();
                }
            } else
                erName = getCurrentActivity().getFullName();
            holder = fText.replace("<<#erName>>",erName);
            fText = holder;
        }

        //Replace #activityType
        if(fText.contains("<<#activityType>>")){
            holder = fText.replace("<<#activityType>>", getCurrentActivity().getClass().getSimpleName());
            fText = holder;
        }

        return fText;
    }

    private String addLinks(String formattedText, EntityManager em){
        String unprocessedText = formattedText;
        System.out.println("RAW-------------------------------------------------");
        System.out.println(unprocessedText);
        StringBuilder processedText  = new StringBuilder();
        int counter = 0;
        while(unprocessedText.contains("<rf>") && unprocessedText.contains("</rf>")){
            System.out.println("PASS " + counter + " ----------------------------------------");
            String holder = unprocessedText;
            int locStart = unprocessedText.indexOf("<rf>");
            int locEnd = unprocessedText.indexOf("</rf>");
            processedText.append(unprocessedText, 0, locStart);
            System.out.println(processedText);
            String idString = unprocessedText.substring(locStart+4,locEnd);
            int id = Integer.parseInt(idString);
            String linkString = getLinkString(em, id);
            processedText.append(linkString);
            System.out.println(processedText);
            unprocessedText = holder.substring(locEnd+5);
            System.out.println("UNPROCESSED ----------------------");
            System.out.println(unprocessedText);

        }
        processedText.append(unprocessedText);
        return processedText.toString();
    }

    private String getLinkString(EntityManager em, int refId){
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
            String update = updateString(linkPath,getCurrentActivity());
            linkPath = update;
        }
        return "<a target=\"_blank\" href=\"" + linkPath + "\">"+linkName+"</a>";
    }

    private String updateString(String text, Activity a){
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

    private List<String> getSubjectAndBody(String text){
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
    private String formatText(String textToFormat){
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
        return formattedText.toString();
    }
    private String processText(HttpServletRequest request, String remainingText){
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

    private String getInputCode(int j){
        return "<[{" + j + "}]>";
    }
    private String getNumberOfInputs(HttpServletRequest request){
        return request.getSession().getAttribute("aNumInputs").toString();
    }

    private String getParameterString(HttpServletRequest request,int i){
        List<String> inputTypes =(List<String>) request.getSession().getAttribute("aInputTypes");
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

    private String wrapInput(String input){
        return " <a target=\"_blank\" href=\"" + input + "\">" + input + "</a> ";
    }
}
