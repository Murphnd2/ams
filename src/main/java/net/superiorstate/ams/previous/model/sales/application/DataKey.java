package net.superiorstate.ams.previous.model.sales.application;

import jakarta.persistence.*;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TemplatePurpose;

@Entity
public class DataKey {

    @Id
    @Column(name="key_name")
    private String keyName;

    @ManyToOne
    @JoinColumn(name = "template_purpose_id")
    private TemplatePurpose templatePurpose;

    @Column(name="easy_name")
    private String easyName;

    public DataKey(){}

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public TemplatePurpose getTemplatePurpose() {
        return templatePurpose;
    }

    public void setTemplatePurpose(TemplatePurpose templatePurpose) {
        this.templatePurpose = templatePurpose;
    }

    public String getEasyName() {
        if(easyName==null)
            return keyName;
        return easyName;
    }

    public void setEasyName(String easyName) {
        this.easyName = easyName;
    }
}
