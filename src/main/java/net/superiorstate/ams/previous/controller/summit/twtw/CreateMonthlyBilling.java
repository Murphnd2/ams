package net.superiorstate.ams.previous.controller.summit.twtw;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.Helper;
import net.superiorstate.ams.previous.data.activity.dActivity;
import net.superiorstate.ams.previous.data.eV;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.data.summit.dH;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.activity.ticket.Ticket;
import net.superiorstate.ams.previous.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.previous.model.billing.BillingGrid;
import net.superiorstate.ams.previous.model.billing.BillingLink;
import net.superiorstate.ams.previous.model.billing.BillingMonth;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Benefit;
import net.superiorstate.ams.previous.model.summit.archive.CoverageStatus;
import net.superiorstate.ams.previous.model.summit.archive.Employee;
import net.superiorstate.ams.previous.model.summit.archive.Employer;
import net.superiorstate.ams.previous.model.summit.imports.order.ImportBenefitCdh;
import net.superiorstate.ams.previous.model.summit.imports.order.ImportEnrollment;
import net.superiorstate.ams.previous.model.summit.temp.Coverage;
import net.superiorstate.ams.previous.model.summit.temp.Enrollment2;

import java.io.IOException;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@WebServlet(name = "CreateMonthlyBilling", value = "/CreateMonthlyBilling")
public class CreateMonthlyBilling extends HttpServlet {

    private BillingMonth billingMonth;

    public BillingMonth getBillingMonth() {
        return billingMonth;
    }

    public void setBillingMonth(BillingMonth billingMonth) {
        this.billingMonth = billingMonth;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        monthlyBillingProcesses(request);
        goToPage(request,response);
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        monthlyBillingProcesses(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/index.jsp");
        dispatcher.forward(request,response);
    }
    private void monthlyBillingProcesses(HttpServletRequest request){
        String flag;
        try{
            flag = request.getParameter("flag");
        } catch (Exception e){
            flag="";
        }
        if(flag==null)
            flag="";
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        if(flag.equals("missing"))
            fillMissingBillingLinks(em);
        else if(flag.equals("pContact"))
            fillPrimaryContacts(em);
        else if(flag.equals("pCleanse"))
            eV.cleanseContactList(em);
        else if(flag.equals("hsa2")){
            BillingMonth bm1 = getBillingMonthByDate(em, Helper.getMonthFor());
            dH.fillHsaBillingGrid(em,bm1);
            fillBillingLinks(em);
            fillDualParticipantGrid(em);
        }
        else if(flag.equals("cobraTest")){
            fillBillingCoverageTableAlt(em,1260);
            logCoverageStatusForThisMonthPB(em,1260);
            fillBillingGrid(em,1260);
        }
        else {
            setBillingFlags(em);
            createBillingMonth(em);
            clearBillingEnrollmentTable(em);
            fillBillingEnrollmentTable(em);
            clearBillingCoverageTable(em);
            fillBillingCoverageTableAlt(em,0);
            logCoverageStatusForThisMonthCDH(em);
            logCoverageStatusForThisMonthPB(em,0);
            fillBillingGrid(em,0);
            BillingMonth bm1 = getBillingMonthByDate(em,Helper.getMonthFor());
            dH.fillHsaBillingGrid(em,bm1);
            fillBillingLinks(em);
            fillDualParticipantGrid(em);
        }
        em.close();
    }
    private void fillPrimaryContacts(EntityManager em){
        Query q = em.createQuery("SELECT a FROM Activity a");
        List<Activity> activityList = (List<Activity>) q.getResultList();
        for(Activity a:activityList){
            if(a.getPrimaryContact()!=null && a.getPrimaryContact().getId()!=null)
                continue;
            String activityType = a.getClass().getSimpleName();
            switch (activityType){
                case "Renewal":
                    Renewal r = (Renewal) a;
                    if(r.getEmployer().getContactList()!=null && r.getEmployer().getContactList().size()>0){
                        Person contact = dActivity.getEmployeePerson(em,r.getEmployer().getContactList().get(0));
                        updateWithThisContact(em,r,contact);
                    } else if(r.getAssigneeContactList()!=null && r.getAssigneeContactList().size()>0){
                        Person contact = r.getAssigneeContactList().get(0);
                        updateWithThisContact(em,r,contact);
                    }
                    break;
                case "Setup":
                    Setup s = (Setup) a;
                    if(s.getContactList()!=null && s.getContactList().size()>0){
                        Person contact = s.getContactList().get(0);
                        updateWithThisContact(em,s,contact);
                    }
                    break;
                case "Ticket":
                    Ticket t = (Ticket) a;
                    if(t.getContact()!=null)
                        updateWithThisContact(em,t,t.getContact());
                    break;
                case "CheckList":
                    CheckList c = (CheckList) a;
                    if(c.getAssignedTo()!=null && c.getAssignedTo().getClass().getSimpleName().equals("Person"))
                        updateWithThisContact(em,c,(Person) c.getAssignedTo());
                default:
                    continue;

            }
        }
    }
    private void updateWithThisContact(EntityManager em, Activity a, Person contact){
        Activity activity = dM.getActivityById(em,a.getId());
        em.getTransaction().begin();
        assert activity != null;
        activity.setPrimaryContact(contact);
        em.persist(contact);
        em.getTransaction().commit();
    }
    private void fillMissingBillingLinks(EntityManager em){
        Query q = em.createQuery("SELECT bg FROM BillingGrid bg LEFT OUTER JOIN BillingLink bl ON (bg.billingMonth = bl.billingMonth AND bg.employer = bl.employer) WHERE (bl.employer is null AND bl.billingMonth is null)");
        List<BillingGrid> billingGridList;
        try{
            billingGridList = (List<BillingGrid>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        if(billingGridList.size()==0)
            return;
        for(BillingGrid bg:billingGridList){
            em.getTransaction().begin();
            BillingLink bl = new BillingLink();
            bl.setEmployer(bg.getEmployer());
            bl.setBillingMonth(bg.getBillingMonth());
            bl.setBillingId(bg.getBillingMonth().getFullDate().toString() + "-" + bg.getEmployer());
            bl.setUniqueId(UUID.randomUUID().toString());
            em.persist(bl);
            em.getTransaction().commit();
        }
    }
    private void fillBillingLinks(EntityManager em){

        //Get All The Billing Months
        List<BillingMonth> billingMonthList;
        Query q = em.createQuery("SELECT bm FROM BillingMonth bm order by bm.year,bm.month");
        try{
            billingMonthList = (List<BillingMonth>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        if(billingMonthList.size()==0)
            return;
        for(BillingMonth bm:billingMonthList){
            List<BillingGrid> billingGridList;
            Query q1 = em.createQuery("SELECT bg FROM BillingGrid bg WHERE bg.billingMonth.monthId = :id");
            q1.setParameter("id",bm.getMonthId());
            try{
                billingGridList = (List<BillingGrid>) q1.getResultList();
            } catch (NoResultException e1){
                continue;
            }
            if(billingGridList.size()==0)
                continue;
            for(BillingGrid bg:billingGridList){
                if(bg.getEmployer()==null)
                    continue;
                String billingLinkId = bm.getFullDate().toString()+"-"+bg.getEmployer().getId();
                Query q2 = em.createQuery("SELECT bl FROM BillingLink bl WHERE bl.billingMonth.monthId = :mId AND bl.employer.id = :eId");
                q2.setParameter("mId",bm.getMonthId());
                q2.setParameter("eId",bg.getEmployer().getId());
                BillingLink bl;
                boolean createOne = false;
                try{
                    bl = (BillingLink) q2.getSingleResult();
                } catch (NoResultException e2){
                    em.getTransaction().begin();
                    BillingLink billingLink = new BillingLink();
                    billingLink.setBillingId(billingLinkId);
                    billingLink.setBillingMonth(bm);
                    billingLink.setEmployer(bg.getEmployer());
                    billingLink.setUniqueId(UUID.randomUUID().toString());
                    em.persist(billingLink);
                    em.getTransaction().commit();
                }
            }
        }
    }
    private void fillDualParticipantGrid(EntityManager em){
        BillingMonth bm = getBillingMonthByDate(em,Helper.getMonthFor());
        Query q = em.createQuery("SELECT bg FROM BillingGrid bg WHERE bg.billingMonth.monthId = :id");
        q.setParameter("id",bm.getMonthId());
        List<BillingGrid> billingGridList;
        try{
            billingGridList = (List<BillingGrid>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        if(billingGridList.size()==0)
            return;
        for(BillingGrid grid: billingGridList){
            if(grid.isFlexSpend() && grid.isHealthReimb())
                setDualPlan(em,grid);
            else if(grid.isFlexSpend() && grid.isHsa())
                setDualPlan(em,grid);
            else if(grid.isHealthReimb() && grid.isHsa())
                setDualPlan(em,grid);
        }
    }
    private void setDualPlan(EntityManager em, BillingGrid grid){
        BillingGrid bg = getBillingGridById(em,grid.getGridId());
        em.getTransaction().begin();
        bg.setDualPlan(true);
        em.persist(bg);
        em.getTransaction().commit();
    }
    private void addCoverageStatusToBillingGrid(EntityManager em, CoverageStatus cs, BillingGrid billingGrid){
        billingGrid.setBillingMonth(getBillingMonthByDate(em, Helper.getMonthFor()));
        billingGrid.setEmployee(cs.getEmployee());
        billingGrid.setEmployer(cs.getEmployer());
        billingGrid.setCurrentStatus("TBD");
        switch (cs.getBillingGroup().getId()){
            case 1:
                billingGrid.setFlexSpend(true);
                if(billingGrid.isHealthReimb())
                    billingGrid.setDualPlan(true);
                break;
            case 2:
                billingGrid.setHealthReimb(true);
                if(billingGrid.isFlexSpend())
                    billingGrid.setDualPlan(true);
                break;
            case 3:
                billingGrid.setCobra(true);
                break;
            case 4:
                billingGrid.setTransit(true);
                break;
            case 5:
                billingGrid.setHsa(true);
                break;
            case 6:
                billingGrid.setLsa(true);
                break;
            default:
                break;
        }
    }
    public BillingGrid getBillingGridById(EntityManager em, String id){
        Query q = em.createQuery("SELECT bg FROM BillingGrid bg WHERE bg.gridId = :id");
        q.setParameter("id",id);
        BillingGrid billingGrid;
        try{
            billingGrid = (BillingGrid) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            billingGrid = new BillingGrid();
            billingGrid.setGridId(id);
        }
        return billingGrid;
    }
    private void closeAnyOpenTransactions(EntityManager em){
        if(em.getTransaction().isActive())
            em.getTransaction().commit();
    }
    private void fillBillingGrid(EntityManager em, int erId){
        Query q = em.createQuery("SELECT cs FROM CoverageStatus cs WHERE cs.monthFor = :month_for AND cs.isActive = true");
        q.setParameter("month_for",Helper.getMonthFor());
        List<CoverageStatus> coverageStatusList;
        try{
            coverageStatusList = (List<CoverageStatus>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        for(CoverageStatus cs: coverageStatusList){
            if(erId!=0 && cs.getEmployer().getId()!=erId)
                continue;
            closeAnyOpenTransactions(em);
            String gridId = cs.getMonthFor() + "-" + cs.getEmployee().getId();
            em.getTransaction().begin();
            BillingGrid billingGrid = getBillingGridById(em,gridId);
            addCoverageStatusToBillingGrid(em,cs,billingGrid);
            em.persist(billingGrid);
            em.getTransaction().commit();
        }
    }
    private void logCoverageStatusForThisMonthPB(EntityManager em, int erId){
        List<Coverage> coverageList;
        Query q = em.createQuery("SELECT c FROM Coverage c WHERE c.isBillable = true");
        try{
            coverageList = (List<Coverage>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        if(coverageList.size()>0){
            for(Coverage coverage:coverageList){
                if(erId!=0 && coverage.getSummitOrganization().getId()!= erId)
                    continue;
                try{
                    em.getTransaction().begin();
                    CoverageStatus c = new CoverageStatus();
                    int benId = coverage.getPbBenefitId();
                    Benefit benefit = dM.getBenefitById(em,benId);
                    Employee employee = dM.getEmployeeById(em, coverage.getSummitEmployee().getId());
                    String id = coverage.getCurrentMonth().toString() + "-" + benefit.getId() + "-"+ employee.getId();
                    c.setId(id);
                    c.setBenefit(benefit);
                    c.setBillingGroup(benefit.getPlanType().getBillingGroup());
                    c.setEmployee(employee);
                    c.setEmployer(employee.getEmployer());
                    c.setMonthFor(coverage.getCurrentMonth());
                    c.setActive(coverage.isBillable());
                    c.setHasCards(false);
                    em.persist(c);
                    em.getTransaction().commit();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
    private void logCoverageStatusForThisMonthCDH(EntityManager em){
        List<Enrollment2> enrollmentList;
        Query q = em.createQuery("SELECT e FROM Enrollment2 e WHERE e.billable = true");
        try {
            enrollmentList = (List<Enrollment2>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        for(int i = 0; i< enrollmentList.size(); i++){
            Enrollment2 enrollment = enrollmentList.get(i);
            try{
                em.getTransaction().begin();
                CoverageStatus c = new CoverageStatus();
                Benefit benefit = dM.getBenefitById(em,enrollment.getImportBenefitCdh().getBenefitId());
                Employee employee = dM.getEmployeeById(em,enrollment.getImportEmployee().getId());
                String id = enrollment.getCurrentMonth().toString() + "-" + benefit.getId() + "-"+ employee.getId();
                c.setId(id);
                c.setBenefit(benefit);
                c.setBillingGroup(benefit.getPlanType().getBillingGroup());
                c.setEmployer(employee.getEmployer());
                c.setEmployee(employee);
                c.setMonthFor(enrollment.getCurrentMonth());
                c.setActive(enrollment.isBillable());
                c.setHasCards(enrollment.isCardEnabled());
                em.persist(c);
                em.getTransaction().commit();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    private void addCoverageStatusAlt(EntityManager em, Benefit b, Employee ee, int covId){


        String status = "Active";
        boolean billable = true;
        if(ee.getCobraStatusId()!=null && ee.getCobraStatusId()>0) {
            status = "COBRA";
            if(ee.getCobraStatusId()==2)
                status="QB";
            else if(ee.getCobraStatusId()==1) {
                status = "TERM";
                billable = false;
            }
        } else if(ee.getSystemStatusId()!=null && ee.getSystemStatusId()==2){
            return;
        } else if(ee.getEeStatusId()!=null && ee.getEeStatusId()>8){
            return;
        } else if(!ee.isActive())
            return;
        em.getTransaction().begin();
        Coverage coverage = new Coverage();
        coverage.setCoverageId(covId);
        coverage.setSummitOrganization(ee.getEmployer());
        coverage.setSummitEmployee(ee);
        coverage.setEmployerId(ee.getEmployer().getAltId());
        coverage.setSummitPlanType(b.getPlanType());
        coverage.setStatus(status);
        coverage.setPbBenefitId(b.getId());
        coverage.setBillable(billable);
        coverage.setCurrentMonth(Helper.getMonthFor());
        coverage.setBenefitName(b.getPlanName());
        coverage.setBillingGroup(dM.getBillingGroupById(em,3));
        coverage.setTierName(b.getPlanType().getPlanTypeName());
        em.persist(coverage);
        em.getTransaction().commit();
    }
    public void fillBillingCoverageTableAlt(EntityManager em, int erId){

        int coverageId = 0;
        if(erId!=0)
            coverageId=99999;
        Query q = em.createQuery("SELECT er FROM Employer er WHERE er.hasPb = true");
        List<Employer> employers;
        try{
            employers = (List<Employer>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        if(employers.size()==0)
            return;
        for(Employer er:employers){
            if(erId !=0)
                er = dM.getEmployerById(em,erId);
            Query qb = em.createQuery("SELECT b FROM Benefit b WHERE b.employer.id = :id AND b.isActive=true AND b.planType.planTypeId>=:lid AND b.planType.planTypeId<=:uid");
            qb.setParameter("id",er.getId());
            qb.setParameter("lid",9);
            qb.setParameter("uid",14);
            List<Benefit> benefitList;
            try{
                benefitList = (List<Benefit>) qb.getResultList();
            } catch (NoResultException e){
                continue;
            }
            if(benefitList.size()>0){
                Query qe = em.createQuery("SELECT ee FROM Employee ee WHERE ee.employer.id = :id AND ee.id >0");
                qe.setParameter("id",er.getId());
                List<Employee> employees;
                try{
                    employees = (List<Employee>) qe.getResultList();
                } catch (NoResultException e2) {
                    continue;
                }
                if(employees.size()==0)
                    continue;
                Benefit b = benefitList.get(0);
                for(Employee ee: employees){
                    coverageId +=1;
                    addCoverageStatusAlt(em,b,ee,coverageId);
                }
            }
            if(erId!=0)
                break;
        }

    }
    public void clearBillingCoverageTable(EntityManager em){
        em.getTransaction().begin();
        Query query = em.createQuery("DELETE FROM Coverage c");
        query.executeUpdate();
        em.getTransaction().commit();
    }
    public void fillBillingEnrollmentTable(EntityManager em){
        List<ImportEnrollment> enrollmentList;
        Query q = em.createQuery("SELECT ie FROM ImportEnrollment ie WHERE ie.planStatus='Active' ORDER BY ie.employerName");
        try{
            enrollmentList = (List<ImportEnrollment>) q.getResultList();
        } catch (NoResultException e){
            enrollmentList = new ArrayList<>();
        }
        if(enrollmentList.size()>0){
            for(ImportEnrollment ie:enrollmentList){
                ImportBenefitCdh importBenefitCdh;
                Query q1 = em.createQuery("SELECT ib FROM ImportBenefitCdh ib WHERE ib.benefitId = :id");
                q1.setParameter("id",Integer.parseInt(ie.getEmployerPlanId()));
                importBenefitCdh = (ImportBenefitCdh) q1.getSingleResult();
                em.getTransaction().begin();
                Enrollment2 enrollment = new Enrollment2();
                enrollment.setEnrollmentId(ie.getEnrollmentId());
                enrollment.setImportEmployer(ie.getImportEmployee().getImportEmployer());
                enrollment.setImportEmployee(ie.getImportEmployee());
                enrollment.setImportBenefitCdh(importBenefitCdh);
                enrollment.setImportBenefitYear(ie.getImportBenefitYear());
                enrollment.setBillingGroup(importBenefitCdh.getPlanType().getBillingGroup());
                enrollment.setCurrentMonth(Helper.getMonthFor());
                enrollment.setTermDate(Date.valueOf(ie.getTermDate()));
                enrollment.setStartDate(Date.valueOf(ie.getImportBenefitYear().getPlanYearStart()));
                enrollment.setEndDate(Date.valueOf(ie.getImportBenefitYear().getPlanYearEnd()));
                enrollment.setBillable(ie.isBillableThisMonth());
                enrollment.setEmployerName(ie.getEmployerName());
                enrollment.setParticipantName(ie.getImportEmployee().getFullNameLastThenFirst());
                enrollment.setBenefitName(importBenefitCdh.getPlanName());
                enrollment.setBenefitGroup(importBenefitCdh.getPlanType().getBillingGroup().getDescription());
                enrollment.setCardEnabled(importBenefitCdh.getCardEnabledBoolean());
                em.persist(enrollment);
                em.getTransaction().commit();
            }
        }
    }
    public void clearBillingEnrollmentTable(EntityManager em){
        em.getTransaction().begin();
        Query q1 = em.createQuery("DELETE FROM Enrollment2 e WHERE e.enrollmentId>0");
        q1.executeUpdate();
        em.getTransaction().commit();
    }
    public void createBillingMonth(EntityManager em){
        LocalDate localDate = LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault());
        LocalDate billingDate = LocalDate.of(localDate.getYear(),localDate.getMonthValue(),1);
        System.out.println("Billing Date: " + billingDate);
        Date date = Date.valueOf(billingDate);
        Query q = em.createQuery("SELECT bm FROM BillingMonth bm");
        List<BillingMonth> billingMonthList;
        try{
            billingMonthList = (List<BillingMonth>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            billingMonthList = new ArrayList<>();
        }
        BillingMonth b = getBillingMonthByDate(em,date);
        setBillingMonth(b);
        if(!billingMonthList.contains(b))
            addBillingMonth(em,date);
    }
    public BillingMonth getBillingMonthByDate(EntityManager em, Date date){
        Query q = em.createQuery("SELECT bm FROM BillingMonth bm WHERE bm.fullDate = :date");
        q.setParameter("date",date);
        BillingMonth billingMonth;
        try{
            billingMonth = (BillingMonth) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            billingMonth = new BillingMonth();
        }
        return  billingMonth;
    }
    private void addBillingMonth(EntityManager em, Date date){
        em.getTransaction().begin();
        BillingMonth billingMonth = new BillingMonth();
        billingMonth.setFullDate(date);
        billingMonth.setYear(date.toLocalDate().getYear());
        billingMonth.setMonth(date.toLocalDate().getMonthValue());
        em.persist(billingMonth);
        em.getTransaction().commit();
    }
    private void setBillingFlags(EntityManager em){
        Query q = em.createQuery("SELECT er FROM Employer er WHERE er.id > 0");
        List<Employer> employerList;
        try{
            employerList = (List<Employer>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        if(employerList.size()==0)
            return;
        for(Employer er: employerList){
            em.getTransaction().begin();
            er.setHasPb(false);
            er.setHasCdh(false);
            er.setHasPop(false);
            em.persist(er);
            em.getTransaction().commit();
            List<Benefit> benefitList = getBenefitList(em,er);
            if(benefitList!=null && benefitList.size()>0){
                em.getTransaction().begin();
                for(Benefit b: benefitList){
                    int pt = b.getPlanType().getPlanTypeId();
                    if(pt==1005||pt==1007)
                        er.setHasPop(true);
                    else if(pt<=8 ||(pt>=1001 && pt<=1004))
                        er.setHasCdh(true);
                    else if(pt<=14 || pt==1010)
                        er.setHasPb(true);
                }
                em.persist(er);
                em.getTransaction().commit();
            }

        }
    }
    private List<Benefit> getBenefitList(EntityManager em, Employer employer){
        Query q = em.createQuery("SELECT b FROM Benefit b WHERE b.employer.id = :eId and b.isActive = true");
        q.setParameter("eId", employer.getId());
        List<Benefit> benefitList;
        try{
            benefitList = (List<Benefit>) q.getResultList();
        } catch (NoResultException e){
            return null;
        }
        return benefitList;
    }
}
