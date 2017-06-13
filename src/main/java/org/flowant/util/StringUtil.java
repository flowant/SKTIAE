package org.flowant.util;

public class StringUtil {
    public static String removeNonDigit(String yearMonth) {
        return yearMonth.replaceAll("\\D"/* NonDigit */, "");
    }
}
