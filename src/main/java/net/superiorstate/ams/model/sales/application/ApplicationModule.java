package net.superiorstate.ams.model.sales.application;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TemplatePurpose;

@Entity
public class ApplicationModule {
    @EmbeddedId
    private ApplicationModuleID applicationModuleID;

    @Id
    @ManyToOne
    @MapsId("applicationId")
    @JoinColumn(name="application_id")
    private Application application;

    @Id
    @ManyToOne
    @MapsId("templatePurposeId")
    @JoinColumn(name = "template_purpose_id")
    private TemplatePurpose templatePurpose;

    public ApplicationModule(){}

    public ApplicationModuleID getApplicationModuleID() {
        return applicationModuleID;
    }

    public void setApplicationModuleID(ApplicationModuleID applicationModuleID) {
        this.applicationModuleID = applicationModuleID;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public TemplatePurpose getTemplatePurpose() {
        return templatePurpose;
    }

    public void setTemplatePurpose(TemplatePurpose templatePurpose) {
        this.templatePurpose = templatePurpose;
    }
}
