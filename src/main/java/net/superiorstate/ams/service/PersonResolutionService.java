// src/main/java/net/superiorstate/ams/service/PersonResolutionService.java
package net.superiorstate.ams.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.general.Address;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Employee;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PersonResolutionService {

    private static final PersonResolutionService INSTANCE = new PersonResolutionService();
    private static final Pattern EMPLOYEE_ID_PATTERN = Pattern.compile("\\((\\d+)\\)");
    private static final Pattern PERSON_ID_PATTERN   = Pattern.compile("\\{(\\d+)\\}");

    private PersonResolutionService() {}

    public static PersonResolutionService getInstance() {
        return INSTANCE;
    }

    public Person getBestPersonFromString(EntityManager em, String input) {
        if (input == null || input.trim().isBlank()) {
            return null;
        }
        String text = input.trim();

        // 1. Employee ID in parentheses → e.g. "John Doe (12345)"
        Integer employeeId = parseEmployeeId(text);
        if (employeeId != null) {
            Employee ee = dM.getEmployeeById(em, employeeId);
            if (ee != null) {
                return getOrCreatePersonForEmployee(em, ee);
            }
        }

        // 2. Person ID in braces → e.g. "{987}"
        Long personId = parsePersonId(text);
        if (personId != null) {
            return dM.getPersonById(em, personId);
        }

        // 3. Valid email → try as Employee first
        if (dbEmail.isValidEmail(text)) {
            Employee ee = getEmployeeByEmail(em, text);
            if (ee != null) {
                return getOrCreatePersonForEmployee(em, ee);
            }
        }

        // 4. Valid email → try existing non-employee Person
        if (dbEmail.isValidEmail(text)) {
            Person existing = getPersonByEmail(em, text);
            if (existing != null) {
                return existing;
            }
        }

        // 5. Looks like full name → try Employee
        if (isProbablyFullName(text)) {
            Employee ee = getEmployeeByFullName(em, text);
            if (ee != null) {
                return getOrCreatePersonForEmployee(em, ee);
            }
        }

        // 6. Looks like full name → try existing Person
        if (isProbablyFullName(text)) {
            Person p = getPersonByFullName(em, text);
            if (p != null) {
                return p;
            }
        }

        return null; // caller will fall back to createPersonFromEmail if needed
    }

    // ────────────────────── Helper methods (all private, no static state) ──────────────────────

    public Person getOrCreatePersonForEmployee(EntityManager em, Employee ee) {
        List<Person> list = em.createQuery(
                        "SELECT p FROM Person p WHERE p.employee.id = :eeId", Person.class)
                .setParameter("eeId", ee.getId())
                .getResultList();

        if (!list.isEmpty()) {
            return list.get(0);
        }

        // Create new Person linked to this Employee (same logic as old eV)
        em.getTransaction().begin();
        try {
            Address a = new Address();
            if (ee.getZipCode() != null) a.setZipCode(ee.getZipCode());
            if (ee.getCity() != null)    a.setCity(ee.getCity());
            if (ee.getAddress1() != null) a.setAddress1(ee.getAddress1());
            if (ee.getAddress2() != null) a.setAddress2(ee.getAddress2());
            if (ee.getState() != null && ee.getState().length() >= 2) {
                a.setState(ee.getState().substring(0, 2));
            }
            em.persist(a);

            Person p = new Person();
            p.setAddress(a);
            p.setEmployee(ee);
            p.setPsp(dM.getPspById(em, 4L));

            // Prefer HR email, then regular email
            if (ee.getHrEmail() != null && dbEmail.isValidEmail(ee.getHrEmail())) {
                p.setEmail(ee.getHrEmail().trim().toLowerCase());
            } else if (ee.getEmail() != null && dbEmail.isValidEmail(ee.getEmail())) {
                p.setEmail(ee.getEmail().trim().toLowerCase());
            }

            if (ee.getFirstName() != null) p.setFirstName(ee.getFirstName().trim().toUpperCase());
            if (ee.getLastName() != null)  p.setLastName(ee.getLastName().trim().toUpperCase());

            em.persist(p);
            em.getTransaction().commit();
            return p;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        }
    }

    private Integer parseEmployeeId(String text) {
        Matcher m = EMPLOYEE_ID_PATTERN.matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private Long parsePersonId(String text) {
        Matcher m = PERSON_ID_PATTERN.matcher(text);
        if (m.find()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private Person getPersonByEmail(EntityManager em, String email) {
        String scrubbed = email.trim().toLowerCase();
        try {
            List<Long> ids = em.createQuery(
                            "SELECT p.id FROM PersonV p WHERE p.email = :email ORDER BY p.id DESC", Long.class)
                    .setParameter("email", scrubbed)
                    .setMaxResults(5)  // safety
                    .getResultList();

            if (!ids.isEmpty()) {
                // Prefer the one that actually has an Employee record attached
                for (Long id : ids) {
                    Person p = dM.getPersonById(em, id);
                    if (p != null && p.getEmployee() != null) {
                        return p;
                    }
                }
                // Otherwise return the most recent one
                return dM.getPersonById(em, ids.get(0));
            }
        } catch (Exception e) {
            // swallow – we don’t want one bad row to break everything
            return null;
        }
        return null;
    }

    private Employee getEmployeeByEmail(EntityManager em, String email) {
        String scrubbed = email.trim().toLowerCase();
        try {
            List<Employee> list = em.createQuery(
                            "SELECT e FROM EmployeeV e WHERE " +
                                    "(e.emailSystem = :email OR e.emailSummit = :email) " +
                                    "ORDER BY e.id DESC", Employee.class)
                    .setParameter("email", scrubbed)
                    .setMaxResults(5)
                    .getResultList();

            return list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    private Employee getEmployeeByFullName(EntityManager em, String fullName) {
        Name name = parseName(fullName);
        if (name.first() == null || name.last() == null) return null;

        try {
            return em.createQuery(
                            "SELECT e FROM EmployeeV e WHERE e.firstName = :f AND e.lastName = :l ORDER BY e.id DESC", Employee.class)
                    .setParameter("f", name.first())
                    .setParameter("l", name.last())
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private Person getPersonByFullName(EntityManager em, String fullName) {
        Name name = parseName(fullName);
        if (name.first() == null || name.last() == null) return null;

        try {
            Long id = em.createQuery(
                            "SELECT p.id FROM PersonV p WHERE p.firstName = :f AND p.lastName = :l ORDER BY p.id DESC", Long.class)
                    .setParameter("f", name.first())
                    .setParameter("l", name.last())
                    .setMaxResults(1)
                    .getSingleResult();
            return dM.getPersonById(em, id);
        } catch (NoResultException e) {
            return null;
        }
    }

    private boolean isProbablyFullName(String text) {
        return text.contains(" ") || text.contains(",");
    }

    private record Name(String first, String last) {}

    private Name parseName(String fullName) {
        if (fullName.contains(",")) {
            String[] parts = fullName.split(",", 2);
            String last = parts[0].trim().toUpperCase();
            String first = parts.length > 1 ? parts[1].trim().toUpperCase() : null;
            return new Name(first, last);
        } else {
            String[] parts = fullName.trim().split("\\s+", 2);
            if (parts.length < 2) return new Name(null, null);
            return new Name(parts[0].toUpperCase(), parts[1].toUpperCase());
        }
    }

    /**
     * New unified entry point used by the servlet.
     * Returns existing Person if found, otherwise creates one from the email.
     */
    public Person resolveOrCreatePerson(EntityManager em, String input) {
        Person p = getBestPersonFromString(em, input);
        if (p != null) {
            return p;
        }

        // Nothing found → create from email (only if it's actually a valid email)
        if (dbEmail.isValidEmail(input)) {
            return createPersonFromEmail(em, input);
        }

        return null;
    }

    /** Same logic as the old eV class had – now lives safely inside the service */
    private Person createPersonFromEmail(EntityManager em, String email) {
        em.getTransaction().begin();
        try {
            Person p = new Person();
            p.setPsp(dM.getPspById(em, 4L));
            p.setEmail(email.trim().toLowerCase());

            // Try to guess name from email (e.g. john.doe@company.com → John Doe)
            String localPart = email.substring(0, email.indexOf('@')).toLowerCase();
            if (localPart.contains(".")) {
                String[] parts = localPart.split("\\.", 2);
                p.setFirstName(capitalize(parts[0]));
                if (parts.length > 1) {
                    p.setLastName(capitalize(parts[1]));
                }
            } else {
                p.setFirstName(capitalize(localPart));
            }

            em.persist(p);
            em.getTransaction().commit();
            return p;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}