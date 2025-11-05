package net.superiorstate.ams.previous.model.activity.note;

import java.util.Comparator;

public class EmailComparator implements Comparator<Email> {
    @Override
    public int compare(Email o1, Email o2) {
        if(o2.getDateGenerated().compareTo(o1.getDateGenerated())!=0){
            return o2.getDateGenerated().compareTo(o1.getDateGenerated());
        } else {
            return o2.getId().compareTo(o1.getId());
        }
    }
}
