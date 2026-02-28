package net.superiorstate.ams.data.dao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TaskSequenceTable;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.checklist.tasks.SortedTask;
import net.superiorstate.ams.model.activity.note.ActivityStatus;
import net.superiorstate.ams.model.activity.note.ReasonCreated;
import net.superiorstate.ams.model.activity.ticket.*;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.WebLink;
import net.superiorstate.ams.model.summit.archive.Employee;

import java.util.ArrayList;
import java.util.List;

public abstract class TicketQueryDAO {


    public static List<Employee> getEmployeeList(EntityManager em){
        Query q = em.createQuery("SELECT e FROM Employee e ORDER BY e.lastName,e.firstName,e.employer.employerName");
        List<Employee> employees;
        try{
            employees = (List<Employee>) q.getResultList();
        }catch (NoResultException e){
            e.printStackTrace();
            employees = new ArrayList<>();
        }
        return employees;
    }
    public static List<tEmployee> getTicketEmployeeListBulk(EntityManager em) {
        String sql = "SELECT e.employee_id, e.first_name, e.last_name, er.employer_name, er.organization_id, " +
                "e.email, e.hr_email, p.id, p.email AS p_email, p.phone " +
                "FROM employee e " +
                "JOIN employer er ON er.organization_id = e.employer_id " +
                "LEFT JOIN assignee p ON p.employee_id = e.employee_id AND p.DTYPE = 'Person' " +
                "WHERE e.last_name IS NOT NULL AND e.last_name <> '' " +
                "ORDER BY e.last_name, e.first_name, er.employer_name";
        Query q = em.createNativeQuery(sql);
        List<Object[]> rows = q.getResultList();
        List<tEmployee> list = new ArrayList<>();
        for (Object[] row : rows) {
            tEmployee t = new tEmployee();
            t.setId(((Number) row[0]).intValue());
            t.setFirstName((String) row[1]);
            t.setLastName((String) row[2]);
            t.setEmployer((String) row[3]);
            t.setEmployerId(((Number) row[4]).intValue());

            String eeEmail = (String) row[5];
            String hrEmail = (String) row[6];
            Number personIdNum = (Number) row[7];
            String personEmail = (String) row[8];
            String personPhone = (String) row[9];

            if (personEmail != null && !personEmail.isEmpty()) {
                t.setEmail(personEmail);
                t.setHasEmail(true);
            } else if (hrEmail != null && !hrEmail.isEmpty()) {
                t.setEmail(hrEmail);
                t.setHasEmail(true);
            } else if (eeEmail != null && !eeEmail.isEmpty()) {
                t.setEmail(eeEmail);
                t.setHasEmail(true);
            } else {
                t.setHasEmail(false);
            }

            if (personPhone != null && !personPhone.isEmpty()) {
                t.setPhone(personPhone);
                t.setHasPhone(true);
            } else {
                t.setHasPhone(false);
            }

            if (personIdNum != null) {
                t.setIsPerson(true);
                t.setPersonId(personIdNum.longValue());
            }

            list.add(t);
        }
        return list;
    }
    public static List<TicketSubCategory> getTicketSubCats(EntityManager em){
        Query q = em.createQuery("SELECT t FROM TicketSubCategory t WHERE t.isActive=true order by t.ticketCategory.shortText , t.description");
        return (List<TicketSubCategory>) q.getResultList();
    }

    public static List<TicketCategory> getTicketCategories(EntityManager em){
        Query q = em.createQuery("SELECT tc FROM TicketCategory tc where tc.active = true order by tc.shortText");
        return (List<TicketCategory>) q.getResultList();
    }

    public static List<WebLink> getInsertLinkList(EntityManager em){
        List<WebLink> insertLinkList;
        Query q = em.createQuery("SELECT w FROM WebLink w WHERE w.linkType.id = :id and w.active=true order by w.plainText");
        q.setParameter("id",3);
        try{
            insertLinkList = (List<WebLink>) q.getResultList();
        } catch (NoResultException e){
            insertLinkList = null;
        }
        return insertLinkList;
    }

    public static List<tEmployee> getTicketEmployeeList(EntityManager em, List<Employee> employees){
        List<tEmployee> employeeList = new ArrayList<>();
        for(Employee e: employees){
            if(e.getLastName()==null)
                continue;
            if(e.getLastName().equalsIgnoreCase(""))
                continue;
            tEmployee t = new tEmployee();
            t.setId(e.getId());
            t.setFirstName(e.getFirstName());
            t.setLastName(e.getLastName());
            t.setEmployer(e.getEmployer().getEmployerName());
            t.setEmployerId(e.getEmployer().getId());
            Person p = getPersonByEmployee(em,e);
            if(p!=null && p.getEmail()!=null && !p.getEmail().equals("")){
                t.setEmail(p.getEmail());
                t.setHasEmail(true);
            } else if(e.getEmail()!=null && !e.getEmail().equals("")){
                t.setEmail(e.getEmail());
                t.setHasEmail(true);
            } else {
                t.setHasEmail(false);
            }
            if(p!=null && p.getPhone()!=null && !p.getPhone().equals("")){
                t.setPhone(p.getPhone());
                t.setHasPhone(true);
            } else {
                t.setHasPhone(false);
            }
            if(p!=null){
                t.setIsPerson(true);
                t.setPersonId(p.getId());
            }
            employeeList.add(t);
        }
        return employeeList;
    }

    public static List<tEmployee> getTicketEmployeeList(EntityManager em){
        List<Employee> employees = getEmployeeList(em);
        return getTicketEmployeeList(em,employees);
    }

    public static List<TicketSubCategory> getTicketSubCategoryList(EntityManager em){
        Query q = em.createQuery("SELECT tsc FROM TicketSubCategory tsc WHERE tsc.isActive = true AND tsc.ticketCategory.active=true order by tsc.ticketCategory.description,tsc.description");
        List<TicketSubCategory> tsc;
        try{
            tsc = (List<TicketSubCategory>) q.getResultList();
        } catch (NoResultException e){
            tsc = new ArrayList<>();
        }
        return tsc;
    }

    public static List<ServiceItem> getActiveTicketServiceItems(EntityManager em){
        return em.createQuery(
                        "SELECT si FROM ServiceItem si WHERE si.activityCategory.id = 3 AND si.suppressed = false AND si.ticketCategory IS NOT NULL AND si.ticketCategory.active = true ORDER BY si.ticketCategory.shortText, si.description",
                        ServiceItem.class)
                .getResultList();
    }
    public static Person getPersonByEmployee(EntityManager em, Employee e) {
        try {
            List<Person> results = em.createQuery(
                            "SELECT p FROM Person p WHERE p.employee.id = :id", Person.class)
                    .setParameter("id", e.getId())
                    .setMaxResults(1) // Just in case multiple match, get only the first
                    .getResultList();

            return results.isEmpty() ? null : results.get(0);
        } catch (Exception ex) {
            // Optional: log or rethrow if needed
            System.err.println("❌ Unexpected error in getPersonByEmployee: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }



    public static List<ContactMethod> getContactMethods(EntityManager em){
        Query q = em.createQuery("SELECT c FROM ContactMethod c");
        return (List<ContactMethod>) q.getResultList();
    }

    public static List<ActivityStatus> getActivityStatuses(EntityManager em){
        Query q = em.createQuery("SELECT a FROM ActivityStatus a ORDER BY a.description");
        return (List<ActivityStatus>) q.getResultList();
    }
    public static List<ReasonCreated> getReasons(EntityManager em){
        Query q = em.createQuery("SELECT r FROM ReasonCreated r ORDER BY r.description");
        return (List<ReasonCreated>) q.getResultList();
    }
    /**
     * Resolves the ServiceItem for a ticket.
     * Prefers the new direct ticketServiceItem FK; falls back to ticketSubCategory chain for legacy tickets.
     */
    private static ServiceItem resolveServiceItemForTicket(EntityManager em, Ticket t){
        // New path: direct FK
        if(t.getTicketServiceItem() != null)
            return t.getTicketServiceItem();
        // Legacy path: through TicketSubCategory
        try {
            if(t.getTicketSubCategory() != null && t.getTicketSubCategory().getServiceItem() != null)
                return t.getTicketSubCategory().getServiceItem();
        } catch (Exception ignored){}
        return null;
    }
    public static List<SortedTask> getTasksRequiredForTheTicket(EntityManager em, Ticket t){
        List<SortedTask> sortedTaskList = new ArrayList<>();
        ServiceItem tp = resolveServiceItemForTicket(em, t);
        if(tp == null) return sortedTaskList;
        RequiredTaskList rtl;
        Query q = em.createQuery("SELECT rtl FROM RequiredTaskList rtl WHERE rtl.serviceItem.id = :id");
        q.setParameter("id",tp.getId());
        try{
            rtl = (RequiredTaskList) q.getSingleResult();
        } catch (Exception e){
            return sortedTaskList;
        }
        if(rtl==null)
            return null;
        List<TaskSequenceTable> taskSequenceTableList;
        Query query = em.createQuery("SELECT tst FROM TaskSequenceTable tst WHERE tst.taskSequence.id = :ts");
        query.setParameter("ts",rtl.getId());
        try{
            taskSequenceTableList = (List<TaskSequenceTable>) query.getResultList();
        } catch (NoResultException ex){
            return null;
        }
        if(taskSequenceTableList.size()==0)
            return null;
        for(TaskSequenceTable tst:taskSequenceTableList){
            sortedTaskList.add(new SortedTask(tst.getTask(),tst.getSortOrder()));
        }
        return sortedTaskList;
    }


    public static List<SortedTask> getTasksRequiredForTicket(EntityManager em, Ticket t){
        ServiceItem tp = resolveServiceItemForTicket(em, t);
        int tpId = (tp != null) ? tp.getId() : -1;
        List<SortedTask> sortedTaskList = new ArrayList<>();
        if(tpId<0)
            return sortedTaskList;
        Query q = em.createQuery("SELECT rtl FROM RequiredTaskList rtl WHERE rtl.serviceItem.id = :id");
        q.setParameter("id",tpId);
        RequiredTaskList rtl;
        try{
            rtl = (RequiredTaskList) q.getSingleResult();
        } catch (NoResultException e){
            return sortedTaskList;
        }
        if(rtl==null)
            return sortedTaskList;
        List<TaskSequenceTable> tstList = rtl.getTaskSequenceTableList();
        for(TaskSequenceTable tst:tstList){
            sortedTaskList.add(new SortedTask(tst.getTask(),tst.getSortOrder()));
        }
        return sortedTaskList;
    }


}
