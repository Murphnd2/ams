package net.superiorstate.ams.previous.model.general;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="sequence")
public class SequenceTracker {
    @Id
    @Column(name="SEQ_NAME")
    private String sequenceName;
    @Column(name="SEQ_COUNT")
    private int sequenceCount;
    public SequenceTracker(){}

    public String getSequenceName() {
        return sequenceName;
    }

    public void setSequenceName(String sequenceName) {
        this.sequenceName = sequenceName;
    }

    public int getSequenceCount() {
        return sequenceCount;
    }

    public void setSequenceCount(int sequenceCount) {
        this.sequenceCount = sequenceCount;
    }
}
