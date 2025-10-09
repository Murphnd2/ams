package net.superiorstate.ams.previous.data;


import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class V {
    private static final String URL_REGEX =
            "^(https?|ftp)://"
                    + "(([a-zA-Z0-9\\-]+\\.)+[a-zA-Z]{2,6})" // domain name
                    + "(:[0-9]{1,5})?" // port (optional)
                    + "(/[^\\s]*)?$"; // resource path (optional)
    private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    private static final String PHONE_REGEX =
            "^(\\+1[-\\s.]?)?\\(?\\d{3}\\)?[-\\s.]?\\d{3}[-\\s.]?\\d{4}$";

    private static final Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX);

    public static boolean isValidURL(String url) {
        if (url == null) {
            return false;
        }
        Matcher matcher = URL_PATTERN.matcher(url);
        return matcher.matches();
    }
    public static boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        return matcher.matches();
    }

    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return false;
        }
        Matcher matcher = PHONE_PATTERN.matcher(phoneNumber);
        return matcher.matches();
    }

    public static String normalizeEmployerName(String rawName) {
        if (rawName == null) return null;

        String normalized = rawName.toLowerCase();

        // Remove punctuation
        normalized = normalized.replaceAll("[.,]", "");

        // Replace "&" with "and"
        normalized = normalized.replaceAll("&", "and");

        // Remove legal suffixes
        normalized = normalized.replaceAll("\\b(inc|llc|ltd|incorporated|corporation)\\b$", "");

        // Collapse multiple spaces and trim
        normalized = normalized.replaceAll("\\s+", " ").trim();

        return normalized;
    }

    public static String normalizeName(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .replaceAll("\\b(jr|sr|ii|iii|iv|v)\\b", "") // remove suffixes
                .replaceAll("[^a-z ]", "") // remove punctuation/numbers
                .replaceAll("\\s+", " ")   // collapse spaces
                .trim();
    }
    public static Date parseDate(String input) {
        if (input == null || input.trim().isEmpty()) return null;
        try {
            return (Date) new SimpleDateFormat("MM/dd/yyyy").parse(input.trim());
        } catch (ParseException e) {
            System.err.println("Failed to parse date: " + input);
            return null;
        }
    }


}
