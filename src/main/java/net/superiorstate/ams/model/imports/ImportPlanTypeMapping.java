package net.superiorstate.ams.model.imports;

import jakarta.persistence.*;
import net.superiorstate.ams.model.summit.archive.PlanType;

@Entity
@Table(name = "import_plan_type_mapping")
public class ImportPlanTypeMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mapping_id")
    private int id;

    @ManyToOne
    @JoinColumn(name = "provider_id")
    private ImportProvider provider;

    @Column(name = "source_plan_code", nullable = false, length = 50)
    private String sourcePlanCode;

    @Column(name = "source_plan_name", length = 200)
    private String sourcePlanName;

    @ManyToOne
    @JoinColumn(name = "target_plan_type_id")
    private PlanType targetPlanType;

    @Column(name = "is_system_default", nullable = false)
    private boolean systemDefault = false;

    public ImportPlanTypeMapping() {}

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

    public String getSourcePlanCode() {
        return sourcePlanCode;
    }

    public void setSourcePlanCode(String sourcePlanCode) {
        this.sourcePlanCode = sourcePlanCode;
    }

    public String getSourcePlanName() {
        return sourcePlanName;
    }

    public void setSourcePlanName(String sourcePlanName) {
        this.sourcePlanName = sourcePlanName;
    }

    public PlanType getTargetPlanType() {
        return targetPlanType;
    }

    public void setTargetPlanType(PlanType targetPlanType) {
        this.targetPlanType = targetPlanType;
    }

    public boolean isSystemDefault() {
        return systemDefault;
    }

    public void setSystemDefault(boolean systemDefault) {
        this.systemDefault = systemDefault;
    }
}
