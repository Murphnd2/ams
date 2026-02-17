package net.superiorstate.ams.data.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.data.Helper;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.previous.model.summit.archive.Benefit;
import net.superiorstate.ams.previous.model.summit.archive.BenefitTier;
import net.superiorstate.ams.previous.model.summit.archive.Employee;
import net.superiorstate.ams.previous.model.summit.archive.Employer;
import net.superiorstate.ams.previous.model.summit.imports.*;

import java.sql.Date;
import java.util.List;

public abstract class SummitSync {

    public static void createBenefitTierTablesPB(EntityManager em){
        Query q = em.createQuery("SELECT sbt FROM sBenefitTierPB sbt");
        List<sBenefitTierPB> benefitTierPBList;
        try{
            benefitTierPBList = (List<sBenefitTierPB>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        if(benefitTierPBList.size()==0)
            return;
        for(sBenefitTierPB sbt:benefitTierPBList)
            whenNoPbTierAdd(em,sbt);
    }
    private static String getId(sBenefitTierPB bt){
        return -bt.getBenefitId()+"-"+bt.getPlanYearId()+"-"+bt.getTierId();
    }
    private static void whenNoPbTierAdd(EntityManager em, sBenefitTierPB sbt){
        Query q = em.createQuery("SELECT bt FROM BenefitTier bt WHERE bt.id = :id");
        q.setParameter("id",getId(sbt));
        BenefitTier bt;
        try{
            bt = (BenefitTier) q.getSingleResult();
        } catch (NoResultException e) {
            bt = null;
        }
        if(bt!=null)
            return;
        em.getTransaction().begin();
        BenefitTier benefitTier = new BenefitTier();
        benefitTier.setId(getId(sbt));
        benefitTier.setBenefit(getBenefit(em,sbt));
        benefitTier.setTierName(sbt.getTierId());
        benefitTier.setTierDescription(sbt.getTierName());
        benefitTier.setStartDate(sbt.getPlanStartDate());
        benefitTier.setEndDate(sbt.getPlanEndDate());
        em.persist(benefitTier);
        em.getTransaction().commit();
    }
    public static void createNewBenefitsPB(EntityManager em){
        Query q = em.createQuery("SELECT sbt FROM sBenefitTierPB sbt LEFT OUTER JOIN Benefit b ON sbt.benefitId = b.pbBenId WHERE b.pbBenId is null");
        List<sBenefitTierPB> sBenefitTierPBList;
        try{
            sBenefitTierPBList= (List<sBenefitTierPB>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        if (sBenefitTierPBList.size()==0)
            return;
        for(sBenefitTierPB sbt: sBenefitTierPBList){
            Benefit b = getBenefit(em,sbt);
            if(b==null){
                em.getTransaction().begin();
                Benefit benefit = new Benefit();
                benefit.setId(-1*sbt.getBenefitId());
                benefit.setPbBenId(sbt.getBenefitId());
                benefit.setPlanType(sbt.getsPlanType());
                benefit.setEmployer(getEmployer(em,sbt.getsEmployer().getOrganizationId()));
                benefit.setPlanName(sbt.getPlanName());
                benefit.setPlanDescription(sbt.getPlanDescription());
                benefit.setActive(true);
                benefit.setHasCards(false);
                benefit.setEffectiveDate(sbt.getPlanEffectiveDate());
                benefit.setNextRenewalDue(Date.valueOf(sbt.getPlanEffectiveDate().toLocalDate().plusYears(1L)));
                benefit.setTerminationDate(sbt.getPlanEndDate());
                em.persist(benefit);
                em.getTransaction().commit();
            }
        }
    }
    private static Benefit getBenefit(EntityManager em, sBenefitTierPB sbt){
        Query q = em.createQuery("SELECT b FROM Benefit b WHERE b.pbBenId = :id");
        q.setParameter("id",sbt.getBenefitId());
        Benefit b;
        try{
            b = (Benefit) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return b;
    }
    public static void createNewBenefitsCDH(EntityManager em){
        Query q = em.createQuery("SELECT sb FROM sBenefit sb LEFT OUTER JOIN Benefit b ON sb.benefitId=b.id WHERE b.id is null");
        List<sBenefit> sBenefitList;
        try{
            sBenefitList = (List<sBenefit>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        if(sBenefitList.size()==0)
            return;
        for(sBenefit sb: sBenefitList) {
            Date effDate = sb.getPlanEffectiveDate();
            Date renDate = Date.valueOf(effDate.toLocalDate().plusYears(1L));
            em.getTransaction().begin();
            Benefit b = new Benefit();
            b.setId(sb.getBenefitId());
            em.persist(b);
            b.setPlanType(sb.getSummitPlanType());
            b.setEmployer(getEmployer(em, sb));
            b.setPlanName(sb.getPlanName());
            b.setPlanDescription(sb.getPlanDescription());
            b.setActive(sb.getPlanStatus().trim().equals("Active"));
            b.setHasCards(sb.getCardEnabledBoolean());
            b.setEffectiveDate(sb.getPlanEffectiveDate());
            b.setNextRenewalDue(renDate);
            b.setTerminationDate(sb.getPlanTerminationDate());
            em.persist(b);
            em.getTransaction().commit();
        }
    }
    public static void createNewEssEmployers(EntityManager em){
        //Find Employers in current Summit Export (sEmployer2) table, that aren't in main import table (sEmployer)
        Query q = em.createQuery("SELECT se2 FROM sEmployer2 se2 LEFT OUTER JOIN sEmployer se ON se2.organizationId=se.organizationId WHERE se.organizationId is null");
        List<sEmployer2> sEmployer2List;
        try{
            sEmployer2List = (List<sEmployer2>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        if(sEmployer2List.size()==0)
            return;
        for(sEmployer2 se2: sEmployer2List){
            em.getTransaction().begin();
            sEmployer essEmployer = new sEmployer();
            essEmployer.setStatus(se2.getStatus());
            essEmployer.setEmployerId(se2.getEmployerId());
            essEmployer.setEmployerName(se2.getEmployerName());
            essEmployer.setOrganizationId(se2.getOrganizationId());
            essEmployer.setDpiSuiteErKey(se2.getDpiSuiteErKey());
            essEmployer.setIsSetUpCompleted(se2.getIsSetUpCompleted());
            essEmployer.setCreatedByUser1(se2.getCreatedByUser1());
            essEmployer.setCreatedDate(se2.getCreatedDate());
            essEmployer.setAgency(se2.getAgency());
            essEmployer.setCreatedByUser(se2.getCreatedByUser());
            essEmployer.setImplementationLeadUserId(se2.getImplementationLeadUserId());
            essEmployer.setImplementationLead(se2.getImplementationLead());
            essEmployer.setAgencyOrganizationId(se2.getAgencyOrganizationId());
            essEmployer.setPhone(se2.getPhone());
            essEmployer.setPhoneNumber(se2.getPhoneNumber());
            essEmployer.setEmail(se2.getEmail());
            essEmployer.setTaxId(se2.getTaxId());
            essEmployer.setSetup(se2.getSetup());
            essEmployer.setSetUpComplete(se2.getSetUpComplete());
            essEmployer.setSetUpCompletionDate(se2.getSetUpCompletionDate());
            essEmployer.setPrimaryContact(se2.getPrimaryContact());
            em.persist(essEmployer);
            em.getTransaction().commit();
        }
    }
    public static void updateEssEmployerActiveStatus(EntityManager em){
        Query q = em.createQuery("SELECT s FROM sEmployer s INNER JOIN sEmployer2 s2 ON s.organizationId=s2.organizationId WHERE s.status <> s2.status");
        List<sEmployer> sEmployerList;
        try{
            sEmployerList = (List<sEmployer>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        if(sEmployerList.size()==0)
            return;
        for(sEmployer se:sEmployerList){
            sEmployer2 s2 = getsEmployer2ById(em,se.getOrganizationId());
            em.getTransaction().begin();
            se.setStatus(s2.getStatus());
            em.persist(se);
            em.getTransaction().commit();
        }
    }
    private static sEmployer2 getsEmployer2ById(EntityManager em, int id){
        Query q = em.createQuery("SELECT s FROM sEmployer2 s WHERE s.organizationId = :id");
        q.setParameter("id",id);
        sEmployer2 s2;
        try{
            s2 = (sEmployer2) q.getSingleResult();
        } catch (NoResultException e){
            return new sEmployer2();
        }
        return s2;
    }
    public static void createNewEmployers(EntityManager em){
        Query q = em.createQuery("SELECT se FROM sEmployer se LEFT OUTER JOIN Employer er ON se.organizationId = er.id WHERE er.id is null and se.status<>:stat");
        q.setParameter("stat","Inactive");
        List<sEmployer> sEmployerList;
        try{
            sEmployerList = (List<sEmployer>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        if(sEmployerList.size()==0)
            return;
        for(sEmployer se:sEmployerList){
            em.getTransaction().begin();
            Employer e = new Employer();
            e.setEmployerName(se.getEmployerName());
            e.setActive(true);
            e.setBillable(true);
            e.setAgency(false);
            e.setId(se.getOrganizationId());
            try{
                e.setErKey(Integer.parseInt(se.getDpiSuiteErKey()));
            } catch (Exception ex){
                e.setErKey(se.getOrganizationId()*-1);
            }
            e.setAltId(se.getEmployerId());
            em.persist(e);
            em.getTransaction().commit();
        }
    }
    public static void updateEmployerActiveStatus(EntityManager em){
        Query q = em.createQuery("SELECT s FROM Employer s INNER JOIN sEmployer s2 ON s.id=s2.organizationId WHERE (s.isActive=true AND s2.status=:inActive) OR (s.isActive=false AND s2.status<>:inActive)");
        List<Employer> employerList;
        q.setParameter("inActive","Inactive");
        try{
            employerList = (List<Employer>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        if(employerList.size()==0)
            return;
        for(Employer e:employerList){
            sEmployer se = getsEmployerById(em,e.getId());
            boolean active = true;
            if(se.getStatus().equals("Inactive"))
                active = false;
            em.getTransaction().begin();
            e.setActive(active);
            em.persist(e);
            em.getTransaction().commit();
        }
    }
    private static sEmployer getsEmployerById(EntityManager em, int id){
        Query q = em.createQuery("SELECT s FROM sEmployer s WHERE s.organizationId = :id");
        q.setParameter("id",id);
        sEmployer s2;
        try{
            s2 = (sEmployer) q.getSingleResult();
        } catch (NoResultException e){
            return new sEmployer();
        }
        return s2;
    }
    private static sEmployee getEssEmployee(EntityManager em, sEmployee2 se2){
        Query q = em.createQuery("SELECT se FROM sEmployee se WHERE se.id = :id");
        q.setParameter("id",se2.getParticipantId());
        sEmployee se;
        try{
            se = (sEmployee) q.getSingleResult();
        } catch (NoResultException e){
            return new sEmployee();
        }
        return se;
    }
    private static int getMmKey(EntityManager em, sEmployee2 se2){
        Query q = em.createQuery("SELECT s FROM sEmployee s WHERE s.id = :id");
        q.setParameter("id",se2.getParticipantId());
        sEmployee se;
        try{
            se = (sEmployee) q.getSingleResult();
        } catch (NoResultException e){
            return -1*se2.getParticipantId();
        }
        if(se.getDpiSuiteMmKey()!=null){
            int theInt;
            try{
                theInt = Integer.parseInt(se.getDpiSuiteMmKey());
            } catch (Exception e){
                theInt = 0;
            }
            return theInt;
        }
        return -1*se2.getParticipantId();
    }
    public static void createNewEmployees2(EntityManager em){
        Query q = em.createQuery("SELECT se2 FROM sEmployee2 se2 LEFT OUTER JOIN Employee ee ON se2.participantId = ee.id WHERE ee.id is null");
        List<sEmployee2> sEmployee2List;
        try{
            sEmployee2List = (List<sEmployee2>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        if(sEmployee2List.size()==0)
            return;
        for(sEmployee2 se:sEmployee2List){
            Employer er = getEmployer(em,se.getOrganizationId());
            if(er == null)
                continue;
            sEmployee altSe = getEssEmployee(em,se);
            em.getTransaction().begin();
            Employee ee = new Employee();
            ee.setId(se.getParticipantId());
            ee.setMmKey(getMmKey(em,se));
            ee.setCustomId(altSe.getDpiSuiteMmKey());
            ee.setLastName(se.getParticipantLast().toUpperCase().trim());
            ee.setFirstName(se.getParticipantFirst().toUpperCase().trim());
            ee.setCity(altSe.getCity());
            ee.setState(altSe.getState());
            ee.setZipCode(altSe.getZipCode());
            ee.setEmail(altSe.getEmail());
            ee.setEeStatusId(se.getEmploymentStatusId());
            ee.setSystemStatusId(se.getUserStatusId());
            ee.setAddress1(altSe.getAddress1());
            ee.setAddress2(altSe.getAddress2());
            em.persist(ee);
            em.getTransaction().commit();
        }
    }
    public static Employer getEmployer(EntityManager em, int id){
        Query q = em.createQuery("SELECT e FROM Employer e WHERE e.id = :id");
        q.setParameter("id",id);
        Employer employer;
        try{
            employer = (Employer) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return employer;
    }
    private static Employer getEmployer(EntityManager em, sBenefit sb){
        return getEmployer(em,sb.getSummitEmployer().getOrganizationId());
    }
    public static void makeEmployeesInactive2(EntityManager em){
        Query q = em.createQuery("SELECT e FROM Employee e INNER JOIN sEmployee2 se ON e.id=se.participantId order by e.id");
        List<Employee> employees;
        try{
            employees = (List<Employee>) q.getResultList();
        } catch (NoResultException e){
            return;
        }

        if(employees.size()==0)
            return;
        for(Employee ee:employees){
            em.getTransaction().begin();
            try{
                Query query = em.createQuery("SELECT s FROM sEmployee2 s WHERE s.participantId = :id");
                query.setParameter("id",ee.getId());
                sEmployee2 s = (sEmployee2) query.getSingleResult();
                ee.setSystemStatusId(s.getUserStatusId());
                ee.setEeStatusId(s.getParticipantStatusId());
                em.persist(ee);
            } catch (Exception e){
                e.printStackTrace();
            } finally {
                em.getTransaction().commit();
            }
        }
    }
    public static void updateEmployeeNames2(EntityManager em){
        Query q = em.createQuery("SELECT ee FROM Employee ee INNER JOIN sEmployee se ON ee.id = se.id WHERE ee.lastName<>se.lastName OR ee.firstName<>se.lastName OR ee.email <> se.email");
        List<Employee> employees;
        try{
            employees = (List<Employee>) q.getResultList();
        } catch (NoResultException e1){
            return;
        }
        if(employees.size()==0)
            return;
        for(Employee e:employees){
            Employee ee = EntityLookup.getEmployeeById(em,e.getId());
            sEmployee se = EntityLookup.getSummitEmployeeById(em,ee.getId());
            em.getTransaction().begin();
            ee.setFirstName(se.getFirstName());
            ee.setLastName(se.getLastName());
            ee.setEmail(se.getEmail());
            em.persist(ee);
            em.getTransaction().commit();
        }
    }
    public static void reactivateBenefits(EntityManager em){
        Query q = em.createQuery("SELECT b FROM Benefit b INNER JOIN sBenefit sb ON sb.benefitId=b.id " +
                "WHERE b.isActive=false AND sb.planStatus<>'Inactive'");
        List<Benefit> benefitList;
        try{
            benefitList = (List<Benefit>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        if(benefitList.size()==0)
            return;
        for(Benefit b: benefitList){
            em.getTransaction().begin();
            b.setActive(true);
            em.persist(b);
            em.getTransaction().commit();
        }
    }
    public static void closeInactiveBenefits(EntityManager em){
        Query q = em.createQuery("SELECT b FROM Benefit b INNER JOIN sBenefit sb ON sb.benefitId=b.id " +
                "WHERE b.isActive=true AND (sb.planStatus='Inactive' OR b.terminationDate < :dt)");
        q.setParameter("dt", Helper.getMonthFor());
        List<Benefit> benefitList;
        try{
            benefitList = (List<Benefit>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        if(benefitList.size()==0)
            return;
        for(Benefit b: benefitList){
            em.getTransaction().begin();
            b.setActive(false);
            em.persist(b);
            em.getTransaction().commit();
        }
    }


}
