package net.superiorstate.ams.data.dao;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.model.activity.note.Email;
import net.superiorstate.ams.model.activity.note.EmailComparator;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.WebLink;
import net.superiorstate.ams.model.summit.archive.Employee;

import java.util.*;
import java.util.regex.Pattern;

public abstract class EmailDAO {

    /* ----------------------------- validation helpers ----------------------------- */

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        String regexPattern = "^(?=.{1,64}@)[A-Za-z0-9._%+-]+(\\.[A-Za-z0-9._%+-]+)*@"
                + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
        return Pattern.compile(regexPattern).matcher(email).matches();
    }

    private static InternetAddress[] toAddresses(Collection<String> emails) throws AddressException {
        if (emails == null || emails.isEmpty()) return new InternetAddress[0];
        List<String> cleaned = new ArrayList<>();
        for (String e : emails) {
            if (e == null) continue;
            String t = e.trim();
            if (t.isEmpty()) continue;
            if (!isValidEmail(t)) continue;
            cleaned.add(t);
        }
        if (cleaned.isEmpty()) return new InternetAddress[0];
        return InternetAddress.parse(String.join(", ", cleaned), true);
    }

    /* ----------------------------- public API (simple) ----------------------------- */

    public static void sendEmail(String fromWho, String toWho, String subject, String theMessage, EntityManager em)
            throws MessagingException {
        List<String> to = new ArrayList<>();
        if (toWho != null) to.add(toWho);
        sendEmail(fromWho, to, Collections.emptyList(), Collections.emptyList(), subject, theMessage, em);
    }

    public static void sendEmail(String fromWho, List<String> toWhoList, String subject, String theMessage, EntityManager em)
            throws MessagingException {
        sendEmail(fromWho, toWhoList, Collections.emptyList(), Collections.emptyList(), subject, theMessage, em);
    }

    /** Fully-specified variant (TO/CC/BCC). */
    public static void sendEmail(String fromWho,
                                 List<String> toWhoList,
                                 List<String> ccList,
                                 List<String> bccList,
                                 String subject,
                                 String htmlBody,
                                 EntityManager em) throws MessagingException {

        Session session = getSession(em);
        // Optional debug toggle via DB: SMTP_DEBUG = TRUE/FALSE
        try {
            String dbg = AppConstantDAO.getConstantValue(em, "SMTP_DEBUG");
            if (dbg != null && dbg.equalsIgnoreCase("true")) session.setDebug(true);
        } catch (Exception ignore) {}

        // FROM header (displayed) must be a verified sender in SMTP2GO
        String smtpFrom = safe(AppConstantDAO.getConstantValue(em, "SMTP_FROM"));
        if (smtpFrom.isBlank()) {
            // Try user email first
            if(isValidEmail(fromWho))
                smtpFrom = safe(fromWho);
            // fallback: SMTP_USER is often an email at SMTP2GO
            else
                smtpFrom = safe(AppConstantDAO.getConstantValue(em, "SMTP_USER"));
        }
        if (!isValidEmail(smtpFrom)) {
            throw new MessagingException("SMTP_FROM is not a valid/verified email address.");
        }

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(smtpFrom));

        // Reply-To: send replies to the human caller if provided
        if (isValidEmail(fromWho)) {
            msg.setReplyTo(new Address[]{new InternetAddress(fromWho.trim())});
        }

        // Recipients
        InternetAddress[] to = toAddresses(toWhoList);
        InternetAddress[] cc = toAddresses(ccList);
        InternetAddress[] bcc = toAddresses(bccList);

        if (to.length == 0 && cc.length == 0 && bcc.length == 0) {
            throw new MessagingException("No valid recipients (To/Cc/Bcc).");
        }
        if (to.length > 0)   msg.setRecipients(Message.RecipientType.TO, to);
        if (cc.length > 0)   msg.setRecipients(Message.RecipientType.CC, cc);
        if (bcc.length > 0)  msg.setRecipients(Message.RecipientType.BCC, bcc);

        msg.setSubject(safe(subject));

        // HTML body
        MimeBodyPart html = new MimeBodyPart();
        html.setContent(safe(htmlBody), "text/html; charset=utf-8");
        Multipart mp = new MimeMultipart();
        mp.addBodyPart(html);
        msg.setContent(mp);

        // Envelope sender (return-path) – important for SMTP2GO acceptance
        session.getProperties().put("mail.smtp.from", smtpFrom);

        // Send using explicit Transport (honors timeouts & envelope)
        Transport transport = null;
        try {
            transport = session.getTransport("smtp");
            transport.connect(); // Authenticator in Session supplies creds
            transport.sendMessage(msg, msg.getAllRecipients());
        } finally {
            if (transport != null) try { transport.close(); } catch (MessagingException ignore) {}
        }
    }

    /* ----------------------------- public API (Email model) ----------------------------- */

    public static void sendEmail(Email email, EntityManager em) throws MessagingException {
        System.out.println("E:1");

        // Build TO from the model
        List<String> toWhoList = new ArrayList<>();
        if (email.getRecipientList() != null) {
            for (Person r : email.getRecipientList()) {
                if (r != null && isValidEmail(r.getEmail())) {
                    toWhoList.add(r.getEmail().trim());
                }
            }
        }

        // Body + attachment links
        String theMessage = safe(email.getDetail());
        List<WebLink> webLinkList = email.getWebLinkList();
        if (webLinkList != null && !webLinkList.isEmpty()) {
            StringBuilder atts = new StringBuilder("<p><b><u>Attachments</u></b><br/><ul>");
            for (WebLink l : webLinkList) {
                atts.append("<li>").append(l.getExternalAnchorTag(em)).append("</li>");
            }
            atts.append("</ul></p>");
            theMessage += atts;
        }

        String subject = safe(email.getSubject());

        // FromWho = the human (for Reply-To); real From header will be SMTP_FROM
        String fromWho = (email.getCreatedBy() != null) ? safe(email.getCreatedBy().getEmail()) : "";

        // Optional: wire CC/BCC here if your flow provides it (pass empty for now)
        sendEmail(fromWho, toWhoList, Collections.emptyList(), Collections.emptyList(), subject, theMessage, em);
    }

    /* ----------------------------- Session / settings ----------------------------- */

    private static Session getSession(EntityManager em) throws MessagingException {
        String server   = safe(AppConstantDAO.getConstantValue(em, "SMTP_SERVER"));   // e.g., mail.smtp2go.com
        String port     = safe(AppConstantDAO.getConstantValue(em, "SMTP_PORT"));     // e.g., 587 or 2525
        String user     = safe(AppConstantDAO.getConstantValue(em, "SMTP_USER"));
        String password = safe(AppConstantDAO.getConstantValue(em, "SMTP_PASSWORD"));

        if (server.isBlank() || port.isBlank() || user.isBlank() || password.isBlank()) {
            throw new MessagingException("SMTP settings incomplete (need SMTP_SERVER, SMTP_PORT, SMTP_USER, SMTP_PASSWORD).");
        }

        Properties prop = new Properties();
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.starttls.required", "true");

        prop.put("mail.smtp.host", server);
        prop.put("mail.smtp.port", port);

        // Timeouts – prevent servlet hangs
        prop.put("mail.smtp.connectiontimeout", "10000"); // 10s
        prop.put("mail.smtp.timeout",           "20000"); // 20s
        prop.put("mail.smtp.writetimeout",      "20000"); // 20s

        // Trust server cert by name; SMTP2GO uses valid certs for its hostnames
        prop.put("mail.smtp.ssl.trust", server);

        // Be lenient on address parsing (display names, etc.)
        prop.put("mail.mime.address.strict", "false");

        return Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });
    }

    private static String safe(String s) { return s == null ? "" : s; }

    /* ----------------------------- legacy helpers (unchanged) ----------------------------- */

    public static Person getPersonByEmail(EntityManager em, String email, PSP psp) {
        if (!isValidEmail(email)) return null;
        Employee employee = getEmployeeByEmail(em, email);
        if (employee != null) {
            Person p = PersonDAO.getPersonByEmployee(em, employee);
            if (p == null) AuthDAO.createPersonFromEmployee(em, employee, psp);
            return PersonDAO.getPersonByEmployee(em, getEmployeeByEmail(em, email));
        }
        Query q = em.createQuery("SELECT p FROM Person p WHERE p.email = :email");
        q.setParameter("email", email);
        List<Person> personList;
        try {
            personList = (List<Person>) q.getResultList();
        } catch (NoResultException e) {
            e.printStackTrace();
            return null;
        }
        if (personList != null && !personList.isEmpty()) return personList.get(0);
        return null;
    }

    public static Employee getEmployeeByEmail(EntityManager em, String email) {
        if (!isValidEmail(email)) return null;
        Query q = em.createQuery("SELECT e FROM Employee e WHERE e.email = :email");
        q.setParameter("email", email);
        List<Employee> employeeList;
        try {
            employeeList = (List<Employee>) q.getResultList();
        } catch (NoResultException e1) {
            return null;
        }
        if (employeeList.isEmpty()) return null;
        return employeeList.get(0);
    }

    public static Employee getEmployeeByEmail(EntityManager em, String email, Person p) {
        if (!isValidEmail(email)) return null;
        Query q = em.createQuery("SELECT e FROM Employee e WHERE e.email = :email");
        q.setParameter("email", email);
        List<Employee> employeeList;
        try {
            employeeList = (List<Employee>) q.getResultList();
        } catch (NoResultException e1) {
            return null;
        }
        if (employeeList.isEmpty()) return null;
        for (Employee ee : employeeList) {
            if (ee.getEmployer().getId() == p.getEmployee().getEmployer().getId())
                return ee;
        }
        return null;
    }

    public static List<Email> getEmailsToRecipient(EntityManager em, String emailAddress) {
        List<Person> personList = getPersonsWithEmail(em, emailAddress);
        List<Email> emailList = new ArrayList<>();
        for (Person p : personList) {
            Query q = em.createQuery("SELECT p FROM Person p WHERE p.id = :id");
            q.setParameter("id", p.getId());
            Person person = (Person) q.getSingleResult();
            List<Email> emails = person.getEmailList();
            if (emails != null && !emails.isEmpty()) {
                emailList.addAll(emails);
            }
        }
        emailList.sort(new EmailComparator());
        return emailList;
    }

    private static List<Person> getPersonsWithEmail(EntityManager em, String emailAddress) {
        Query q = em.createQuery("SELECT p FROM Person p WHERE p.email =:emailAdd");
        q.setParameter("emailAdd", emailAddress);
        List<Person> personList;
        try {
            personList = (List<Person>) q.getResultList();
        } catch (NoResultException e) {
            return Collections.emptyList();
        }
        return personList;
    }

    /* ----------------------------- removed on purpose ----------------------------- */
    // private static Message getMessage() { ... }  // (hardcoded creds) => deleted to prevent accidental use
    // private static Message getMessage(HttpServletRequest request) { ... } // unused; remove to avoid servlet coupling
}

