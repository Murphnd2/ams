package net.superiorstate.ams.previous.controller.summit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.Summit;

import java.io.IOException;

@WebServlet(name = "RefreshData", value = "/RefreshData")
public class RefreshData extends HttpServlet {
    private boolean employerTable;
    private boolean employeeTable;
    private boolean benefitsTable;
    private boolean benefitTierTable;
    private boolean enrollmentTable;
    private boolean pbBenefitsTable;
    private boolean pbCoverageTable;
    private EntityManager em;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            refreshTheData(request);
            goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        refreshTheData(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/index.jsp");
        dispatcher.forward(request,response);
    }

    private void refreshTheData(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        setEm(emf.createEntityManager());
        setTableBooleans(request);
        processTableUpdateRequest(request);
        getEm().close();

    }

    private void processTableUpdateRequest(HttpServletRequest request){
        if(refreshEmployerTable())
            processEmployerRefresh();
        if(refreshEmployeeTable())
            processEmployeeRefresh();
        if(refreshBenefitsTable())
            processBenefitRefresh();
        if(refreshPbBenefitsTable())
            processPbBenefitsRefresh();
        if(refreshBenefitTierTable())
            processBenefitTierRefresh();
    }

    private void processBenefitTierRefresh(){
        Summit.createBenefitTierTablesPB(em);
    }
    private void processPbBenefitsRefresh(){
        Summit.createNewBenefitsPB(em);
    }
    private void processBenefitRefresh(){
        Summit.createNewBenefitsCDH(em);
        Summit.reactivateBenefits(em);
        Summit.closeInactiveBenefits(em);
    }

    private void processEmployeeRefresh(){
        Summit.createNewEmployees2(em);
        Summit.makeEmployeesInactive2(em);
        Summit.updateEmployeeNames2(em);
    }

    private void processEmployerRefresh(){
        //Copy new from sEmployer2 Table into sEmployerTable
        Summit.createNewEssEmployers(em);
        Summit.updateEssEmployerActiveStatus(em);

        //Copy new from sEmployer Table into Employer Table
        Summit.createNewEmployers(em);
        Summit.updateEmployerActiveStatus(em);
    }

    private void setTableBooleans(HttpServletRequest request){
        String erTable = request.getParameter("refreshEmployer");
        setEmployerTable(false);
        if(erTable!=null && erTable.equals("Y"))
            setEmployerTable(true);
        String eeTable = request.getParameter("refreshEmployee");
        setEmployeeTable(false);
        if(eeTable!=null && eeTable.equals("Y"))
            setEmployeeTable(true);
        String benTable = request.getParameter("refreshBenefits");
        setBenefitsTable(false);
        if(benTable!=null && benTable.equals("Y"))
            setBenefitsTable(true);
        String benTierTable = request.getParameter("refreshBenefitTier");
        setBenefitTierTable(false);
        if(benTierTable!=null && benTierTable.equals("Y"))
            setBenefitTierTable(true);
        String enrollTable = request.getParameter("refreshEnrollment");
        setEnrollmentTable(false);
        if(enrollTable!=null && enrollTable.equals("Y"))
            setEnrollmentTable(true);
        String pbBenTable = request.getParameter("refreshPbBenefit");
        setPbBenefitsTable(false);
        if(pbBenTable!=null && pbBenTable.equals("Y"))
            setPbBenefitsTable(true);
        String pbCovTable = request.getParameter("refreshPbCoverage");
        setPbCoverageTable(false);
        if(pbCovTable!=null && pbCovTable.equals("Y"))
            setPbCoverageTable(true);
    }

    public boolean refreshEmployerTable() {
        return employerTable;
    }

    public void setEmployerTable(boolean employerTable) {
        this.employerTable = employerTable;
    }

    public boolean refreshEmployeeTable() {
        return employeeTable;
    }

    public void setEmployeeTable(boolean employeeTable) {
        this.employeeTable = employeeTable;
    }

    public boolean refreshBenefitsTable() {
        return benefitsTable;
    }

    public void setBenefitsTable(boolean benefitsTable) {
        this.benefitsTable = benefitsTable;
    }

    public boolean refreshBenefitTierTable() {
        return benefitTierTable;
    }

    public void setBenefitTierTable(boolean benefitTierTable) {
        this.benefitTierTable = benefitTierTable;
    }

    public boolean refreshEnrollmentTable() {
        return enrollmentTable;
    }

    public void setEnrollmentTable(boolean enrollmentTable) {
        this.enrollmentTable = enrollmentTable;
    }

    public boolean refreshPbBenefitsTable() {
        return pbBenefitsTable;
    }

    public void setPbBenefitsTable(boolean pbBenefitsTable) {
        this.pbBenefitsTable = pbBenefitsTable;
    }

    public boolean refreshPbCoverageTable() {
        return pbCoverageTable;
    }

    public void setPbCoverageTable(boolean pbCoverageTable) {
        this.pbCoverageTable = pbCoverageTable;
    }

    public EntityManager getEm() {
        return em;
    }

    public void setEm(EntityManager em) {
        this.em = em;
    }
}
