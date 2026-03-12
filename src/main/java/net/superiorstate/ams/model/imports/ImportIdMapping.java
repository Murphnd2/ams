package net.superiorstate.ams.model.imports;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "import_id_mapping")
@NamedQueries({
    @NamedQuery(
        name = "ImportIdMapping.resolve",
        query = "SELECT m FROM ImportIdMapping m WHERE m.provider.id = :providerId AND m.entityType = :entityType AND m.externalId = :externalId"
    ),
    @NamedQuery(
        name = "ImportIdMapping.findByInternal",
        query = "SELECT m FROM ImportIdMapping m WHERE m.entityType = :entityType AND m.internalId = :internalId"
    ),
    @NamedQuery(
        name = "ImportIdMapping.findByProvider",
        query = "SELECT m FROM ImportIdMapping m WHERE m.provider.id = :providerId AND m.entityType = :entityType ORDER BY m.externalId"
    )
})
public class ImportIdMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mapping_id")
    private int id;

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    private ImportProvider provider;

    @Column(name = "entity_type", nullable = false, length = 20)
    private String entityType;

    @Column(name = "external_id", nullable = false, length = 50)
    private String externalId;

    @Column(name = "internal_id", nullable = false)
    private int internalId;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = true;

    @Column(name = "notes", length = 200)
    private String notes;

    @Column(name = "created_on", insertable = false, updatable = false)
    private Timestamp createdOn;

    @Column(name = "updated_on", insertable = false, updatable = false)
    private Timestamp updatedOn;

    public ImportIdMapping() {}

    public ImportIdMapping(ImportProvider provider, String entityType, String externalId, int internalId) {
        this.provider = provider;
        this.entityType = entityType;
        this.externalId = externalId;
        this.internalId = internalId;
        this.primary = true;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ImportProvider getProvider() {
        return provider;
    }

    public void setProvider(ImportProvider provider) {
        this.provider = provider;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public int getInternalId() {
        return internalId;
    }

    public void setInternalId(int internalId) {
        this.internalId = internalId;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Timestamp getCreatedOn() {
        return createdOn;
    }

    public Timestamp getUpdatedOn() {
        return updatedOn;
    }
}
