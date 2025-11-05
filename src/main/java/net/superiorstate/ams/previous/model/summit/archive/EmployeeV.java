package net.superiorstate.ams.previous.model.summit.archive;

import jakarta.persistence.*;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import software.amazon.awssdk.annotations.Immutable;

@Entity
@Immutable
@Table(name="employeev")
public class EmployeeV {
    @Id
    @Column(name="employee_id")
    private int id;

    @ManyToOne
    @JoinColumn(name="employer_id")
    private Employer employer;

    @Column(name="f_name")
    private String firstName;

    @Column(name="l_name")
    private String lastName;

    @Column(name="email1")
    private String emailSummit;

    @Column(name="email2")
    private String emailSystem;

    public EmployeeV(){}

    public int getId() {
        return id;
    }

    public Employer getEmployer() {
        return employer;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmailSummit() {
        return emailSummit;
    }

    public String getEmailSystem() {
        return emailSystem;
    }

    public String getEmail(){
        if(getEmailSystem()!=null && dbEmail.isValidEmail(getEmailSystem()))
            return getEmailSystem();
        else if(getEmailSummit()!=null && dbEmail.isValidEmail(getEmailSummit()))
            return getEmailSummit();
        return null;
    }
}
