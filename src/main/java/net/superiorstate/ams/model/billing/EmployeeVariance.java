package net.superiorstate.ams.model.billing;

import net.superiorstate.ams.model.summit.archive.Employee;

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
