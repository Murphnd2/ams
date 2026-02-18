package net.superiorstate.ams.model.activity.ticket;

public class tEmployee {
    private int id;
    private String lastName;
    private String firstName;
    private String employer;
    private int employerId;
    private Long personId;
    private String email;
    private String phone;
    private boolean hasEmail;
    private boolean hasPhone;

    private boolean isPerson;

    public tEmployee(){}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getEmployer() {
        return employer;
    }

    public String getEr(){
        if(employer.length()>8)
            return employer.substring(0,8) + ".." ;
        else return employer;
    }

    public void setEmployer(String employer) {
        this.employer = employer;
    }

    public int getEmployerId() {
        return employerId;
    }

    public void setEmployerId(int employerId) {
        this.employerId = employerId;
    }

    public Long getPersonId() {
        return personId;
    }

    public void setPersonId(Long personId) {
        this.personId = personId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean getHasEmail() {
        return hasEmail;
    }

    public void setHasEmail(boolean hasEmail) {
        this.hasEmail = hasEmail;
    }

    public boolean getHasPhone() {
        return hasPhone;
    }

    public void setHasPhone(boolean hasPhone) {
        this.hasPhone = hasPhone;
    }

    public boolean isPerson() {
        return isPerson;
    }

    public void setIsPerson(boolean person) {
        isPerson = person;
    }

    public String getFullName(){
        return getFirstName() + " " + getLastName();
    }

    public String getDropDownString(){
        String ddString = getLastName() +", "+ getFirstName() + " [" + getErShort() +"]";
        if(hasEmail)
            ddString += " [E]";
        if(hasPhone)
            ddString += " [P]";
        return ddString;
    }

    private String getErShort(){
        if(getEmployer().length()>20){
            return getEmployer().substring(0,19) + "...";
        } else return getEmployer();
    }
}
