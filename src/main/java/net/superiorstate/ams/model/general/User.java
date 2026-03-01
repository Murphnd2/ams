package net.superiorstate.ams.model.general;

import jakarta.persistence.*;

import java.util.Date;
import java.util.List;

@Entity
public class User {

    @Column(name="user_name", unique = true)
    private String userName;

    @Column(unique = true,name="email")
    private String email;

    @Column(name="verified_email")
    private boolean emailVerified;

    @Column(name="password_hash")
    private String passwordHash;

    @Column(name="salt")
    private String salt;

    @Id
    @OneToOne
    @JoinColumn(name="person_id")
    private Person person;

    @Column(name="temp_guid")
    private String tempGuid;

    @Column(name="guid_expires")
    private Date guidExpiration;

    @Column(name="guid_used")
    private boolean guidUsed;

    @Column(name="allow_set_password")
    private boolean allowSetPassword;

    @Column(name="is_active")
    private boolean isActive = true;

    @ManyToMany
    @JoinTable(name="userinroles",
            joinColumns = @JoinColumn(name="person_id"),inverseJoinColumns = @JoinColumn(name="role_id"))
    List<UserRole> userRoleList;

    public User(){}

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public String getTempGuid() {
        return tempGuid;
    }

    public void setTempGuid(String tempGuid) {
        this.tempGuid = tempGuid;
    }

    public Date getGuidExpiration() {
        return guidExpiration;
    }

    public void setGuidExpiration(Date guidExpiration) {
        this.guidExpiration = guidExpiration;
    }

    public boolean isGuidUsed() {
        return guidUsed;
    }

    public void setGuidUsed(boolean guidUsed) {
        this.guidUsed = guidUsed;
    }

    public boolean isAllowSetPassword() {
        return allowSetPassword;
    }

    public void setAllowSetPassword(boolean allowSetPassword) {
        this.allowSetPassword = allowSetPassword;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public List<UserRole> getUserRoleList() {
        return userRoleList;
    }

    public void setUserRoleList(List<UserRole> userRoleList) {
        this.userRoleList = userRoleList;
    }

    public void addUserToRole(UserRole ur){
        this.getUserRoleList().add(ur);
        ur.getUserList().add(this);
    }

    public void removeUserFromRole(UserRole ur){
        this.getUserRoleList().remove(ur);
        ur.getUserList().remove(this);
    }
}
