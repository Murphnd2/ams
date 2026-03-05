package net.superiorstate.ams.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.dao.*;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.Activity25;
import net.superiorstate.ams.model.Activity25p;
import net.superiorstate.ams.model.Activity25u;
import net.superiorstate.ams.model.Constant;
import net.superiorstate.ams.controller.authentication.AuthenticateUser;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ActivityCategory;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TaskFrequency;
import net.superiorstate.ams.model.activity.note.ActivityStatus;
import net.superiorstate.ams.model.activity.note.ReasonCreated;
import net.superiorstate.ams.model.activity.ticket.ContactMethod;
import net.superiorstate.ams.model.activity.ticket.TicketCategory;
import net.superiorstate.ams.model.activity.ticket.tEmployee;
import net.superiorstate.ams.model.general.BpoRegistration;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.UserRole;
import net.superiorstate.ams.model.general.WebLink;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.sales.agency.Prospect;
import net.superiorstate.ams.model.sales.agency.Rate;
import net.superiorstate.ams.model.sales.offering.Enhancement;
import net.superiorstate.ams.model.sales.offering.LOS;
import net.superiorstate.ams.model.summit.archive.Employer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class AmsDataGlobal {

    /** Lightweight agent info for the Setup modal agent dropdown. */
    public static class AgentInfo {
        private final long id;
        private final String name;
        private final String agencyIds;

        public AgentInfo(long id, String name, String agencyIds) {
            this.id = id;
            this.name = name;
            this.agencyIds = agencyIds;
        }

        public long getId() { return id; }
        public String getName() { return name; }
        public String getAgencyIds() { return agencyIds; }
    }


    private List<Person> users;
    private List<Person> bpoUsers;
    private List<Person> opportunityManagers;
    private List<BpoRegistration> activeBpoRegistrations;
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
    private boolean useTimeclock = true;
    private String schemaVersion = "Unknown";
    private String brandingPath;
    private List<Activity25u> activitiesAllOpen;
    private List<Agency> agencies;
    private List<Prospect> prospects;
    private List<Activity25p> activitiesWithDelegation;
    private List<LOS> losList;
    private List<Enhancement> enhancementList;
    private List<Rate> rateList;
    private Map<Long, List<Long>> rateLosMap;
    private Map<Long, List<Integer>> rateExtraMap;
    private Map<Long, List<Long>> agencyRateMap;
    private List<AgentInfo> setupAgents;
    private Map<Long, String> prospectAgencyMap;
    private Map<Long, Long> agencyManagerMap;
    private Long pspHomeAgencyId;

    public AmsDataGlobal(){};

    public void initializeGlobalData(EntityManager em){
        String systemType = AppConfig.getSystemType();
        System.out.println("🔧 System type: " + systemType);
        System.out.println("[DEBUG] initializeGlobalData called");
        try {
            this.emf = em.getEntityManagerFactory();
            setPsp(EntityLookup.getPspById(em,4L));
            setUsers(RecurringChecklistDAO.getPspUserList(em,getPsp()));
            setOpportunityManagers(loadOpportunityManagers(em));
            List<Person> allBpo = new ArrayList<>();
            allBpo.addAll(AuthenticateUser.getUsersByRole(em, 102));
            for (Person p : AuthenticateUser.getUsersByRole(em, 103)) {
                if (!allBpo.contains(p)) allBpo.add(p);
            }
            Collections.sort(allBpo);
            setBpoUsers(allBpo);
            setActiveBpoRegistrations(loadActiveBpoRegistrations(em));
            setAssignableRoles(loadAssignableRoles(em));
            setConstants(em);

            // Cache authoritative system type from DB constant into AppConfig
            String dbSystemType = getConstantValue(em, "SYSTEM_TYPE");
            if (dbSystemType != null && !dbSystemType.isBlank()) {
                AppConfig.setSystemType(dbSystemType);
                System.out.println("🔧 System type cached from DB: " + dbSystemType);
            }

            if (AppConfig.isPsp()) {
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
                setProspects(SalesDAO.getProspectsByPsp(em, getPsp().getId().intValue()));
                setLosList(loadLosList(em));
                setEnhancementList(loadEnhancementList(em));
                setRateList(SalesDAO.getRateList(em, getPsp().getId().intValue()));
                setRateLosMap(SalesDAO.getRateLosMap(em));
                setRateExtraMap(SalesDAO.getRateExtraMap(em));
                setAgencyRateMap(SalesDAO.getAgencyRateMap(em));
                setSetupAgents(buildAgentList(em));
                setProspectAgencyMap(buildProspectAgencyMap(em));
                setAgencyManagerMap(SalesDAO.getAgencyManagerMap(em));
            }
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

        try {
            String utc = getConstantValue(em, "USE_TIMECLOCK");
            this.useTimeclock = !"false".equalsIgnoreCase(utc);
        } catch (Exception e) { this.useTimeclock = true; }

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
        // If the DB value points to /branding/ but the file doesn't exist locally, use default
        String lNav;
        try {
            lNav = getConstantValue(em, "LOGO_NAVBAR");
            if (lNav == null || lNav.isBlank() || !brandingFileExists(lNav)) lNav = "/images/logoA.png";
        } catch (Exception e) { lNav = "/images/logoA.png"; }
        setLogoNavbar(lNav);

        String lLogin;
        try {
            lLogin = getConstantValue(em, "LOGO_LOGIN");
            if (lLogin == null || lLogin.isBlank() || !brandingFileExists(lLogin)) lLogin = "/images/logoA.png";
        } catch (Exception e) { lLogin = "/images/logoA.png"; }
        setLogoLogin(lLogin);

        String fav;
        try {
            fav = getConstantValue(em, "FAVICON");
            if (fav == null || fav.isBlank() || !brandingFileExists(fav)) fav = "/favicon.ico";
        } catch (Exception e) { fav = "/favicon.ico"; }
        setFavicon(fav);

        // PSP Home Agency ID
        String homeAgencyIdStr = getConstantValue(em, "PSP_HOME_AGENCY_ID");
        if (homeAgencyIdStr != null && !homeAgencyIdStr.isEmpty()) {
            this.pspHomeAgencyId = Long.parseLong(homeAgencyIdStr);
        }

        // Schema version (latest applied migration)
        try {
            Object result = em.createNativeQuery(
                "SELECT script_name FROM schema_version ORDER BY applied_on DESC LIMIT 1"
            ).getSingleResult();
            if (result != null) schemaVersion = result.toString();
        } catch (Exception e) {
            schemaVersion = "Unknown";
        }
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

    /** Reset the lazy-load flag so the next getEmployees() call reloads from DB. */
    public void resetEmployeeCache() {
        this.employeesLoaded = false;
        this.employees = null;
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

    /** Returns true if the path is NOT a /branding/ path, or if the branding file exists on disk. */
    private boolean brandingFileExists(String path) {
        if (path == null || !path.startsWith("/branding/")) return true;
        String filename = path.substring("/branding/".length());
        if (filename.contains("?")) filename = filename.substring(0, filename.indexOf("?"));
        try {
            return java.nio.file.Files.exists(java.nio.file.Paths.get(brandingPath, filename));
        } catch (Exception e) { return false; }
    }

    public boolean isChatbotEnabled() { return chatbotEnabled; }
    public boolean isUseTimeclock() { return useTimeclock; }
    public String getSchemaVersion() { return schemaVersion; }
    public PSP getPsp() {
        return psp;
    }

    public List<Person> getUsers() {
        return users;
    }

    public List<Person> getBpoUsers() {
        return bpoUsers;
    }

    public List<Person> getOpportunityManagers() { return opportunityManagers; }
    public void setOpportunityManagers(List<Person> opportunityManagers) { this.opportunityManagers = opportunityManagers; }

    /** Load persons with role 5 (PSP Admin) or 9 (PSP Sales), deduplicated and sorted */
    private List<Person> loadOpportunityManagers(EntityManager em) {
        List<Person> managers = new ArrayList<>(AuthenticateUser.getUsersByRole(em, 5));
        for (Person p : AuthenticateUser.getUsersByRole(em, 9)) {
            if (!managers.contains(p)) managers.add(p);
        }
        Collections.sort(managers);
        return managers;
    }

    public List<BpoRegistration> getActiveBpoRegistrations() {
        return activeBpoRegistrations;
    }

    public void setActiveBpoRegistrations(List<BpoRegistration> activeBpoRegistrations) {
        this.activeBpoRegistrations = activeBpoRegistrations;
    }

    private List<BpoRegistration> loadActiveBpoRegistrations(EntityManager em) {
        try {
            return em.createQuery(
                    "SELECT b FROM BpoRegistration b WHERE b.isActive = true AND b.isApproved = true " +
                    "AND b.isRequested = true AND b.isAccepted = true ORDER BY b.bpoName", BpoRegistration.class)
                    .getResultList();
        } catch (Exception e) {
            return new ArrayList<>();
        }
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
    public List<Prospect> getProspects() {
        return prospects;
    }
    public void setProspects(List<Prospect> prospects) {
        this.prospects = prospects;
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

    public boolean isContactTrackingDisabled() {
        return daysSinceWarning >= 99;
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

    /** Reload all user-related caches after activation/deactivation/role changes. */
    public void refreshUserCaches(EntityManager em) {
        setUsers(RecurringChecklistDAO.getPspUserList(em, getPsp()));
        setOpportunityManagers(loadOpportunityManagers(em));
        List<Person> allBpo = new ArrayList<>();
        allBpo.addAll(AuthenticateUser.getUsersByRole(em, 102));
        for (Person p : AuthenticateUser.getUsersByRole(em, 103)) {
            if (!allBpo.contains(p)) allBpo.add(p);
        }
        Collections.sort(allBpo);
        setBpoUsers(allBpo);
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

    private List<LOS> loadLosList(EntityManager em) {
        try {
            List<LOS> list = em.createNamedQuery("LOS.getByPsp", LOS.class)
                    .setParameter("psp_id", getPsp().getId())
                    .getResultList();
            list.removeIf(LOS::isSuppressed);
            return list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<LOS> getLosList() {
        return losList;
    }

    public void setLosList(List<LOS> losList) {
        this.losList = losList;
    }

    private List<Enhancement> loadEnhancementList(EntityManager em) {
        try {
            return em.createQuery(
                    "SELECT e FROM Enhancement e WHERE e.psp.id = :pspId " +
                    "AND e.suppressed = false AND e.serviceItem IS NOT NULL " +
                    "ORDER BY e.sortOrder", Enhancement.class)
                    .setParameter("pspId", getPsp().getId())
                    .getResultList();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<Enhancement> getEnhancementList() {
        return enhancementList;
    }

    public void setEnhancementList(List<Enhancement> enhancementList) {
        this.enhancementList = enhancementList;
    }

    public List<Rate> getRateList() {
        return rateList;
    }

    public void setRateList(List<Rate> rateList) {
        this.rateList = rateList;
    }

    public Map<Long, List<Long>> getRateLosMap() {
        return rateLosMap;
    }

    public void setRateLosMap(Map<Long, List<Long>> rateLosMap) {
        this.rateLosMap = rateLosMap;
    }

    public Map<Long, List<Integer>> getRateExtraMap() {
        return rateExtraMap;
    }

    public void setRateExtraMap(Map<Long, List<Integer>> rateExtraMap) {
        this.rateExtraMap = rateExtraMap;
    }

    public Map<Long, List<Long>> getAgencyRateMap() {
        return agencyRateMap;
    }

    public void setAgencyRateMap(Map<Long, List<Long>> agencyRateMap) {
        this.agencyRateMap = agencyRateMap;
    }

    /** Comma-separated rate IDs assigned to the given agency (for JSP data attributes). */
    public String getAgencyRateIds(Long agencyId) {
        if (agencyRateMap == null || !agencyRateMap.containsKey(agencyId)) return "";
        StringBuilder sb = new StringBuilder();
        for (Long id : agencyRateMap.get(agencyId)) {
            if (sb.length() > 0) sb.append(",");
            sb.append(id);
        }
        return sb.toString();
    }

    /** Reload rate/LOS/enhancement/agency/agent caches after sales data changes. */
    public void refreshSalesData(EntityManager em) {
        setLosList(loadLosList(em));
        setEnhancementList(loadEnhancementList(em));
        setRateList(SalesDAO.getRateList(em, getPsp().getId().intValue()));
        setRateLosMap(SalesDAO.getRateLosMap(em));
        setRateExtraMap(SalesDAO.getRateExtraMap(em));
        setAgencyRateMap(SalesDAO.getAgencyRateMap(em));
        setAgencies(SalesDAO.getAgencyList(em, getPsp().getId().intValue()));
        setProspects(SalesDAO.getProspectsByPsp(em, getPsp().getId().intValue()));
        setSetupAgents(buildAgentList(em));
        setProspectAgencyMap(buildProspectAgencyMap(em));
        setAgencyManagerMap(SalesDAO.getAgencyManagerMap(em));
    }

    /** Comma-separated LOS IDs available in the given rate (for JSP data attributes). */
    public String getRateLosIds(Long rateId) {
        if (rateLosMap == null || !rateLosMap.containsKey(rateId)) return "";
        StringBuilder sb = new StringBuilder();
        for (Long id : rateLosMap.get(rateId)) {
            if (sb.length() > 0) sb.append(",");
            sb.append(id);
        }
        return sb.toString();
    }

    /** Comma-separated Enhancement ServiceItem IDs available in the given rate (for JSP data attributes). */
    public String getRateExtraIds(Long rateId) {
        if (rateExtraMap == null || !rateExtraMap.containsKey(rateId)) return "";
        StringBuilder sb = new StringBuilder();
        for (Integer id : rateExtraMap.get(rateId)) {
            if (sb.length() > 0) sb.append(",");
            sb.append(id);
        }
        return sb.toString();
    }

    // ═══ Agent / Prospect → Agency mappings for Setup modal ═══

    /** Build unique agent list with their agency memberships from DB data. */
    private List<AgentInfo> buildAgentList(EntityManager em) {
        try {
            List<Object[]> rows = SalesDAO.getAgencyAgentData(em);
            Map<Long, String> nameMap = new LinkedHashMap<>();
            Map<Long, List<Long>> agMap = new HashMap<>();
            for (Object[] row : rows) {
                Long agencyId = (Long) row[0];
                Long personId = (Long) row[1];
                String name = (String) row[2];
                nameMap.put(personId, name);
                agMap.computeIfAbsent(personId, k -> new ArrayList<>()).add(agencyId);
            }
            List<AgentInfo> list = new ArrayList<>();
            for (Map.Entry<Long, String> entry : nameMap.entrySet()) {
                Long personId = entry.getKey();
                String name = entry.getValue();
                StringBuilder sb = new StringBuilder();
                for (Long aid : agMap.get(personId)) {
                    if (sb.length() > 0) sb.append(",");
                    sb.append(aid);
                }
                list.add(new AgentInfo(personId, name, sb.toString()));
            }
            return list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /** Build prospect → comma-separated agency IDs map (through prospect.agent's agency memberships). */
    private Map<Long, String> buildProspectAgencyMap(EntityManager em) {
        Map<Long, String> result = new HashMap<>();
        try {
            List<Object[]> rows = SalesDAO.getProspectAgencyData(em);
            Map<Long, List<Long>> temp = new HashMap<>();
            for (Object[] row : rows) {
                Long prospectId = (Long) row[0];
                Long agencyId = (Long) row[1];
                temp.computeIfAbsent(prospectId, k -> new ArrayList<>()).add(agencyId);
            }
            for (Map.Entry<Long, List<Long>> entry : temp.entrySet()) {
                StringBuilder sb = new StringBuilder();
                for (Long aid : entry.getValue()) {
                    if (sb.length() > 0) sb.append(",");
                    sb.append(aid);
                }
                result.put(entry.getKey(), sb.toString());
            }
        } catch (Exception e) {
            // return empty map
        }
        return result;
    }

    public List<AgentInfo> getSetupAgents() { return setupAgents; }
    public void setSetupAgents(List<AgentInfo> setupAgents) { this.setupAgents = setupAgents; }

    public Map<Long, String> getProspectAgencyMap() { return prospectAgencyMap; }
    public void setProspectAgencyMap(Map<Long, String> prospectAgencyMap) { this.prospectAgencyMap = prospectAgencyMap; }

    /** Comma-separated agency IDs for the given prospect (for JSP data attributes). */
    public String getProspectAgencyIds(Long prospectId) {
        if (prospectAgencyMap == null || !prospectAgencyMap.containsKey(prospectId)) return "";
        return prospectAgencyMap.get(prospectId);
    }

    /** True if the given agency has at least one agent. */
    public boolean hasAgents(Long agencyId) {
        if (setupAgents == null) return false;
        for (AgentInfo ai : setupAgents) {
            for (String id : ai.getAgencyIds().split(",")) {
                if (id.equals(String.valueOf(agencyId))) return true;
            }
        }
        return false;
    }

    public Long getPspHomeAgencyId() { return pspHomeAgencyId; }

    public Map<Long, Long> getAgencyManagerMap() { return agencyManagerMap; }
    public void setAgencyManagerMap(Map<Long, Long> agencyManagerMap) { this.agencyManagerMap = agencyManagerMap; }

    /** Returns the agency manager (role 8) personId for the given agency, or 0 if none. */
    public long getAgencyManagerId(Long agencyId) {
        if (agencyManagerMap == null || !agencyManagerMap.containsKey(agencyId)) return 0;
        return agencyManagerMap.get(agencyId);
    }

    /** Returns comma-separated agency IDs that the given person belongs to as an agent. */
    public String getPersonAgencyIds(Long personId) {
        if (setupAgents == null) return "";
        for (AgentInfo ai : setupAgents) {
            if (ai.getId() == personId) return ai.getAgencyIds();
        }
        return "";
    }
}
