package cn.edu.zzti.qi.util;

public final class StrUtils {

    public static final boolean equals(String str1, String str2) {
        if (null == str1 || null == str2) return false;
        return str1.equals(str2);
    }

    public static final boolean equalsIgnoreCase(String str1, String str2) {
        if (null == str1 || null == str2) return false;
        return str1.equalsIgnoreCase(str2);
    }

    public static final boolean isEmpty(String str) {
        return null == str || str.length() <= 0;
    }

    public static final boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
}
