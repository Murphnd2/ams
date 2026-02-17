package net.superiorstate.ams.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.model.Activity25;
import net.superiorstate.ams.model.Activity25p;
import net.superiorstate.ams.model.Activity25u;
import net.superiorstate.ams.model.Constant;
import net.superiorstate.ams.controller.authentication.AuthenticateUser;
import net.superiorstate.ams.data.dao.ChecklistDAO;
import net.superiorstate.ams.data.dao.RecurringChecklistDAO;
import net.superiorstate.ams.data.dao.TicketQueryDAO;
import net.superiorstate.ams.data.dao.SequenceDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TaskFrequency;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TemplateGroup;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TemplatePurpose;
import net.superiorstate.ams.previous.model.activity.note.ActivityStatus;
import net.superiorstate.ams.previous.model.activity.note.ReasonCreated;
import net.superiorstate.ams.previous.model.activity.ticket.ContactMethod;
import net.superiorstate.ams.previous.model.activity.ticket.TicketCategory;
import net.superiorstate.ams.previous.model.activity.ticket.TicketSubCategory;
import net.superiorstate.ams.previous.model.activity.ticket.tEmployee;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.WebLink;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

import java.util.ArrayList;
import java.util.List;

public class AmsDataGlobal {


    private List<Person> users;
    private List<Person> bpoUsers;
    private List<TemplateGroup> templateGroups;
    private List<TemplatePurpose> templatePurposes;
    private List<ReasonCreated> reasonsCreated;
    private List<ContactMethod> contactMethods;

    private volatile boolean delegationDirty = false;
    private List<TicketCategory> ticketCategories;
    private List<TicketSubCategory> ticketSubCategories;
    private List<TaskFrequency> taskFrequencies;
    private List<ActivityStatus> activityStatuses;
    private List<WebLink> insertLinks;

    private List<Employer> employers;

    private List<tEmployee> employees;
    private PSP psp;
    private String webPath;
    private String summitPath;
    private String savePath;
    private String falseClose;
    private int daysSinceDanger;
    private int daysSinceWarning;
    private String smtpPassword;
    private String smtpPort;
    private String smtpServer;
    private String smtpUser;
    private String sslPort;
    private List<Activity25u> activitiesAllOpen;

    private List<Activity25p> activitiesWithDelegation;

    public AmsDataGlobal(){};

    public void initializeGlobalData(EntityManager em){
        System.out.println("[DEBUG] initializeGlobalData called");
        setPsp(EntityLookup.getPspById(em,4L));
        setUsers(RecurringChecklistDAO.getPspUserList(em,getPsp()));
        setBpoUsers(AuthenticateUser.getUsersByRole(em,101));
        setTemplateGroups(SequenceDAO.getTemplateGroups(em));
        setTemplatePurposes(SequenceDAO.getTemplatePurposes(em));
        setReasonsCreated(TicketQueryDAO.getReasons(em));
        setContactMethods(TicketQueryDAO.getContactMethods(em));
        setTicketCategories(TicketQueryDAO.getTicketCategories(em));
        setTicketSubCategories(TicketQueryDAO.getTicketSubCategoryList(em));
        setTaskFrequencies(ChecklistDAO.getTaskFrequencies(em));
        setActivityStatuses(TicketQueryDAO.getActivityStatuses(em));
        setInsertLinks(TicketQueryDAO.getInsertLinkList(em));
        setEmployers(generateEmployerList(em));
        setActivitiesAllOpen(retrieveActivitiesAllOpen(em));
        setActivitiesWithDelegation(retrieveActivitiesWithDependencies(em));
        setConstants(em);
        fillEmployeeList(em);

        //setEmployees(dbTicket.getTicketEmployeeList(em));
         /**/
    }

    public void miniUpdate(EntityManager em){

        setTemplatePurposes(SequenceDAO.getTemplatePurposes(em));

        setEmployers(generateEmployerList(em));

        fillEmployeeList(em);
    }

    public List<TicketCategory> getTicketCategories() {
        return ticketCategories;
    }

    private void fillEmployeeList(EntityManager em){
        List<tEmployee> te = TicketQueryDAO.getTicketEmployeeList(em);
        setEmployees(te);
    }

    public List<Activity25p> retrieveActivitiesWithDependencies(EntityManager em){
        Query q = em.createQuery("SELECT a FROM Activity25p a");
        List<Activity25p> activity25ps;
        try{
            activity25ps = (List<Activity25p>) q.getResultList();
        } catch (NoResultException e){return new ArrayList<>();}
        return activity25ps;
    }

    private List<Activity25u> retrieveActivitiesAllOpen(EntityManager em){
        List<Activity25> activity25s;
        Query q = em.createQuery("SELECT a FROM Activity25 a");
        try{
            activity25s = (List<Activity25>) q.getResultList();
        } catch (NoResultException e){
            return new ArrayList<>();
        }
        List<Activity25u> activity25us = new ArrayList<>();
        for(Activity25 a: activity25s)
            activity25us.add(new Activity25u(a));
        return activity25us;
    }

    public void setTicketCategories(List<TicketCategory> ticketCategories) {
        this.ticketCategories = ticketCategories;
    }
    private List<Employer> generateEmployerList(EntityManager em){
        Query q = em.createQuery("SELECT e FROM Employer e WHERE e.isActive=true order by e.employerName");
        List<Employer> employerList;
        try{
            employerList = (List<Employer>) q.getResultList();
        } catch (NoResultException e){
            return  new ArrayList<>();
        }
        return employerList;
    }

    public List<Employer> getEmployers() {
        return employers;
    }

    public void setEmployers(List<Employer> employers) {
        this.employers = employers;
    }

    private void setConstants(EntityManager em){
        String x;

        int daysSinceDanger;
        try{
            x = getConstantValue(em,"DAYS_SINCE_DANGER");
            daysSinceDanger = Integer.parseInt(x);
        } catch (Exception e){daysSinceDanger = 14;}
        setDaysSinceDanger(daysSinceDanger);

        int daysSinceWarning;
        try{
            x = getConstantValue(em,"DAYS_SINCE_WARNING");
            daysSinceWarning = Integer.parseInt(x);
        } catch (Exception e){daysSinceWarning = 7;}
        setDaysSinceWarning(daysSinceWarning);

        String falseClose;
        try{
            falseClose = getConstantValue(em,"FALSE_CLOSE");
        } catch (Exception e){falseClose="2000-01-01";}
        setFalseClose(falseClose);

        String savePath;
        try{
            savePath = getConstantValue(em,"SAVE_PATH");
        } catch (Exception e){savePath = "c:\\data\\";}
        setSavePath(savePath);

        String smtpPassword;
        try{
            smtpPassword = getConstantValue(em,"SMTP_PASSWORD");
        }catch (Exception e){smtpPassword="";}
        setSmtpPassword(smtpPassword);

        String smtpPort;
        try{
            smtpPort = getConstantValue(em,"SMTP_PORT");
        } catch (Exception e){smtpPort = "2525";}
        setSmtpPort(smtpPort);

        String smtpServer;
        try{
            smtpServer = getConstantValue(em,"SMTP_SERVER");
        } catch (Exception e){smtpServer="mail.smtp2go.com";}
        setSmtpServer(smtpServer);

        String smtpUser;
        try{
            smtpUser = getConstantValue(em,"SMTP_USER");
        } catch (Exception e){smtpUser="jspSmtpSender";}
        setSmtpUser(smtpUser);

        String sslPort;
        try{
            sslPort = getConstantValue(em,"SSL_PORT");
        }catch (Exception e){sslPort = "443";}
        setSslPort(sslPort);

        String summitPath;
        try{
            summitPath = getConstantValue(em,"SUMMIT_PATH");
        } catch (Exception e){summitPath = "https://superiorstate.summitwith.us";}
        setSummitPath(summitPath);

        String webPath;
        try{
            webPath = getConstantValue(em,"WEB_PATH");
        } catch (Exception e){webPath = "https://superiorstate.biz/";}
        setWebPath(webPath);
    }

    public String getConstantValue(EntityManager em, String constantName){
        Constant c;
        Query q = em.createQuery("SELECT c FROM Constant c WHERE c.name = :name");
        q.setParameter("name",constantName);
        String value;
        try{
            c = (Constant) q.getSingleResult();
        } catch (NoResultException e){
            return "";
        }
        if(c!=null)
            return c.getValue();
        return "";
    }

    public void setUsers(List<Person> users) {
        this.users = users;
    }

    public void setBpoUsers(List<Person> bpoUsers) {
        this.bpoUsers = bpoUsers;
    }

    public void setTemplateGroups(List<TemplateGroup> templateGroups) {
        this.templateGroups = templateGroups;
    }

    public void setTemplatePurposes(List<TemplatePurpose> templatePurposes) {
        this.templatePurposes = templatePurposes;
    }
    public void markDelegationDirty() {
        this.delegationDirty = true;
    }

    public boolean isDelegationDirty() {
        return this.delegationDirty;
    }

    public void clearDelegationDirty() {
        this.delegationDirty = false;
    }

    public void setReasonsCreated(List<ReasonCreated> reasonsCreated) {
        this.reasonsCreated = reasonsCreated;
    }

    public void setContactMethods(List<ContactMethod> contactMethods) {
        this.contactMethods = contactMethods;
    }

    public void setTicketSubCategories(List<TicketSubCategory> ticketSubCategories) {
        this.ticketSubCategories = ticketSubCategories;
    }

    public void setTaskFrequencies(List<TaskFrequency> taskFrequencies) {
        this.taskFrequencies = taskFrequencies;
    }

    public void setActivityStatuses(List<ActivityStatus> activityStatuses) {
        this.activityStatuses = activityStatuses;
    }

    public void setInsertLinks(List<WebLink> insertLinks) {
        this.insertLinks = insertLinks;
    }

    public void setEmployees(List<tEmployee> employees) {
        this.employees = employees;
    }

    public void setPsp(PSP psp) {
        this.psp = psp;
    }

    public void setWebPath(String webPath) {
        this.webPath = webPath;
    }

    public void setSummitPath(String summitPath) {
        this.summitPath = summitPath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public void setFalseClose(String falseClose) {
        this.falseClose = falseClose;
    }

    public void setDaysSinceDanger(int daysSinceDanger) {
        this.daysSinceDanger = daysSinceDanger;
    }

    public void setDaysSinceWarning(int daysSinceWarning) {
        this.daysSinceWarning = daysSinceWarning;
    }

    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = smtpPassword;
    }

    public void setSmtpPort(String smtpPort) {
        this.smtpPort = smtpPort;
    }

    public void setSmtpServer(String smtpServer) {
        this.smtpServer = smtpServer;
    }

    public void setSmtpUser(String smtpUser) {
        this.smtpUser = smtpUser;
    }

    public void setSslPort(String sslPort) {
        this.sslPort = sslPort;
    }

    public PSP getPsp() {
        return psp;
    }

    public List<Person> getUsers() {
        return users;
    }

    public List<Person> getBpoUsers() {
        return bpoUsers;
    }

    public List<TemplateGroup> getTemplateGroups() {
        return templateGroups;
    }

    public List<TemplatePurpose> getTemplatePurposes() {
        return templatePurposes;
    }

    public List<ReasonCreated> getReasonsCreated() {
        return reasonsCreated;
    }

    public List<ContactMethod> getContactMethods() {
        return contactMethods;
    }

    public List<TicketSubCategory> getTicketSubCategories() {
        return ticketSubCategories;
    }

    public List<TaskFrequency> getTaskFrequencies() {
        return taskFrequencies;
    }

    public List<ActivityStatus> getActivityStatuses() {
        return activityStatuses;
    }

    public List<WebLink> getInsertLinks() {
        return insertLinks;
    }

    public List<tEmployee> getEmployees() {
        return employees;
    }

    public String getWebPath() {
        return webPath;
    }

    public String getSummitPath() {
        return summitPath;
    }

    public String getSavePath() {
        return savePath;
    }

    public String getFalseClose() {
        return falseClose;
    }

    public int getDaysSinceDanger() {
        return daysSinceDanger;
    }

    public int getDaysSinceWarning() {
        return daysSinceWarning;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public String getSmtpPort() {
        return smtpPort;
    }

    public String getSmtpServer() {
        return smtpServer;
    }

    public String getSmtpUser() {
        return smtpUser;
    }

    public String getSslPort() {
        return sslPort;
    }

    public void addUser(Person newUser){
        List<Person> newList = new ArrayList<>(getUsers());
        newList.add(newUser);
        setUsers(newList);
    }

    public List<Activity25u> getActivitiesAllOpen() {
        return activitiesAllOpen;
    }

    public void setActivitiesAllOpen(List<Activity25u> activitiesAllOpen) {
        this.activitiesAllOpen = activitiesAllOpen;
    }

    public List<Activity25p> getActivitiesWithDelegation() {
        return activitiesWithDelegation;
    }

    public void setActivitiesWithDelegation(List<Activity25p> activitiesWithDelegation) {
        this.activitiesWithDelegation = activitiesWithDelegation;
    }
}
