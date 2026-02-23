package net.superiorstate.ams.data;

public class ActivityFilter {
    private boolean viewRenewal;
    private boolean viewSetup;
    private boolean viewTicket;
    private boolean viewNeedsContact;
    private boolean viewWaitingOnUs;
    private boolean sortAlphabetically;
    private int ownershipFilter;

    private boolean viewOpportunity;
    private int needsContactWarning;

    public ActivityFilter(){}

    public boolean isViewRenewal() {
        return viewRenewal;
    }

    public void setViewRenewal(boolean viewRenewal) {
        this.viewRenewal = viewRenewal;
    }

    public boolean isViewSetup() {
        return viewSetup;
    }

    public void setViewSetup(boolean viewSetup) {
        this.viewSetup = viewSetup;
    }

    public boolean isViewTicket() {
        return viewTicket;
    }

    public void setViewTicket(boolean viewTicket) {
        this.viewTicket = viewTicket;
    }

    public boolean isViewNeedsContact() {
        return viewNeedsContact;
    }

    public void setViewNeedsContact(boolean viewNeedsContact) {
        this.viewNeedsContact = viewNeedsContact;
    }

    public boolean isViewWaitingOnUs() {
        return viewWaitingOnUs;
    }

    public void setViewWaitingOnUs(boolean viewWaitingOnUs) {
        this.viewWaitingOnUs = viewWaitingOnUs;
    }

    public boolean isSortAlphabetically() {
        return sortAlphabetically;
    }

    public void setSortAlphabetically(boolean sortAlphabetically) {
        this.sortAlphabetically = sortAlphabetically;
    }

    public int getOwnershipFilter() {
        return ownershipFilter;
    }

    public void setOwnershipFilter(int ownershipFilter) {
        this.ownershipFilter = ownershipFilter;
    }

    public int getNeedsContactWarning() {
        return needsContactWarning;
    }

    public void setNeedsContactWarning(int needsContactWarning) {
        this.needsContactWarning = needsContactWarning;
    }

    public boolean isViewOpportunity() { return viewOpportunity; }
    public void setViewOpportunity(boolean viewOpportunity) { this.viewOpportunity = viewOpportunity; }
    public void initializeFilter(){
        setViewRenewal(true);
        setViewSetup(true);
        setViewTicket(true);
        setViewNeedsContact(true);
        setViewWaitingOnUs(true);
        setSortAlphabetically(false);
        setOwnershipFilter(1);
        setViewOpportunity(false);
    }
}
