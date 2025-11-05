package net.superiorstate.ams.previous.data.service;
import jakarta.mail.*;
import java.util.Properties;

public class ReadInboundEmailService {
    public static final String IMAP_HOST = "outlook.office365.com";

    public void check(String user, String password){
        Session session = this.getImapSession();
        try{
            Store store = session.getStore("imap");
            store.connect(IMAP_HOST, 993, user, password);
            Folder emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);
            Message[] messages = emailFolder.getMessages();
            for (int i = 0; i < messages.length; i++) {
                Message msg = messages[i];
                Address[] fromAddress = msg.getFrom();
                String from = fromAddress[0].toString();
                String subject = msg.getSubject();
                Address[] toList = msg.getRecipients(Message.RecipientType.TO);
                Address[] ccList = msg.getRecipients(Message.RecipientType.CC);
                String contentType = msg.getContentType();
                System.out.println("------------------------------------------------");
                System.out.println("FROM: "+ from);
                System.out.println("SUBJECT: " + subject);
            }
            emailFolder.close(false);
            store.close();
        } catch (Exception e){
            e.printStackTrace();
        }

    }
    private Session getImapSession(){
        Properties props = new Properties();
        props.setProperty("mail.store.protocol","imap");
        props.setProperty("mail.debug", "true");
        props.setProperty("mail.imap.host","outlook.office365.com");
        props.setProperty("mail.imap.port", "993");
        props.put("mail.imap.ssl.enable", "true"); // required for Gmail
        props.put("mail.imap.auth.mechanisms", "XOAUTH2");
        Session session = Session.getDefaultInstance(props, null);
        session.setDebug(true);
        return session;
    }
}
