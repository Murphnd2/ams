package net.superiorstate.ams.model.imports;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "import_provider")
public class ImportProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "provider_id")
    private int id;

    @Column(name = "provider_name", nullable = false, length = 100)
    private String providerName;

    @Column(name = "provider_code", nullable = false, unique = true, length = 30)
    private String providerCode;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "psp_id", nullable = false)
    private long pspId;

    @Column(name = "created_on", insertable = false, updatable = false)
    private Timestamp createdOn;

    public ImportProvider() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getPspId() {
        return pspId;
    }

    public void setPspId(long pspId) {
        this.pspId = pspId;
    }

    public Timestamp getCreatedOn() {
        return createdOn;
    }
}
