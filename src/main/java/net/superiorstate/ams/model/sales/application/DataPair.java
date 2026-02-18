package net.superiorstate.ams.model.sales.application;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class DataPair {
    @Id
    @GeneratedValue
    @Column(name="data_pair_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "key_name")
    private DataKey dataKey;

    @Column(name="data_value")
    private String dataValue;

    @OneToMany(mappedBy = "dataPair")
    List<ApplicationData> applicationDataList;


    public DataPair(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DataKey getDataKeyAssociation() {
        return dataKey;
    }

    public void setDataKeyAssociation(DataKey dataKey) {
        this.dataKey = dataKey;
    }

    public String getDataValue() {
        return dataValue;
    }

    public void setDataValue(String dataValue) {
        this.dataValue = dataValue;
    }

    public List<ApplicationData> getApplicationDataList() {
        return applicationDataList;
    }

    public void setApplicationDataList(List<ApplicationData> applicationDataList) {
        this.applicationDataList = applicationDataList;
    }
}
