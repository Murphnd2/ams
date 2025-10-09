package net.superiorstate.ams.previous.data.model;

import java.util.ArrayList;
import java.util.List;

public abstract class dL {
    public static final String VID_WHAT_FSA ="https://www.youtu.be/1RXqF3Wp_FU";
    public static final String VID_WHAT_DCAP = "https://www.youtu.be/AA1kXyj7E58";
    public static final String VID_WHAT_HSA = "https://www.youtu.be/vAoEmR1v8gY";
    public static final String VID_MOBILE_APP = "https://www.youtu.be/R8YkGsw8o-c";
    public static final String VID_MOBILE_FILE_CLAIM = "https://youtu.be/MMxFTTNkRNY";
    public static final String VID_WEB_CHECK_BALANCE = "https://www.youtu.be/n5Sxdje50I4";
    public static final String VID_WEB_REGISTERING = "https://youtu.be/3psOz0lq244";

    public static final String PACKET_FSA_ENROLL = "https://superiorstate-my.sharepoint.com/:b:/g/personal/kevin_superiorstate_net/EWlpiOtducJGhKcLUC59JhUBcUpBypB0TAJ1NxU-SFFgwA?e=rs0Bo5";

    public static final String INFO_HRA_TYPES = "https://superiorstate-my.sharepoint.com/:b:/g/personal/kevin_superiorstate_net/ETZOzMzwcc5AuSC2jsENRJIBMlZQdW_qODeCJ35WW2veIg?e=S1zjho";
    public static final String INFO_HRA_ICHRA = "https://superiorstate-my.sharepoint.com/:b:/g/personal/kevin_superiorstate_net/ER0LYF_AE5NDulbqXqnHSIQBhlx35mrg1cVp2JMJyUm_iQ?e=YV9xeD";
    public static final String INFO_HRA_EBHRA = "https://superiorstate-my.sharepoint.com/:b:/g/personal/kevin_superiorstate_net/ETaOUE2YZsBFpes_ukhc820BJpbmoIi3rfxqNkeEOU2RBA?e=CdvMin";
    public static final String INFO_HRA_QSHRA = "https://superiorstate-my.sharepoint.com/:b:/g/personal/kevin_superiorstate_net/ERnGVhAsuHVJj6ph0aDR54MB-fhn_rEac5HmOhkcJ717Sg?e=1FzThR";
    public static final String INFO_HRA_BASIC = "https://superiorstate-my.sharepoint.com/:b:/g/personal/kevin_superiorstate_net/ET-t8NDA3ilLrwWN_M4dRHABvOBEF0UenCj5m7FDum5S9A?e=jX4HAb";
    public static final String INFO_FSA_LPFSA = "";
    public static final String INFO_TRANSIT = "";

    public static final String GUIDE_HSA_ENROLL = "https://superiorstate-my.sharepoint.com/:b:/g/personal/kevin_superiorstate_net/Ed9LaOXyBfZKhz-WaqYGgKYB2o4sQbOdXldBTH92LIafcQ?e=4rK7r1";
    public static final String GUIDE_SUMMIT_CDH_ER = "https://superiorstate-my.sharepoint.com/:b:/g/personal/kevin_superiorstate_net/EbCO6T1I7n5DmyHBQl1Ujc8BISXyn77XEOsl5n5fytF-JQ?e=CCd6wF";
    public static final String GUIDE_SUMMIT_PB_ER = "https://superiorstate-my.sharepoint.com/:b:/g/personal/kevin_superiorstate_net/EWtVE0s5bpBPiau_5WzcUooBPIoER06gwm4_rFhTsNzV4A?e=3r7vrU";


    public static List<InsertLinkPair> getInsertableLinks(){
        List<InsertLinkPair> linkList = new ArrayList<>();
        linkList.add(createLink("HSA Enrollment Guide",GUIDE_HSA_ENROLL));
        linkList.add(createLink("CDH Employer Guide",GUIDE_SUMMIT_CDH_ER));
        linkList.add(createLink("COBRA Employer Guide", GUIDE_SUMMIT_PB_ER));
        return linkList;

    }


    private static InsertLinkPair createLink(String linkName, String linkPath){
        String link = "<a target=\"_blank\" href=\"" + linkPath + "\">" + linkName + "</a>";
        InsertLinkPair ilp = new InsertLinkPair();
        ilp.setName(linkName);
        ilp.setLink(link);
        return ilp;
    }

    public static class InsertLinkPair{
        private String name;
        private String link;
        public InsertLinkPair(){}

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }
    }


}
