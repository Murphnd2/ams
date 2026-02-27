package net.superiorstate.ams.model.sales.application;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;

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
    private ServiceItem serviceItem;

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

    public ServiceItem getServiceItem() {
        return serviceItem;
    }

    public void setServiceItem(ServiceItem serviceItem) {
        this.serviceItem = serviceItem;
    }
}
