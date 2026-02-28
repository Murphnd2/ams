package net.superiorstate.ams.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.dao.*;
import net.superiorstate.ams.model.Activity25;
import net.superiorstate.ams.model.Activity25p;
import net.superiorstate.ams.model.Activity25u;
import net.superiorstate.ams.model.Constant;
import net.superiorstate.ams.controller.authentication.AuthenticateUser;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ActivityCategory;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TaskFrequency;
import net.superiorstate.ams.model.activity.note.ActivityStatus;
import net.superiorstate.ams.model.activity.note.ReasonCreated;
import net.superiorstate.ams.model.activity.ticket.ContactMethod;
import net.superiorstate.ams.model.activity.ticket.TicketCategory;
import net.superiorstate.ams.model.activity.ticket.tEmployee;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.UserRole;
import net.superiorstate.ams.model.general.WebLink;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.summit.archive.Employer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AmsDataGlobal {


    private List<Person> users;
    private List<Person> bpoUsers;
    private List<ActivityCategory> activityCategories;
    private List<ServiceItem> serviceItems;
    private List<ReasonCreated> reasonsCreated;
    private List<ContactMethod> contactMethods;

    private volatile boolean delegationDirty = false;
    private List<TicketCategory> ticketCategories;
    private List<ServiceItem> ticketServiceItems;
    private List<TaskFrequency> taskFrequencies;
    private List<ActivityStatus> activityStatuses;
    private List<WebLink> insertLinks;

    private List<Employer> employers;

    private List<tEmployee> employees;

    private EntityManagerFactory emf;
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
    private String logoNavbar;
    private String logoLogin;
    private String favicon;

    private boolean chatbotEnabled;
    private String brandingPath;
    private List<Activity25u> activitiesAllOpen;
    private List<Agency> agencies;
    private List<Activity25p> activitiesWithDelegation;

    public AmsDataGlobal(){};

    public void initializeGlobalData(EntityManager em){
        System.out.println("[DEBUG] initializeGlobalData called");
        try {
            this.emf = em.getEntityManagerFactory();
            setPsp(EntityLookup.getPspById(em,4L));
            setUsers(RecurringChecklistDAO.getPspUserList(em,getPsp()));
            List<Person> allBpo = new ArrayList<>();
            allBpo.addAll(AuthenticateUser.getUsersByRole(em, 102));
            for (Person p : AuthenticateUser.getUsersByRole(em, 103)) {
                if (!allBpo.contains(p)) allBpo.add(p);
            }
            Collections.sort(allBpo);
            setBpoUsers(allBpo);
            setTemplateGroups(SequenceDAO.getTemplateGroups(em));
            setServiceItems(SequenceDAO.getServiceItems(em));
            setReasonsCreated(TicketQueryDAO.getReasons(em));
            setContactMethods(TicketQueryDAO.getContactMethods(em));
            setTicketCategories(TicketQueryDAO.getTicketCategories(em));
            setTicketServiceItems(TicketQueryDAO.getActiveTicketServiceItems(em));
            setTaskFrequencies(ChecklistDAO.getTaskFrequencies(em));
            setActivityStatuses(TicketQueryDAO.getActivityStatuses(em));
            setInsertLinks(TicketQueryDAO.getInsertLinkList(em));
            setEmployers(generateEmployerList(em));
            setActivitiesAllOpen(retrieveActivitiesAllOpen(em));
            setActivitiesWithDelegation(retrieveActivitiesWithDependencies(em));
            setAgencies(SalesDAO.getAgencyList(em, getPsp().getId().intValue()));
            setAssignableRoles(loadAssignableRoles(em));
            setConstants(em);
        } catch (Exception e) {
            System.err.println("❌ initializeGlobalData FAILED: " + e.getMessage());
            e.printStackTrace();
            throw e; // re-throw so EmfListener catches it too
        }



        //setEmployees(dbTicket.getTicketEmployeeList(em));
         /**/
    }

    public void miniUpdate(EntityManager em){

        setServiceItems(SequenceDAO.getServiceItems(em));

        setEmployers(generateEmployerList(em));

    }
    private List<UserRole> assignableRoles;
    public List<TicketCategory> getTicketCategories() {
        return ticketCategories;
    }

    private volatile boolean employeesLoaded = false;

    private void fillEmployeeList(EntityManager em){
        List<tEmployee> te = TicketQueryDAO.getTicketEmployeeListBulk(em);
        setEmployees(te);
        employeesLoaded = true;
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

        setSavePath(AppConfig.get("SAVE_PATH", "/var/lib/tomcat10/data/"));
        setBrandingPath(AppConfig.get("BRANDING_PATH", System.getProperty("catalina.base") + "/branding/"));
        this.chatbotEnabled = "true".equalsIgnoreCase(AppConfig.get("CHATBOT_ENABLED", "false"));

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

        // Logo and favicon paths (PSP-customizable, with generic fallback)
        String lNav;
        try {
            lNav = getConstantValue(em, "LOGO_NAVBAR");
            if (lNav == null || lNav.isBlank()) lNav = "/images/ssa-logo-default.png";
        } catch (Exception e) { lNav = "/images/ssa-logo-default.png"; }
        setLogoNavbar(lNav);

        String lLogin;
        try {
            lLogin = getConstantValue(em, "LOGO_LOGIN");
            if (lLogin == null || lLogin.isBlank()) lLogin = "/images/ssa-logo-login-default.png";
        } catch (Exception e) { lLogin = "/images/ssa-logo-login-default.png"; }
        setLogoLogin(lLogin);

        String fav;
        try {
            fav = getConstantValue(em, "FAVICON");
            if (fav == null || fav.isBlank()) fav = "/images/favicon-default.ico";
        } catch (Exception e) { fav = "/images/favicon-default.ico"; }
        setFavicon(fav);
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

    public void setTemplateGroups(List<ActivityCategory> activityCategories) {
        this.activityCategories = activityCategories;
    }

    public void setServiceItems(List<ServiceItem> serviceItems) {
        this.serviceItems = serviceItems;
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

    public void setTicketServiceItems(List<ServiceItem> ticketServiceItems) {
        this.ticketServiceItems = ticketServiceItems;
    }
    private List<UserRole> loadAssignableRoles(EntityManager em) {
        Query q = em.createQuery(
                "SELECT ur FROM UserRole ur WHERE ur.id NOT IN (2, 3, 4, 8, 9, 102, 103) ORDER BY ur.description");
        try {
            return q.getResultList();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<UserRole> getAssignableRoles() {
        return assignableRoles;
    }

    public void setAssignableRoles(List<UserRole> assignableRoles) {
        this.assignableRoles = assignableRoles;
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

    public String getLogoNavbar() { return logoNavbar; }
    public void setLogoNavbar(String logoNavbar) { this.logoNavbar = logoNavbar; }
    public String getLogoLogin() { return logoLogin; }
    public void setLogoLogin(String logoLogin) { this.logoLogin = logoLogin; }
    public String getFavicon() { return favicon; }
    public void setFavicon(String favicon) { this.favicon = favicon; }
    public String getBrandingPath() { return brandingPath; }
    public void setBrandingPath(String brandingPath) { this.brandingPath = brandingPath; }

    public boolean isChatbotEnabled() { return chatbotEnabled; }
    public PSP getPsp() {
        return psp;
    }

    public List<Person> getUsers() {
        return users;
    }

    public List<Person> getBpoUsers() {
        return bpoUsers;
    }

    public List<ActivityCategory> getTemplateGroups() {
        return activityCategories;
    }

    public List<ServiceItem> getServiceItems() {
        return serviceItems;
    }

    public List<ReasonCreated> getReasonsCreated() {
        return reasonsCreated;
    }

    public List<ContactMethod> getContactMethods() {
        return contactMethods;
    }

    public List<ServiceItem> getTicketServiceItems() {
        return ticketServiceItems;
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
    public List<Agency> getAgencies() {
        return agencies;
    }

    public void setAgencies(List<Agency> agencies) {
        this.agencies = agencies;
    }
    public List<tEmployee> getEmployees() {
        if (!employeesLoaded && emf != null) {
            EntityManager em = emf.createEntityManager();
            try {
                fillEmployeeList(em);
                System.out.println("✅ Employee list lazy-loaded (" + employees.size() + " records)");
            } finally {
                em.close();
            }
        }
        return employees;
    }

    public boolean isEmployeesLoaded() {
        return employeesLoaded;
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
