package net.superiorstate.ams.model.sales.application;

import jakarta.persistence.*;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.sales.offering.LOS;

import java.util.List;

@Entity
@Table(name="applicationsection")
public class ApplicationSection implements Comparable<ApplicationSection> {
    @Id
    @GeneratedValue
    @Column(name="section_id")
    private Long id;

    @Column(name="name",columnDefinition = "varchar(100)",nullable = false)
    private String name;

    @Column(name="description",columnDefinition = "varchar(500)")
    private String description;

    @Column(name="scope",columnDefinition = "varchar(10) DEFAULT 'ALL'",nullable = false)
    private String scope;

    @Column(name="sort_order")
    private int sortOrder;

    @ManyToOne
    @JoinColumn(name="psp_id",nullable = false)
    private PSP psp;

    @ManyToMany
    @JoinTable(name="applicationsectionlos",
            joinColumns = @JoinColumn(name="section_id"),
            inverseJoinColumns = @JoinColumn(name="los_id"))
    private List<LOS> losList;

    @OneToMany(mappedBy = "applicationSection")
    @OrderBy("sortOrder")
    private List<ApplicationField> fieldList;

    public ApplicationSection(){}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public PSP getPsp() { return psp; }
    public void setPsp(PSP psp) { this.psp = psp; }

    public List<LOS> getLosList() { return losList; }
    public void setLosList(List<LOS> losList) { this.losList = losList; }

    public List<ApplicationField> getFieldList() { return fieldList; }
    public void setFieldList(List<ApplicationField> fieldList) { this.fieldList = fieldList; }

    @Override
    public int compareTo(ApplicationSection o) {
        return Integer.compare(this.sortOrder, o.getSortOrder());
    }
}