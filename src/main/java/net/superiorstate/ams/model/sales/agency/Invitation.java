package net.superiorstate.ams.model.sales.agency;

import jakarta.persistence.*;
import net.superiorstate.ams.model.general.Person;

import java.sql.Timestamp;

@Entity
public class Invitation {

    @Id
    @GeneratedValue
    @Column(name = "invitation_id")
    private Long id;

    @Column(name = "guid", columnDefinition = "varchar(36)", nullable = false, unique = true)
    private String guid;

    @Column(name = "email", columnDefinition = "varchar(200)", nullable = false)
    private String email;

    @Column(name = "first_name", columnDefinition = "varchar(100)")
    private String firstName;

    @Column(name = "last_name", columnDefinition = "varchar(100)")
    private String lastName;

    @ManyToOne
    @JoinColumn(name = "person_id")
    private Person person;
    @ManyToOne
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    @Column(name = "role", columnDefinition = "varchar(20)", nullable = false)
    private String role;

    @ManyToOne
    @JoinColumn(name = "invited_by", nullable = false)
    private Person invitedBy;

    @Column(name = "date_created", nullable = false, updatable = false, insertable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp dateCreated;

    @Column(name = "date_expires", nullable = false)
    private Timestamp dateExpires;

    @Column(name = "date_accepted")
    private Timestamp dateAccepted;

    @Column(name = "is_used")
    private boolean isUsed;

    public Invitation() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGuid() { return guid; }
    public void setGuid(String guid) { this.guid = guid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public Person getPerson() { return person; }
    public void setPerson(Person person) { this.person = person; }

    public Agency getAgency() { return agency; }
    public void setAgency(Agency agency) { this.agency = agency; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Person getInvitedBy() { return invitedBy; }
    public void setInvitedBy(Person invitedBy) { this.invitedBy = invitedBy; }

    public Timestamp getDateCreated() { return dateCreated; }
    public void setDateCreated(Timestamp dateCreated) { this.dateCreated = dateCreated; }

    public Timestamp getDateExpires() { return dateExpires; }
    public void setDateExpires(Timestamp dateExpires) { this.dateExpires = dateExpires; }

    public Timestamp getDateAccepted() { return dateAccepted; }
    public void setDateAccepted(Timestamp dateAccepted) { this.dateAccepted = dateAccepted; }

    public boolean isUsed() { return isUsed; }
    public void setIsUsed(boolean used) { this.isUsed = used; }
}
