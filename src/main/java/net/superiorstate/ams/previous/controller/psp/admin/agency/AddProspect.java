package net.superiorstate.ams.previous.controller.psp.admin.agency;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dG;
import net.superiorstate.ams.previous.data.misc.dbAuth;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.general.*;
import net.superiorstate.ams.previous.model.sales.agency.Agency;
import net.superiorstate.ams.previous.model.sales.agency.Prospect;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "AddProspect", value = "/AddProspect")
public class AddProspect extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addProspect(request,response);
        goToAdminHomePage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addProspect(request,response);
        goToAdminHomePage(request,response);
    }
    private void goToAdminHomePage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException{
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("PspAgencyHome");
        dispatcher.forward(request,response);
    }
    private void addProspect(HttpServletRequest request,HttpServletResponse response){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        Address address = new Address();
        address.setAddress1(request.getParameter("address1"));
        address.setAddress2(request.getParameter("address2"));
        address.setCity(request.getParameter("city"));
        address.setState(request.getParameter("stateList"));
        address.setZipCode(request.getParameter("zipCode"));
        em.getTransaction().begin();
        em.persist(address);
        em.getTransaction().commit();

        Person contact = new Person();
        PSP psp = (PSP) request.getSession().getAttribute("psp");
        contact.setPsp(psp);
        contact.setFirstName(request.getParameter("firstName"));
        contact.setLastName(request.getParameter("lastName"));
        contact.setMiddleInit("");
        contact.setEmail(request.getParameter("email"));
        String pPhone = request.getParameter("personPhone");
        if(pPhone!=null && !pPhone.isEmpty() && !pPhone.trim().isEmpty())
            contact.setPhone(pPhone);
        String pTitle = request.getParameter("title");
        if(pTitle!=null && !pTitle.isEmpty() && !pTitle.trim().isEmpty())
            contact.setTitle(pTitle);
        contact.setAddress(address);
        em.getTransaction().begin();
        em.persist(contact);
        em.getTransaction().commit();

        Prospect prospect = new Prospect();
        prospect.setAddress(address);
        prospect.setContact(contact);
        prospect.setAgent(getAgent(em,request));
        prospect.setName(request.getParameter("prospectName"));

        em.getTransaction().begin();
        em.persist(prospect);
        em.getTransaction().commit();

        request.getSession().setAttribute("hasCurrentProspect",true);
        request.getSession().setAttribute("currentProspect",prospect);

        em.close();
    }

    private Person getAgent(EntityManager em, HttpServletRequest request){
        boolean hasCurrentAgent = (boolean) request.getSession().getAttribute("hasCurrentAgent");
        Person agent = new Person();
        Person currentAgent = new Person();
        if(hasCurrentAgent){
            currentAgent = (Person) request.getSession().getAttribute("currentAgent");
        } else {
            Agency currentAgency = (Agency) request.getSession().getAttribute("currentAgency");
            List<Person> agencyAgentList = (List<Person>) request.getSession().getAttribute("agencyAgentList");
            if(agencyAgentList.size()<1){
                currentAgent = getGenericAgent(em,request,currentAgency);
            } else{
                boolean hasGenericAgent = false;
                Person genericAgent = new Person();
                for(int i = 0; i < agencyAgentList.size(); i++){
                    if(agencyAgentList.get(i).getFirstName()=="Generic"){
                        hasGenericAgent = true;
                        genericAgent = agencyAgentList.get(i);
                        break;
                    }
                }
                if(hasGenericAgent){
                    currentAgent = genericAgent;
                } else {
                    currentAgent = getGenericAgent(em,request,currentAgency);
                }
            }
        }
        agent = dM.getPersonById(em,currentAgent.getId());
        request.getSession().setAttribute("hasCurrentAgent",true);
        request.getSession().setAttribute("currentAgent",agent);
        return agent;
    }
    private Person getGenericAgent(EntityManager em, HttpServletRequest request, Agency currentAgency){
        Person agent = new Person();
        Agency agency = dG.getAgencyFull(em,currentAgency.getId());
        agent.setPsp(agency.getPsp());
        agent.setEmail(agency.getPrimaryContact().getEmail());
        agent.setAddress(agency.getAddress());
        agent.setPhone(agency.getPhone());
        agent.setTitle(agency.getPrimaryContact().getTitle());
        agent.setLastName("Agency " + agency.getId());
        agent.setFirstName("Generic");
        agent.setMiddleInit("");

        em.getTransaction().begin();
        em.persist(agent);
        em.getTransaction().commit();

        em.getTransaction().begin();
        agency.addAgent(agent);
        em.persist(agency);
        UserRole agentRole = dG.getUserRoleById(em,2);
        User u = dbAuth.getUserFromPerson(em,agent);
        u.addUserToRole(agentRole);
        em.persist(agent);
        em.persist(u);
        em.getTransaction().commit();

        return agent;
    }
}
