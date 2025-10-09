package net.superiorstate.ams.model;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TemplatePurpose;
import net.superiorstate.ams.previous.model.activity.ticket.TicketSubCategory;

public class ReqTaskListTix {
    long id;
    String description;
    RequiredTaskList requiredTaskList;
    TemplatePurpose templatePurpose;
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

    public TemplatePurpose getTemplatePurpose() {
        return templatePurpose;
    }

    public void setTemplatePurpose(TemplatePurpose templatePurpose) {
        this.templatePurpose = templatePurpose;
    }

    public TicketSubCategory getTicketSubCategory() {
        return ticketSubCategory;
    }

    public void setTicketSubCategory(TicketSubCategory ticketSubCategory) {
        this.ticketSubCategory = ticketSubCategory;
    }

    public void fillRequiredTask(EntityManager em,long id){
        setRequiredTaskList(dM.getReqListById(em,id));
        setTemplatePurpose(dM.getTemplatePurposeById(em,getRequiredTaskList().getTemplatePurpose().getId()));
        Query q = em.createQuery("SELECT tsc FROM TicketSubCategory tsc WHERE tsc.templatePurpose.id = :id");
        q.setParameter("id",getTemplatePurpose().getId());
        TicketSubCategory tsc;
        try{
            tsc = (TicketSubCategory) q.getSingleResult();
        } catch (NoResultException e){
            tsc = null;
        }
        setTicketSubCategory(tsc);
        setId(getRequiredTaskList().getId());
        assert tsc != null;
        setDescription(tsc.getTicketCategory().getShortText()+" - "+tsc.getDescription());
    }
}
