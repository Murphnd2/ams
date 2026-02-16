package net.superiorstate.ams.controller.monthly;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.V;
import net.superiorstate.ams.previous.model.summit.archive.Employee;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "CleanseEe", value = "/CleanseEe")
public class CleanseEe extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    private void cleanse(EntityManager em){
        //Find list of names that appear multiple times in the Employee Table
        TypedQuery<String> q = em.createQuery("SELECT LOWER(CONCAT(e.firstName,' ',e.lastName)) FROM Employee e GROUP BY LOWER(CONCAT(e.firstName,' ',e.lastName)) HAVING COUNT(e.id)>1 ",String.class);
        List<String> nameList = q.getResultList();
        for(String s: nameList){
            cleanName(em, s);
        }
    }

    private void cleanName(EntityManager em, String name){
        //Find list of Employees with the same name
        TypedQuery<Employee> q = em.createQuery("SELECT e FROM Employee e WHERE LOWER(CONCAT(e.firstName,' ',e.lastName)) = :name",Employee.class);
        q.setParameter("name",name);
        List<Employee> employees = q.getResultList();
        //Employee best = findBestEmployee(em,employees);
       // List<Employee> removeList = getRemovalList(em, best, employees);

    }

    private List<Employee> getRemovalList(EntityManager em, Employee best, List<Employee> employees){
        List<Employee> returnList = new ArrayList<>(employees);
        return returnList.stream().filter(obj->obj.getId()!=best.getId()).toList();
    }


    private boolean goodEmail(Employee ee){
        return ((ee.getEmail()!=null && V.isValidEmail(ee.getEmail())) || (ee.getHrEmail()!=null && V.isValidEmail(ee.getHrEmail())));
    }
}
