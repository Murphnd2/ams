package net.superiorstate.ams.data.resolver;

import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.renewal.Renewal;
import net.superiorstate.ams.model.activity.ticket.Ticket;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.summit.archive.Employee;
import net.superiorstate.ams.model.summit.archive.Employer;

/**
 * Resolves the Summit employer id (Employer.altId) for the current active
 * activity. Returns null when the activity has no resolvable employer
 * ("not applicable"): non-Renewal/Ticket types, tickets whose person is not
 * an employee of an employer, or any null hop in the chain.
 */
public final class SummitEmployerResolver {

    private SummitEmployerResolver() {}

    /** @return the employer altId, or null if not applicable. */
    public static Integer resolveAltId(Activity activity) {
        Employer employer = resolveEmployer(activity);
        return employer == null ? null : employer.getAltId();
    }

    private static Employer resolveEmployer(Activity activity) {
        if (activity == null) return null;

        if (activity instanceof Renewal) {
            return ((Renewal) activity).getEmployer();
        }

        if (activity instanceof Ticket) {
            Ticket t = (Ticket) activity;
            Person person = t.getPrimaryContact() != null ? t.getPrimaryContact() : t.getContact();
            if (person == null) return null;
            Employee employee = person.getEmployee();
            if (employee == null) return null;
            return employee.getEmployer();
        }

        return null;
    }
}
