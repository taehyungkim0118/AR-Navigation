package com.example.tmaptest.Utils;

public class StringUtils {
    public static String join(char delim, String... strings) {
        if (strings.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(strings[0]);
        for (int i=1; i<strings.length; i++) {
            if (strings[i] != null && !strings[i].isEmpty()) {
                sb.append(delim + strings[i]);
            }
        }

        return sb.toString();
    }
}
