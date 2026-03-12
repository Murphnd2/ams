package net.superiorstate.ams.model.imports;

import jakarta.persistence.*;

@Entity
@Table(name = "import_file_type")
public class ImportFileType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_type_id")
    private int id;

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    private ImportProvider provider;

    @Column(name = "file_label", nullable = false, length = 100)
    private String fileLabel;

    @Column(name = "target_entity", nullable = false, length = 20)
    private String targetEntity;

    @Column(name = "file_format", nullable = false, length = 10)
    private String fileFormat = "CSV";

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "is_required", nullable = false)
    private boolean required = true;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "update_mode", nullable = false, length = 20)
    private String updateMode = "CREATE_AND_UPDATE";

    @Column(name = "mapping_status", nullable = false, length = 10)
    private String mappingStatus = "PENDING";

    public ImportFileType() {}

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

    public String getFileLabel() {
        return fileLabel;
    }

    public void setFileLabel(String fileLabel) {
        this.fileLabel = fileLabel;
    }

    public String getTargetEntity() {
        return targetEntity;
    }

    public void setTargetEntity(String targetEntity) {
        this.targetEntity = targetEntity;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUpdateMode() {
        return updateMode;
    }

    public void setUpdateMode(String updateMode) {
        this.updateMode = updateMode;
    }

    public String getMappingStatus() {
        return mappingStatus;
    }

    public void setMappingStatus(String mappingStatus) {
        this.mappingStatus = mappingStatus;
    }
}
