package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.model.activity.renewal.Renewal;
import net.superiorstate.ams.model.activity.renewal.RenewalEmployer;
import net.superiorstate.ams.model.activity.renewal.RenewalItem;
import net.superiorstate.ams.model.summit.archive.Benefit;
import net.superiorstate.ams.model.summit.archive.Employee;
import net.superiorstate.ams.model.summit.archive.Employer;

import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public abstract class RenewalQueryDAO {

    private static Date getCutoff(){
        LocalDate todayLd = LocalDate.now();
        LocalDate thisMonthLd = LocalDate.of(todayLd.getYear(), todayLd.getMonthValue(),1);
        LocalDate cutoffLd = thisMonthLd.plusMonths(3L);
        return Date.valueOf(cutoffLd);
    }

    private static List<Benefit> getUpcomingBenefits(EntityManager em){
        Query q = em.createQuery("Select b FROM Benefit b JOIN FETCH b.planType WHERE b.nextRenewalDue < :cutoff AND b.isActive=true order by b.employer.id,b.nextRenewalDue");
        q.setParameter("cutoff",getCutoff());
        List<Benefit> benefitList;
        try{
            benefitList = (List<Benefit>) q.getResultList();
        } catch (NoResultException e){
            benefitList = null;
        }
        return benefitList;
    }

    private static List<Benefit> getBenefitsInActiveRenewals(EntityManager em){
        Query q = em.createQuery("SELECT ri FROM RenewalItem ri WHERE ri.renewal.isComplete=false order by ri.renewal.employer.employerName");
        List<RenewalItem> renewalItemList;
        try{
            renewalItemList = (List<RenewalItem>) q.getResultList();
        } catch (NoResultException e){
            renewalItemList = null;
        }
        if(renewalItemList==null || renewalItemList.size()==0)
            return new ArrayList<>();
        List<Benefit> benefitList = new ArrayList<>();
        for(RenewalItem ri:renewalItemList){
            if(benefitList.contains(ri.getBenefit()))
                continue;
            benefitList.add(ri.getBenefit());
        }
        return benefitList;
    }
    public static List<RenewalEmployer> getEmployerRenewals(EntityManager em){
        List<Benefit> fullBenefitList = getUpcomingBenefits(em);
        if(fullBenefitList==null || fullBenefitList.size()==0) //No Benefits meet cutoff threshold
            return new ArrayList<>();
        List<Benefit> renewalBenefitList = getBenefitsInActiveRenewals(em);
        List<Benefit> benefitList;
        if(renewalBenefitList.size()>0){
            List<Integer> renewalBenefitIdList = new ArrayList<>();
            benefitList = new ArrayList<>();
            for(Benefit b:renewalBenefitList){
                renewalBenefitIdList.add(b.getId());
            }
            for(Benefit b:fullBenefitList){
                if(renewalBenefitIdList.contains(b.getId()))
                    continue;
                benefitList.add(b);
            }
        } else benefitList = fullBenefitList;
        //Build Initial Employer List from Benefits
        LocalDate todayLd = LocalDate.now();
        LocalDate thisMonthLd = LocalDate.of(todayLd.getYear(), todayLd.getMonthValue(),1);
        Date thisMonth = Date.valueOf(thisMonthLd);
        Date nextMonth = Date.valueOf(thisMonthLd.plusMonths(1L));
        List<RenewalEmployer> fullList = new ArrayList<>();
        List<Employer> employerCheck = new ArrayList<>();
        for(Benefit b: benefitList){
            Employer er = b.getEmployer();
            if(employerCheck.contains(er))
                continue;
            employerCheck.add(er);
            RenewalEmployer re = new RenewalEmployer();
            re.setEmployer(er);
            Date renewalDateCheck = Date.valueOf(LocalDate.of(b.getNextRenewalDue().toLocalDate().getYear(), b.getNextRenewalDue().toLocalDate().getMonthValue(), 1));
            if(renewalDateCheck.compareTo(thisMonth) < 0)
                re.setStage(0);
            else if(renewalDateCheck.compareTo(nextMonth)< 0)
                re.setStage(1);
            else if(renewalDateCheck.compareTo(nextMonth)==0)
                re.setStage(2);
            else re.setStage(3);
            re.setLastRenewed(b.getLastRenewed());
            fullList.add(re);
        }
        Collections.sort(fullList);
        return fullList;
    }

    /**
     * Returns employer renewals with benefits pre-loaded (2 queries total).
     * Each RenewalEmployer carries its sorted benefit list for client-side rendering.
     */
    public static List<RenewalEmployer> getEmployerRenewalsWithBenefits(EntityManager em) {
        List<Benefit> fullBenefitList = getUpcomingBenefits(em);
        if (fullBenefitList == null || fullBenefitList.isEmpty())
            return new ArrayList<>();

        List<Benefit> renewalBenefitList = getBenefitsInActiveRenewals(em);

        // Filter out benefits already in active renewals
        List<Benefit> benefitList;
        if (!renewalBenefitList.isEmpty()) {
            Set<Integer> renewalBenefitIds = new HashSet<>();
            for (Benefit b : renewalBenefitList) {
                renewalBenefitIds.add(b.getId());
            }
            benefitList = new ArrayList<>();
            for (Benefit b : fullBenefitList) {
                if (!renewalBenefitIds.contains(b.getId()))
                    benefitList.add(b);
            }
        } else {
            benefitList = fullBenefitList;
        }

        // Group benefits by employer (query already ordered by employer.id, nextRenewalDue)
        LocalDate todayLd = LocalDate.now();
        LocalDate thisMonthLd = LocalDate.of(todayLd.getYear(), todayLd.getMonthValue(), 1);
        Date thisMonth = Date.valueOf(thisMonthLd);
        Date nextMonth = Date.valueOf(thisMonthLd.plusMonths(1L));

        Map<Integer, RenewalEmployer> employerMap = new LinkedHashMap<>();
        for (Benefit b : benefitList) {
            Employer er = b.getEmployer();
            RenewalEmployer re = employerMap.get(er.getId());
            if (re == null) {
                re = new RenewalEmployer();
                re.setEmployer(er);
                // Stage based on earliest benefit (first encountered due to query ordering)
                Date renewalDateCheck = Date.valueOf(LocalDate.of(
                        b.getNextRenewalDue().toLocalDate().getYear(),
                        b.getNextRenewalDue().toLocalDate().getMonthValue(), 1));
                if (renewalDateCheck.compareTo(thisMonth) < 0)
                    re.setStage(0);
                else if (renewalDateCheck.compareTo(nextMonth) < 0)
                    re.setStage(1);
                else if (renewalDateCheck.compareTo(nextMonth) == 0)
                    re.setStage(2);
                else
                    re.setStage(3);
                re.setLastRenewed(b.getLastRenewed());
                employerMap.put(er.getId(), re);
            }
            re.getBenefits().add(b);
        }

        List<RenewalEmployer> fullList = new ArrayList<>(employerMap.values());
        Collections.sort(fullList);
        return fullList;
    }

    public static List<Employer> getEmployersWithUpcomingRenewals(EntityManager em){
        Query q1 = em.createQuery("SELECT b FROM Benefit b WHERE b.nextRenewalDue < :nextDate AND b.isActive=true order by b.employer.employerName");
        LocalDate todayLd = LocalDate.now();
        LocalDate thisMonthLd = LocalDate.of(todayLd.getYear(), todayLd.getMonthValue(),1);
        LocalDate cutoffLd = thisMonthLd.plusMonths(3L);
        Date cutoff = Date.valueOf(cutoffLd);
        q1.setParameter("nextDate",cutoff);
        List<Benefit> fullBenefitList;
        try{
            fullBenefitList = (List<Benefit>) q1.getResultList();
            System.out.println("Benefits Found: " + fullBenefitList.size());
        } catch (NoResultException e){
            fullBenefitList = null;
            System.out.println("No Benefits From Query");
        }
        if(fullBenefitList==null || fullBenefitList.size()==0)
            return null;

        Query q2 = em.createQuery("SELECT ri FROM RenewalItem ri WHERE ri.renewal.isComplete = false");
        List<RenewalItem> activeRenewalItemList;
        try{
            activeRenewalItemList = (List<RenewalItem>) q2.getResultList();
        } catch (NoResultException e){
            activeRenewalItemList = null;
        }

        List<Employer> employerList = new ArrayList<>();

        if(activeRenewalItemList==null || activeRenewalItemList.size()==0){
            for(Benefit b:fullBenefitList)
                if(!employerList.contains(b.getEmployer()))
                    employerList.add(b.getEmployer());
            System.out.println("Employer Size 1: "+ employerList.size());
        }
        else{
            List<Benefit> activeBenefitList = new ArrayList<>();
            for(RenewalItem ri: activeRenewalItemList)
                if(!activeBenefitList.contains(ri.getBenefit()))
                    activeBenefitList.add(ri.getBenefit());
            List<Benefit> inActiveBenefitList = new ArrayList<>();
            for(Benefit b:fullBenefitList)
                if(!activeBenefitList.contains(b))
                    inActiveBenefitList.add(b);
            for(Benefit b:inActiveBenefitList)
                if(!employerList.contains(b.getEmployer()))
                    employerList.add(b.getEmployer());
            System.out.println("Employer Size 2: "+ employerList.size());
        }
        return employerList;
    }



    public static List<Benefit> getBenefitsNotInRenewal(EntityManager em, Renewal r){
        List<Benefit> allBenefits = null;
        Query q = em.createQuery("SELECT b FROM Benefit b WHERE b.employer.id = :id and b.isActive=true");
        q.setParameter("id",r.getEmployer().getId());
        try{
            allBenefits = (List<Benefit>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
        }
        if(allBenefits==null)
            return new ArrayList<>();

        List<RenewalItem> activeRenewalItems = null;
        q = em.createQuery("SELECT ri FROM RenewalItem ri WHERE ri.renewal.isComplete=false and ri.renewal.employer.id = :id");
        q.setParameter("id",r.getEmployer().getId());
        try{
            activeRenewalItems = (List<RenewalItem>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
        }
        if(activeRenewalItems==null)
            return allBenefits;

        for(RenewalItem ri:activeRenewalItems){
            Benefit b = ri.getBenefit();
            allBenefits.remove(b);
        }
        System.out.println("_____________ALL BENEFITS NOT IN RENEWAL_____");
        for(Benefit b:allBenefits){
            System.out.println("BENEFIT: "+b.getPlanName());
        }
        return allBenefits;
    }
    private static List<Employee> getEmployeesForRenewal(EntityManager em, Renewal r){
        Query q = em.createQuery("SELECT e FROM Employee e WHERE e.employer.id = :id order by e.lastName,e.firstName");
        q.setParameter("id",r.getEmployer().getId());
        List<Employee> employeeList;
        try{
            employeeList = (List<Employee>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            employeeList = new ArrayList<>();
        }
        return employeeList;
    }

    public static List<Employee> getEmployeesAssignedToRenewal(EntityManager em, Renewal r){
        Query q = em.createQuery("SELECT r FROM Renewal r WHERE r.id = :id");
        q.setParameter("id",r.getId());
        Renewal renewal;
        try{
            renewal = (Renewal) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            return new ArrayList<>();
        }
        return r.getEmployer().getContactList();
    }

    public static List<Employee> getContactsNotAssigned(EntityManager em, Renewal r){
        List<Employee> employeeList = getEmployeesForRenewal(em,r);
        List<Employee> assignedList = getEmployeesAssignedToRenewal(em,r);
        for(Employee e: assignedList){
            employeeList.remove(e);
        }
        return employeeList;
    }

}
