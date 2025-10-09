package net.superiorstate.ams.previous.data.misc;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import net.superiorstate.ams.previous.model.activity.note.Email;
import net.superiorstate.ams.previous.model.activity.note.EmailComparator;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.WebLink;
import net.superiorstate.ams.previous.model.summit.archive.Employee;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

public abstract class dbEmail {
    public static boolean isValidEmail(String email){
        String regexPattern ="^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
                + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
        return Pattern.compile(regexPattern).matcher(email).matches();
    }

    public static void sendEmail(String fromWho, String toWho, String subject, String theMessage, EntityManager em) throws MessagingException {
        List<String> toWhoList = new ArrayList<>();
        toWhoList.add(toWho);
        sendEmail(fromWho,toWhoList,subject,theMessage,em);
    }

    public static void sendEmail(String fromWho, List<String> toWhoList, String subject, String theMessage, EntityManager em) throws MessagingException {
        Message message = getMessage(em);
        System.out.println("FROM WHO:" + fromWho);
        message.setFrom(new InternetAddress(fromWho));
        String to = "";
        for(int j=0;j<toWhoList.size();j++){
            to = to + toWhoList.get(j);
            if(j!= toWhoList.size()-1)
                to = to + ", ";
        }
        InternetAddress[] parse = InternetAddress.parse(to,true);
        message.setRecipients(Message.RecipientType.TO,parse);
        message.setSubject(subject);
        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(theMessage,"text/html; charset=utf-8");
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);
        message.setContent(multipart);
        Transport.send(message);
    }

    public static Person getPersonByEmail(EntityManager em,String email, PSP psp){
        if(!isValidEmail(email))
            return null;
        Employee employee = getEmployeeByEmail(em,email);
        if(employee!=null){
            Person p = dP.getPersonByEmployee(em,employee);
            if(p==null)
                dbAuth.createPersonFromEmployee(em,employee,psp);
            return dP.getPersonByEmployee(em,getEmployeeByEmail(em,email));
        }

        Query q = em.createQuery("SELECT p FROM Person p WHERE p.email = :email");
        q.setParameter("email",email);
        List<Person> personList;
        try{
            personList = (List<Person>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            return null;
        }
        if(personList != null && personList.size()>0)
            return personList.get(0);
        return null;
    }

    public static Employee getEmployeeByEmail(EntityManager em, String email){
        if(!isValidEmail(email))
            return null;
        Query q = em.createQuery("SELECT e FROM Employee e WHERE e.email = :email");
        q.setParameter("email",email);
        List<Employee> employeeList;
        try{
            employeeList  = (List<Employee>) q.getResultList();
        } catch (NoResultException e1){
            return null;
        }
        if(employeeList.size()==0)
            return null;
        return employeeList.get(0);
    }
    public static Employee getEmployeeByEmail(EntityManager em, String email, Person p){
        if(!isValidEmail(email))
            return null;
        Query q = em.createQuery("SELECT e FROM Employee e WHERE e.email = :email");
        q.setParameter("email",email);
        List<Employee> employeeList;
        try{
            employeeList  = (List<Employee>) q.getResultList();
        } catch (NoResultException e1){
            return null;
        }
        if(employeeList.size()==0)
            return null;
        for(Employee ee:employeeList){
            if(ee.getEmployer().getId()==p.getEmployee().getEmployer().getId())
                return ee;
        }
        return null;
    }


    public static void sendEmail(Email email, EntityManager em) throws MessagingException {
        System.out.println("E:1");
        List<String> toWhoList = new ArrayList<>();
        for(Person r: email.getRecipientList()){
            toWhoList.add(r.getEmail());
        }
        String theMessage = email.getDetail();
        String theAttachments="";
        List<WebLink> webLinkList = email.getWebLinkList();
        if(webLinkList.size()>0){
            theAttachments = "<p><b><u>Attachments</u></b><br/><ul>";
            for(WebLink l: webLinkList){
                theAttachments += "<li>" + l.getExternalAnchorTag(em) + "</li>";
            }
            theAttachments += "</ul></p>";
        }
        theMessage += theAttachments;
        System.out.println("ID: "+email.getCreatedBy().getId());
        System.out.println("Email: "+email.getCreatedBy().getEmail());
        sendEmail(email.getCreatedBy().getEmail(),toWhoList,email.getSubject(),theMessage,em);
    }

    private static Message getMessage(){
        return getMessage("mail.smtp2go.com","2525","jspSmtpSender","MZSVP0ZYm3YiTK0w");
    }

    private static Message getMessage(String server, String port, String user, String password){
        Properties prop = new Properties();
        prop.put("mail.smtp.auth",true);
        prop.put("mail.smtp.starttls.enable","true");
        prop.put("mail.smtp.host",server);
        prop.put("mail.smtp.port",port);
        prop.put("mail.smtp.ssl.trust",server);
        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication(){
                return new PasswordAuthentication(user,password);
            }
        });
        return new MimeMessage(session);
    }
    public static Message getMessage(EntityManager em){
        String smtpServer = dbA.getConstantValue(em,"SMTP_SERVER");
        String smtpPort = dbA.getConstantValue(em,"SMTP_PORT");
        String smtpUserName = dbA.getConstantValue(em,"SMTP_USER");
        String smtpPassword = dbA.getConstantValue(em,"SMTP_PASSWORD");
        return  getMessage(smtpServer,smtpPort,smtpUserName,smtpPassword);
    }
    private static Message getMessage(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Message m = getMessage(em);
        em.close();
        return m;
    }
    private static List<Person> getPersonsWithEmail(EntityManager em, String emailAddress){
        Query q = em.createQuery("SELECT p FROM Person p WHERE p.email =:emailAdd");
        q.setParameter("emailAdd",emailAddress);
        List<Person> personList;
        try{
            personList=(List<Person>) q.getResultList();
        } catch (NoResultException e){
            return null;
        }
        return personList;
    }

    public static List<Email> getEmailsToRecipient(EntityManager em, String emailAddress){
        List<Person> personList = getPersonsWithEmail(em,emailAddress);
        List<Email> emailList = new ArrayList<>();
        for(Person p : personList){
            Query q = em.createQuery("SELECT p FROM Person p WHERE p.id = :id");
            q.setParameter("id",p.getId());
            Person person = (Person) q.getSingleResult();
            List<Email> emails = person.getEmailList();
            if(emails!=null && emails.size()>0){
                for(Email e: emails)
                    emailList.add(e);
            }
        }
        Collections.sort(emailList,new EmailComparator());
        return emailList;
    }
}
