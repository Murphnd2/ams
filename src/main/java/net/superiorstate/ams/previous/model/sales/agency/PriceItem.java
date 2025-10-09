package net.superiorstate.ams.previous.model.sales.agency;

import jakarta.persistence.*;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.WebLink;

@Entity
public class PriceItem implements Comparable<PriceItem> {

    @Id
    @GeneratedValue
    @Column(name="price_item_id")
    private Long id;

    @Column(name="description", columnDefinition = "varchar(100)")
    private String description;

    @Column(name="sort_order")
    private int sortOrder;

    @Column
    private boolean suppressed;

    @ManyToOne
    @JoinColumn(name="weblink_id")
    private WebLink webLink;

    @ManyToOne
    @JoinColumn(name = "psp_id")
    private PSP psp;

    public PriceItem (){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isSuppressed() {
        return suppressed;
    }

    public void setSuppressed(boolean suppressed) {
        this.suppressed = suppressed;
    }

    public WebLink getWebLink() {
        return webLink;
    }

    public void setWebLink(WebLink webLink) {
        this.webLink = webLink;
    }

    public PSP getPsp() {
        return psp;
    }

    public void setPsp(PSP psp) {
        this.psp = psp;
    }
    @Override
    public int compareTo(PriceItem pi) {
        if(this.getSortOrder() < pi.getSortOrder()) {
            return -1;
        } else if (this.getSortOrder() > pi.getSortOrder()){
            return 1;
        } else if (this.getDescription().compareTo(pi.getDescription()) !=0) {
            return this.getDescription().compareTo(pi.getDescription());
        } else {
            return this.getId().compareTo(pi.getId());
        }
    }
}
