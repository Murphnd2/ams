package net.superiorstate.ams.model;

public class ActivityLandingFilter {
    public boolean myOpenOnly;

    public boolean includeRenewal = true;
    public boolean includeSetup = true;
    public boolean includeTicket = true;

    public boolean viewNeedsContact;
    public boolean viewWaitingOnUs;

    public boolean sortAlphabetically;   // <-- used by SQL now

    public int pageSize = 500;
    public int offset = 0;
}

