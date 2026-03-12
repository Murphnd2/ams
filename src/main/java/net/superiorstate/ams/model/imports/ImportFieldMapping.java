package net.superiorstate.ams.model.imports;

import jakarta.persistence.*;

@Entity
@Table(name = "import_field_mapping")
public class ImportFieldMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mapping_id")
    private int id;

    @ManyToOne
    @JoinColumn(name = "file_type_id", nullable = false)
    private ImportFileType fileType;

    @Column(name = "source_column", nullable = false, length = 100)
    private String sourceColumn;

    @Column(name = "canonical_field", nullable = false, length = 50)
    private String canonicalField;

    @Column(name = "is_required", nullable = false)
    private boolean required = false;

    @Column(name = "is_key", nullable = false)
    private boolean key = false;

    @Column(name = "transform_rule", length = 200)
    private String transformRule;

    @Column(name = "is_fk", nullable = false)
    private boolean fk = false;

    @Column(name = "fk_entity_type", length = 20)
    private String fkEntityType;

    public ImportFieldMapping() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ImportFileType getFileType() {
        return fileType;
    }

    public void setFileType(ImportFileType fileType) {
        this.fileType = fileType;
    }

    public String getSourceColumn() {
        return sourceColumn;
    }

    public void setSourceColumn(String sourceColumn) {
        this.sourceColumn = sourceColumn;
    }

    public String getCanonicalField() {
        return canonicalField;
    }

    public void setCanonicalField(String canonicalField) {
        this.canonicalField = canonicalField;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isKey() {
        return key;
    }

    public void setKey(boolean key) {
        this.key = key;
    }

    public String getTransformRule() {
        return transformRule;
    }

    public void setTransformRule(String transformRule) {
        this.transformRule = transformRule;
    }

    public boolean isFk() {
        return fk;
    }

    public void setFk(boolean fk) {
        this.fk = fk;
    }

    public String getFkEntityType() {
        return fkEntityType;
    }

    public void setFkEntityType(String fkEntityType) {
        this.fkEntityType = fkEntityType;
    }
}
