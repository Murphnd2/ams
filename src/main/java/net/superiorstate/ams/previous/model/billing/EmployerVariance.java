package net.superiorstate.ams.previous.model.billing;

import net.superiorstate.ams.previous.model.summit.archive.Employer;

public class EmployerVariance {
    private Employer employer;
    private BillingMonth currentMonth;
    private BillingMonth lastMonth;
    private int cobraCurrent;
    private int cobraLast;
    private int fsaCurrent;
    private int fsaLast;
    private int hraCurrent;
    private int hraLast;
    private int hsaCurrent;
    private int hsaLast;
    private int tranCurrent;
    private int tranLast;
    private int dualCurrent;
    private int dualLast;
    private int directCurrent;
    private int directLast;
    private int retireeCurrent;
    private int retireeLast;
    private int lsaCurrent;
    private int lsaLast;

    public EmployerVariance(){}

    public Employer getEmployer() {
        return employer;
    }

    public void setEmployer(Employer employer) {
        this.employer = employer;
    }

    public BillingMonth getCurrentMonth() {
        return currentMonth;
    }

    public void setCurrentMonth(BillingMonth currentMonth) {
        this.currentMonth = currentMonth;
    }

    public BillingMonth getLastMonth() {
        return lastMonth;
    }

    public void setLastMonth(BillingMonth lastMonth) {
        this.lastMonth = lastMonth;
    }

    public int getCobraCurrent() {
        return cobraCurrent;
    }

    public void setCobraCurrent(int cobraCurrent) {
        this.cobraCurrent = cobraCurrent;
    }

    public int getCobraLast() {
        return cobraLast;
    }

    public void setCobraLast(int cobraLast) {
        this.cobraLast = cobraLast;
    }

    public int getFsaCurrent() {
        return fsaCurrent;
    }

    public void setFsaCurrent(int fsaCurrent) {
        this.fsaCurrent = fsaCurrent;
    }

    public int getFsaLast() {
        return fsaLast;
    }

    public void setFsaLast(int fsaLast) {
        this.fsaLast = fsaLast;
    }

    public int getHraCurrent() {
        return hraCurrent;
    }

    public void setHraCurrent(int hraCurrent) {
        this.hraCurrent = hraCurrent;
    }

    public int getHraLast() {
        return hraLast;
    }

    public void setHraLast(int hraLast) {
        this.hraLast = hraLast;
    }

    public int getHsaCurrent() {
        return hsaCurrent;
    }

    public void setHsaCurrent(int hsaCurrent) {
        this.hsaCurrent = hsaCurrent;
    }

    public int getHsaLast() {
        return hsaLast;
    }

    public void setHsaLast(int hsaLast) {
        this.hsaLast = hsaLast;
    }

    public int getTranCurrent() {
        return tranCurrent;
    }

    public void setTranCurrent(int tranCurrent) {
        this.tranCurrent = tranCurrent;
    }

    public int getTranLast() {
        return tranLast;
    }

    public void setTranLast(int tranLast) {
        this.tranLast = tranLast;
    }

    public int getDualCurrent() {
        return dualCurrent;
    }

    public void setDualCurrent(int dualCurrent) {
        this.dualCurrent = dualCurrent;
    }

    public int getDualLast() {
        return dualLast;
    }

    public void setDualLast(int dualLast) {
        this.dualLast = dualLast;
    }

    public int getDirectCurrent() {
        return directCurrent;
    }

    public void setDirectCurrent(int directCurrent) {
        this.directCurrent = directCurrent;
    }

    public int getDirectLast() {
        return directLast;
    }

    public void setDirectLast(int directLast) {
        this.directLast = directLast;
    }

    public int getRetireeCurrent() {
        return retireeCurrent;
    }

    public void setRetireeCurrent(int retireeCurrent) {
        this.retireeCurrent = retireeCurrent;
    }

    public int getRetireeLast() {
        return retireeLast;
    }

    public void setRetireeLast(int retireeLast) {
        this.retireeLast = retireeLast;
    }

    public int getLsaCurrent() {
        return lsaCurrent;
    }

    public void setLsaCurrent(int lsaCurrent) {
        this.lsaCurrent = lsaCurrent;
    }

    public int getLsaLast() {
        return lsaLast;
    }

    public void setLsaLast(int lsaLast) {
        this.lsaLast = lsaLast;
    }

    public int getCobraNet(){
        return this.cobraCurrent - this.cobraLast;
    }

    public int getFsaNet(){
        return  this.fsaCurrent - this.fsaLast;
    }

    public int getHraNet(){
        return this.hraCurrent - this.hraLast;
    }

    public int getHsaNet(){
        return this.hsaCurrent - this.hsaLast;
    }

    public int getTransitNet(){
        return this.tranCurrent - this.tranLast;
    }

    public int getDualNet(){
        return this.dualCurrent - this.dualLast;
    }

    public int getRetireeNet(){
        return this.retireeCurrent - this.retireeLast;
    }

    public int getDirectNet(){
        return this.directCurrent - this.directLast;
    }

    public int getLsaNet(){
        return this.lsaCurrent - this.lsaLast;
    }
}
