package net.superiorstate.ams.data.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public class AutoSafe {

    private static final Safelist EMAIL_WHITELIST = Safelist.relaxed()
            .addTags("p","br","b","i","u","ul","ol","li","a")
            .addAttributes("a", "href", "target")
            .addProtocols("a", "href", "http", "https", "mailto");

    /** Used for input labels and final email body **/
    public static String clean(String input) {
        if (input == null) return "";
        return Jsoup.clean(input, EMAIL_WHITELIST);
    }

    /** Only allow expected aInput-0 … aInput-N parameters **/
    public static String getInput(String valueFromUser, int maxIndex, int index) {
        if (index < 0 || index > maxIndex || valueFromUser == null) {
            return "";
        }
        return clean(valueFromUser);
    }
}