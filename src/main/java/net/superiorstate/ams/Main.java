package net.superiorstate.ams;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class Main {
    public static void main(String[] args) {

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("default");
        EntityManager em = emf.createEntityManager();
        /*PSP psp = new PSP();
        psp.setFullName("TEST THIS");
        psp.setTaxId("12-3456789");
        em.getTransaction().begin();
        em.persist(psp);
        em.getTransaction().commit();


        Person person = new Person();
        person.setFirstName("Kevin");
        person.setLastName("Murphy");
        person.setPsp(psp);
        person.setTitle("President");
        em.getTransaction().begin();
        em.persist(person);
        em.getTransaction().commit();

        psp.setContact(person);
        em.getTransaction().begin();
        em.persist(psp);
        em.getTransaction().commit();*/

        em.close();
        //StarterData.fillStarterData();
    }


}
