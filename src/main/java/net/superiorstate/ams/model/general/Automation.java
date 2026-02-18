package net.superiorstate.ams.model.general;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Automation {

    @Id
    @Column(name="auto_id")
    private int id;

    @Column(name="name")
    private String automationName;

    @Column(name="input_count")
    private int inputCount;

    @Column(name="input_names")
    private String inputNames;

    @Column(name="is_link")
    private String inputLinks;

    @Column(name="content")
    private String content;

    public Automation(){}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAutomationName() {
        return automationName;
    }

    public void setAutomationName(String automationName) {
        this.automationName = automationName;
    }

    public int getInputCount() {
        return inputCount;
    }

    public void setInputCount(int inputCount) {
        this.inputCount = inputCount;
    }

    public String getInputNames() {
        return inputNames;
    }

    public void setInputNames(String inputNames) {
        this.inputNames = inputNames;
    }

    public String getInputLinks() {
        return inputLinks;
    }

    public void setInputLinks(String inputLinks) {
        this.inputLinks = inputLinks;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getHtmlContent(){

        String stepOne = getContent().replaceAll("<","&lt;");
        String stepTwo = stepOne.replaceAll(">","&gt;");
        return "<pre><code>" + stepTwo + "</code></pre>";
    }
}
