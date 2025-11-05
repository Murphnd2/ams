package net.superiorstate.ams.previous.model.general;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class LinkType {
    @Id
    @Column(name="link_type_id")
    private int id;

    @Column(name="type_name")
    private String typeName;

    public LinkType(){}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }
}
