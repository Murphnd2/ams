package net.superiorstate.ams.model.market;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One row per employee on an employer's census, loaded at Setup time from a spreadsheet the
 * employer supplies. This is the AMS-owned participant roster (V094) — the source for Summit
 * files 4 (Demographics) and 5 (HRA Enrollment).
 * <p>
 * Keyed to {@code prospect}, not {@code employer}: nothing in the codebase links a Prospect to
 * an Employer, and the roster is loaded before any Summit import exists to produce an
 * {@code employer} row. {@code prospectId} is held as a plain {@code Long} rather than a
 * {@code @ManyToOne Prospect} — this entity is written in bulk and read as a flat list, and a
 * managed association would pull a Prospect graph into every row for no reader that wants it.
 * <p>
 * <b>{@code id} is the opaque immutable participant key.</b> The Summit
 * {@code Participant TPA Custom ID} is derived from it at emit time as
 * {@code {SUMMIT_TPA_ID_PREFIX}-P-{id}} and is deliberately never stored (LA-33), the same
 * rule the employer key follows (LA-29/LA-32).
 * <p>
 * <b>Holds no SSN, date of birth, or compensation.</b> Summit's Demographics import requires
 * none of them; they are dropped at the parser as unrecognised columns as well as being absent
 * here (LA-35). {@code email} is nullable and populated only when the employer's file carries it.
 */
@Entity
@Table(name = "employer_participant")
public class EmployerParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "prospect_id", nullable = false)
    private Long prospectId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "address_line1", nullable = false)
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "state", nullable = false)
    private String state;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(name = "email")
    private String email;

    /**
     * Applied uniformly to every row from a single form field, never read from a spreadsheet
     * column — which is what makes it safe as the trailing column of Summit file 4.
     */
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    public EmployerParticipant() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProspectId() { return prospectId; }
    public void setProspectId(Long prospectId) { this.prospectId = prospectId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }

    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
