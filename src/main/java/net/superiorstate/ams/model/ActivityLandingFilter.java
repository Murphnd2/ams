package net.superiorstate.ams.model;

public class ActivityLandingFilter {
    /**
     * 0 = All Open
     * 1 = My World (I own + actionable delegated)
     * 2 = I Own (assigned_to_id = me only)
     * 3 = Helping On (actionable delegated only, not owned)
     */
    public int ownershipFilter = 1;

    public boolean includeRenewal = true;
    public boolean includeSetup = true;
    public boolean includeTicket = true;

    public boolean includeOpportunity = false;
    public boolean viewNeedsContact;
    public boolean viewWaitingOnUs;
    public boolean sortAlphabetically;

    public int pageSize = 500;
    public int offset = 0;
}
