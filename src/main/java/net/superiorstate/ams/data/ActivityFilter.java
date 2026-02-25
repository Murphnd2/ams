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

    // New: single int replacing the two-boolean attention matrix
    // 0=Show All, 1=Needs Attention (either), 2=Waiting On Us only, 3=Needs Contact only
    private int attentionFilter;

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

    public int getAttentionFilter() { return attentionFilter; }

    /**
     * Sets the attention filter and syncs the legacy boolean fields so that
     * downstream code (ActivityLandingDao, AmsDataLocal.filterActivityListing)
     * continues to work without changes.
     *
     * 0 = Show All        → both false
     * 1 = Needs Attention  → both true (either flag matches)
     * 2 = Waiting On Us    → waitingOnUs=true, needsContact=false
     * 3 = Needs Contact    → waitingOnUs=false, needsContact=true
     */
    public void setAttentionFilter(int attentionFilter) {
        this.attentionFilter = attentionFilter;
        switch (attentionFilter) {
            case 1 -> { viewWaitingOnUs = true;  viewNeedsContact = true;  }
            case 2 -> { viewWaitingOnUs = true;  viewNeedsContact = false; }
            case 3 -> { viewWaitingOnUs = false; viewNeedsContact = true;  }
            default -> { viewWaitingOnUs = false; viewNeedsContact = false; }
        }
    }

    public void initializeFilter(){
        setViewRenewal(true);
        setViewSetup(true);
        setViewTicket(true);
        setAttentionFilter(1);
        setSortAlphabetically(false);
        setOwnershipFilter(1);
        setViewOpportunity(false);
    }
}
