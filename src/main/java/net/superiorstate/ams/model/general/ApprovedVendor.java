package net.superiorstate.ams.model.general;

import jakarta.persistence.*;
import java.sql.Date;
import java.time.LocalDate;

@Entity
@Table(name = "approved_vendors")
public class ApprovedVendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vendor_id")
    private Long vendorId;

    @Column(name = "vendor_name", nullable = false, length = 100)
    private String vendorName;

    @Column(name = "vendor_url", nullable = false, length = 255)
    private String vendorUrl;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "date_added", nullable = false)
    private Date dateAdded;

    public ApprovedVendor() {}

    @PrePersist
    private void prePersist() {
        if (dateAdded == null) {
            dateAdded = Date.valueOf(LocalDate.now());
        }
    }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public String getVendorUrl() { return vendorUrl; }
    public void setVendorUrl(String vendorUrl) { this.vendorUrl = vendorUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Date getDateAdded() { return dateAdded; }
    public void setDateAdded(Date dateAdded) { this.dateAdded = dateAdded; }
}
