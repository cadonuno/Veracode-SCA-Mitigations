package util;

import java.util.List;

public class EmptyUtils {
    public static boolean isNullOrEmpty(String aString) {
        return aString == null || "".equals(aString.trim());
    }

    public static boolean isNullOrEmptyList(List<?> listToCheck) {
        return listToCheck == null || listToCheck.isEmpty();
    }
}
