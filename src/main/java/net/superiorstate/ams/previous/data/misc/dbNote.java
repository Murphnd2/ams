package net.superiorstate.ams.previous.data.misc;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.note.Note;

import java.util.ArrayList;
import java.util.List;

public abstract class dbNote {


    public static List<Note> getActivityHistory(EntityManager em, Activity a){
        Query q = em.createQuery("SELECT n FROM Note n WHERE n.activity.id = :id  order by n.dateCreated desc ");
        q.setParameter("id",a.getId());
        List<Note> noteList;
        try{
            noteList = (List<Note>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            noteList = new ArrayList<>();
        }
        return noteList;
    }
    public static Note getNoteById(EntityManager em, Long id){
        Query q = em.createQuery("SELECT n FROM Note n WHERE n.id = :id");
        q.setParameter("id",id);
        Note n;
        try{
            n = (Note) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            return null;
        }
        return n;
    }
}
