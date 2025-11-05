package net.superiorstate.ams.previous.model.general;

import jakarta.persistence.*;
import net.superiorstate.ams.previous.model.summit.archive.Employee;
import software.amazon.awssdk.annotations.Immutable;

@Entity
@Immutable
@Table(name="personv")
public class PersonV {

    @Id
    @Column(name="id")
    private long id;

    @Column(name="f_name")
    private String firstName;

    @Column(name="l_name")
    private String lastName;

    @Column(name="email1")
    private String email;

    @ManyToOne
    @JoinColumn(name="employee_id")
    private Employee employee;

    public PersonV(){}

    public long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public Employee getEmployee() {
        return employee;
    }
}
