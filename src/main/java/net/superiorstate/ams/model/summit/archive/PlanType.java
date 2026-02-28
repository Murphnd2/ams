package net.superiorstate.ams.model.summit.archive;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.billing.BillingGroup;

@Entity
public class PlanType {

    @Id
    @Column(name="PlanType_ID")
    private int planTypeId;

    @Column(name="Code")
    private String code;

    @Column(name="PlanTypeName")
    private String planTypeName;

    @Column(name="level")
    private String level;

    @Column(name="los")
    private String los;

    @Column(name="employer_name")
    private String employerName;

    @ManyToOne
    @JoinColumn(name="billing_group_id")
    private BillingGroup billingGroup;

    @ManyToOne
    @JoinColumn(name="purpose_id")
    private ServiceItem serviceItem;

    public PlanType(){}

    public int getPlanTypeId() {
        return planTypeId;
    }

    public String getCode() {
        return code;
    }

    public String getPlanTypeName() {
        return planTypeName;
    }

    public void setPlanTypeId(int planTypeId) {
        this.planTypeId = planTypeId;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setPlanTypeName(String planTypeName) {
        this.planTypeName = planTypeName;
    }

    public BillingGroup getBillingGroup() {
        return billingGroup;
    }

    public void setBillingGroup(BillingGroup billingGroup) {
        this.billingGroup = billingGroup;
    }

    public ServiceItem getServiceItem() {
        return serviceItem;
    }

    public void setServiceItem(ServiceItem serviceItem) {
        this.serviceItem = serviceItem;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLos() {
        return los;
    }

    public void setLos(String los) {
        this.los = los;
    }

    public String getEmployerName() {
        return employerName;
    }

    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }
}
