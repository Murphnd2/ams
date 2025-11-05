package net.superiorstate.ams.previous.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.sales.application.Application;
import net.superiorstate.ams.previous.model.sales.application.ApplicationData;
import net.superiorstate.ams.previous.model.sales.application.DataKey;
import net.superiorstate.ams.previous.model.sales.application.DataPair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

@WebServlet(name = "ImportSetup", value = "/ImportSetup")
public class ImportSetup extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    private void importCsv(HttpServletRequest request) throws IOException {

        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Application a = (Application) request.getSession().getAttribute("currentApplication");

        String csvFilePath = "Here.csv";
        BufferedReader lineReader = new BufferedReader(new FileReader(csvFilePath));

        //Header Line
        String headerText = lineReader.readLine();
        String dataText = lineReader.readLine();
        String[] keyData = headerText.split(",");
        String[] valueData = dataText.split(",");
        for(int i = 0; i < keyData.length; i++){
            if(getDataKeyByName(em,keyData[i])==null){
                em.getTransaction().begin();
                DataKey dataKey = new DataKey();
                dataKey.setKeyName(keyData[i]);
                em.persist(dataKey);
                em.getTransaction().commit();
            }
            em.getTransaction().begin();
            DataPair dp = new DataPair();
            dp.setDataKeyAssociation(getDataKeyByName(em,keyData[i]));
            dp.setDataValue(valueData[i]);
            em.persist(dp);
            em.getTransaction().commit();

            em.getTransaction().begin();
            ApplicationData ad = new ApplicationData();
            Application application = dM.getApplicationById(em, a.getSetup().getId());
            ad.setApplication(application);
            ad.setDataPair(dp);
            em.persist(ad);
            em.getTransaction().commit();
        }
    }


    private DataKey getDataKeyByName(EntityManager em, String name){
        Query q = em.createQuery("SELECT dk FROM DataKey dk WHERE dk.keyName = :name");
        q.setParameter("name",name);
        DataKey dataKey;
        try{
            dataKey = (DataKey) q.getSingleResult();
        }catch (NoResultException e){
            e.printStackTrace();
            dataKey = null;
        }
        return dataKey;
    }
}
