package net.superiorstate.ams.model;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.ticket.TicketSubCategory;

import java.util.List;

public class ReqTaskListTix {
    long id;
    String description;
    RequiredTaskList requiredTaskList;
    ServiceItem serviceItem;
    TicketSubCategory ticketSubCategory;

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

    public TicketSubCategory getTicketSubCategory() {
        return ticketSubCategory;
    }

    public void setTicketSubCategory(TicketSubCategory ticketSubCategory) {
        this.ticketSubCategory = ticketSubCategory;
    }

    public void fillRequiredTask(EntityManager em,long id){
        setRequiredTaskList(EntityLookup.getReqListById(em,id));
        setServiceItem(EntityLookup.getServiceItemById(em,getRequiredTaskList().getServiceItem().getId()));
        Query q = em.createQuery("SELECT tsc FROM TicketSubCategory tsc WHERE tsc.serviceItem.id = :id");
        q.setParameter("id", getServiceItem().getId());
        TicketSubCategory tsc;
        List<?> results = q.getResultList();
        tsc = results.isEmpty() ? null : (TicketSubCategory) results.get(0);
        setTicketSubCategory(tsc);
        setId(getRequiredTaskList().getId());
        assert tsc != null;
        setDescription(tsc.getTicketCategory().getShortText()+" - "+tsc.getDescription());
    }
}
