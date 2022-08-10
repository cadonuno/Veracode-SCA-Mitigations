package util;

import java.util.regex.Pattern;

public class NumbersUtil {
    private static final Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");

    public static boolean isNumeric(String aString) {
        if (aString == null) {
            return false;
        }
        return pattern.matcher(aString).matches();
    }
}
