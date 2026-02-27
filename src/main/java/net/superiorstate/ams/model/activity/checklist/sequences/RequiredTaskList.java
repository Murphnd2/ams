package net.superiorstate.ams.model.activity.checklist.sequences;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;

@Entity
public class RequiredTaskList extends TaskSequence {

    @OneToOne
    @JoinColumn(name="purpose_id")
    private ServiceItem serviceItem;

    public RequiredTaskList(){}

    public ServiceItem getServiceItem() {
        return serviceItem;
    }

    public void setServiceItem(ServiceItem serviceItem) {
        this.serviceItem = serviceItem;
    }
}
