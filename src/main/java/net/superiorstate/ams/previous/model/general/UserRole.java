package net.superiorstate.ams.previous.model.general;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import java.util.List;

@Entity
public class UserRole {
    @Id
    @Column(name="role_id")
    private int id;

    @Column(name="description",columnDefinition = "varchar(100)")
    private String description;

    @ManyToMany(mappedBy = "userRoleList")
    List<User> userList;

    public UserRole(){}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<User> getUserList() {
        return userList;
    }

    public void setUserList(List<User> userList) {
        this.userList = userList;
    }
}
