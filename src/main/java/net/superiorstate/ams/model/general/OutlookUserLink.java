package net.superiorstate.ams.model.general;

import jakarta.persistence.*;

import java.sql.Timestamp;

/**
 * Cross-reference linking an AMS Person record to a Microsoft 365 identity.
 * Used by the "Log to AMS" Outlook Web Add-in for token-based authentication.
 *
 * Each row carries a per-user API token that the add-in stores in localStorage
 * and sends on every request as `Authorization: Bearer {token}`. This avoids
 * session cookies entirely and lets the taskpane authenticate automatically
 * once the link is established.
 *
 * Created: V060 migration.
 */
@Entity
@Table(name = "outlook_user_link")
public class OutlookUserLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(name = "m365_email", nullable = false, unique = true, length = 255)
    private String m365Email;

    @Column(name = "api_token", nullable = false, unique = true, length = 64)
    private String apiToken;

    @Column(name = "created_date", updatable = false, insertable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp createdDate;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public OutlookUserLink() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public String getM365Email() {
        return m365Email;
    }

    public void setM365Email(String m365Email) {
        this.m365Email = m365Email;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
