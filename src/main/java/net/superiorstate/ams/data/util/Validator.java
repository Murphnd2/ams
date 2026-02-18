package net.superiorstate.ams.data.util;


import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Validator {
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
    public static Optional<String> getExtensionByStringHandling(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

}
