package net.superiorstate.ams.model.general;

import jakarta.persistence.*;
import net.superiorstate.ams.model.general.User;

@Entity
@Table(name = "user_filter_preset")
public class UserFilterPreset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "person_id", nullable = false)
    private User user;

    @Column(name = "slot_number", nullable = false)
    private int slotNumber;

    @Column(name = "label", columnDefinition = "varchar(16)", nullable = false)
    private String label;

    @Column(name = "view_renewal", nullable = false)
    private boolean viewRenewal;

    @Column(name = "view_setup", nullable = false)
    private boolean viewSetup;

    @Column(name = "view_ticket", nullable = false)
    private boolean viewTicket;

    @Column(name = "view_opportunity", nullable = false)
    private boolean viewOpportunity;

    @Column(name = "ownership_filter", nullable = false)
    private int ownershipFilter;

    @Column(name = "attention_filter", nullable = false)
    private int attentionFilter;

    @Column(name = "sort_alphabetically", nullable = false)
    private boolean sortAlphabetically;

    public UserFilterPreset() {}

    // ── Getters & Setters ──

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public int getSlotNumber() { return slotNumber; }
    public void setSlotNumber(int slotNumber) { this.slotNumber = slotNumber; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public boolean isViewRenewal() { return viewRenewal; }
    public void setViewRenewal(boolean viewRenewal) { this.viewRenewal = viewRenewal; }

    public boolean isViewSetup() { return viewSetup; }
    public void setViewSetup(boolean viewSetup) { this.viewSetup = viewSetup; }

    public boolean isViewTicket() { return viewTicket; }
    public void setViewTicket(boolean viewTicket) { this.viewTicket = viewTicket; }

    public boolean isViewOpportunity() { return viewOpportunity; }
    public void setViewOpportunity(boolean viewOpportunity) { this.viewOpportunity = viewOpportunity; }

    public int getOwnershipFilter() { return ownershipFilter; }
    public void setOwnershipFilter(int ownershipFilter) { this.ownershipFilter = ownershipFilter; }

    public int getAttentionFilter() { return attentionFilter; }
    public void setAttentionFilter(int attentionFilter) { this.attentionFilter = attentionFilter; }

    public boolean isSortAlphabetically() { return sortAlphabetically; }
    public void setSortAlphabetically(boolean sortAlphabetically) { this.sortAlphabetically = sortAlphabetically; }
}
