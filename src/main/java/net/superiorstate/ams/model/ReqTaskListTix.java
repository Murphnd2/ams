package net.superiorstate.ams.model;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;

public class ReqTaskListTix {
    long id;
    String description;
    RequiredTaskList requiredTaskList;
    ServiceItem serviceItem;

    public ReqTaskListTix(EntityManager em, long id){
        fillRequiredTask(em,id);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RequiredTaskList getRequiredTaskList() {
        return requiredTaskList;
    }

    public void setRequiredTaskList(RequiredTaskList requiredTaskList) {
        this.requiredTaskList = requiredTaskList;
    }

    public ServiceItem getServiceItem() {
        return serviceItem;
    }

    public void setServiceItem(ServiceItem serviceItem) {
        this.serviceItem = serviceItem;
    }

    public void fillRequiredTask(EntityManager em, long id){
        setRequiredTaskList(EntityLookup.getReqListById(em,id));
        setServiceItem(EntityLookup.getServiceItemById(em,getRequiredTaskList().getServiceItem().getId()));
        setId(getRequiredTaskList().getId());
        // Build description from ServiceItem's ticketCategory + description
        String catShort = (getServiceItem().getTicketCategory() != null)
                ? getServiceItem().getTicketCategory().getShortText()
                : "General";
        setDescription(catShort + " - " + getServiceItem().getDescription());
    }
}
