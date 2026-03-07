package net.superiorstate.ams.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Constant {
    @Id
    @Column(name="name")
    private String name;

    @Column(name="value")
    private String value;

    @Column(name="note")
    private String note;

    @Column(name="text_value", columnDefinition = "text")
    private String textValue;

    public Constant(){}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getTextValue() {
        return textValue;
    }

    public void setTextValue(String textValue) {
        this.textValue = textValue;
    }
}
