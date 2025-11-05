package net.superiorstate.ams.previous.data.model;

public abstract class dH {

    public static String wrapInAnchorTag(String theLink){
        return wrapInAnchorTag(theLink,theLink);
    }

    public static String wrapInAnchorTag(String theLink, String name){
        String firstPart = "<a target=\"_blank\" href=\"";
        String thirdPart = "\">";
        String lastPart = "</a>";
        return firstPart + theLink + thirdPart + name + lastPart;
    }
}
