package net.superiorstate.ams.model.sales.application;

import jakarta.persistence.*;

@Entity
public class ApplicationData {
    @EmbeddedId
    private ApplicationDataID applicationDataID;

    @Id
    @ManyToOne
    @MapsId("dataPairId")
    @JoinColumn(name="data_pair_id")
    private DataPair dataPair;

    @Id
    @ManyToOne
    @MapsId("applicationId")
    @JoinColumn(name="application_id")
    private Application application;

    public ApplicationData(){}

    public ApplicationDataID getApplicationDataID() {
        return applicationDataID;
    }

    public void setApplicationDataID(ApplicationDataID applicationDataID) {
        this.applicationDataID = applicationDataID;
    }

    public DataPair getDataPair() {
        return dataPair;
    }

    public void setDataPair(DataPair dataPair) {
        this.dataPair = dataPair;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }
}
