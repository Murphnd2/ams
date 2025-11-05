package net.superiorstate.ams.previous.model.billing;

import net.superiorstate.ams.previous.model.summit.archive.Employee;

public class EmployeeVariance extends EmployerVariance {
    private Employee employee;
    public EmployeeVariance(){}

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }
}
