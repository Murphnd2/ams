package net.superiorstate.ams.model.sales.agency;

import jakarta.persistence.*;
import net.superiorstate.ams.model.general.Address;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.general.Person;

import java.util.List;

@Entity
public class Agency implements Comparable<Agency> {
    @Id
    @GeneratedValue
    @Column(name = "agency_id")
    private Long id;

    @Column(name="agency_name",columnDefinition = "varchar(200)")
    private String name;
    @Column(name="tax_id",columnDefinition = "varchar(20)")
    private String taxId;
    @Column(name="phone",columnDefinition = "varchar(12)")
    private String phone;

    @Column(name="suppressed", nullable = false)
    private boolean suppressed;

    @Column(name="markup_enabled", nullable = false)
    private boolean markupEnabled;

    /** V068: vanity host for this agency's branded landing page (stored lowercase, unique). */
    @Column(name="landing_host", columnDefinition = "varchar(255)")
    private String landingHost;

    /** V068: sanitized custom landing-page HTML; non-blank = white-label front door enabled. */
    @Column(name="landing_html", columnDefinition = "MEDIUMTEXT")
    private String landingHtml;

    /** V069: agency's verified sending (sub)domain, e.g. admin.swbd.com (stored lowercase, unique). */
    @Column(name="email_domain", columnDefinition = "varchar(255)")
    private String emailDomain;

    /** V069: manual PSP-admin flag — true only once email_domain is Verified in SMTP2GO. Gates Tier-1 sending. */
    @Column(name="email_verified", nullable = false)
    private boolean emailVerified;

    @ManyToOne
    @JoinColumn(name="psp_id")
    private PSP psp;

    @OneToOne
    @JoinColumn(name="address_id")
    private Address address;

    @OneToOne
    @JoinColumn(name="contact_id")
    private Person primaryContact;

    @OneToOne
    @JoinColumn(name="manager_id")
    private Person manager;
    @ManyToMany
    @JoinTable(name="agencyrates",
            joinColumns = @JoinColumn(name="agency_id"),inverseJoinColumns = @JoinColumn(name="rate_id"))
    List<Rate> agencyRateList;


    @ManyToMany
    @JoinTable(name="agents",
            joinColumns = @JoinColumn(name="agency_id"),inverseJoinColumns = @JoinColumn(name="person_id"))
    List<Person> agentList;

    @ManyToOne
    @JoinColumn(name="parent_agency_id")
    private Agency parentAgency;

    @Column(name="quote_token")
    private String quoteToken;

   public Agency(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isSuppressed() {
        return suppressed;
    }

    public void setSuppressed(boolean suppressed) {
        this.suppressed = suppressed;
    }

    public boolean isMarkupEnabled() {
        return markupEnabled;
    }

    public void setMarkupEnabled(boolean markupEnabled) {
        this.markupEnabled = markupEnabled;
    }

    public String getLandingHost() {
        return landingHost;
    }

    public void setLandingHost(String landingHost) {
        this.landingHost = landingHost;
    }

    public String getLandingHtml() {
        return landingHtml;
    }

    public void setLandingHtml(String landingHtml) {
        this.landingHtml = landingHtml;
    }

    public String getEmailDomain() {
        return emailDomain;
    }

    public void setEmailDomain(String emailDomain) {
        this.emailDomain = emailDomain;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public PSP getPsp() {
        return psp;
    }

    public void setPsp(PSP psp) {
        this.psp = psp;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Person getPrimaryContact() {
        return primaryContact;
    }

    public void setPrimaryContact(Person primaryContact) {
        this.primaryContact = primaryContact;
    }

    public Person getManager() {
        return manager;
    }

    public void setManager(Person manager) {
        this.manager = manager;
    }

    public List<Rate> getAgencyRateList() {
        return agencyRateList;
    }

    public void setAgencyRateList(List<Rate> agencyRateList) {
        this.agencyRateList = agencyRateList;
    }

    public List<Person> getAgentList() {
        return agentList;
    }

    public void setAgentList(List<Person> agentList) {
        this.agentList = agentList;
    }

    public Agency getParentAgency() {
        return parentAgency;
    }

    public void setParentAgency(Agency parentAgency) {
        this.parentAgency = parentAgency;
    }

    public String getQuoteToken() {
        return quoteToken;
    }

    public void setQuoteToken(String quoteToken) {
        this.quoteToken = quoteToken;
    }

    public void addRate(Rate rate){
        this.agencyRateList.add(rate);
        rate.getListOfAgenciesWithThisRate().add(this);
    }

    public void removeRate(Rate rate){
        this.agencyRateList.remove(rate);
        rate.getListOfAgenciesWithThisRate().remove(this);
    }

    public void addAgent(Person agent){
        this.agentList.add(agent);
        agent.getListOfAgenciesWithThisAgent().add(this);
    }

    public void removeAgent(Person agent){
        this.agentList.remove(agent);
        agent.getListOfAgenciesWithThisAgent().remove(this);
    }

    @Override
        public int compareTo(Agency o) {
            if(this.name.compareTo(o.getName()) !=0){
                return this.name.compareTo(o.getName());
            } else {
                return this.id.compareTo(o.getId());
            }
        }
}
