package io.neba.core.util;

/**
 * @author Olaf Otto
 */
public class StringUtil {
    /**
     * Appends the given String to the given array of Strings.
     *
     * @param append must not be <code>null</code>.
     * @param appendTo must not be <code>null</code>.
     *
     * @return a new array representing the concatenation.
     */
    public static String[] append(String append, String[] appendTo) {
        if (append == null) {
            throw new IllegalArgumentException("Method argument append must not be null.");
        }
        if (appendTo == null) {
            throw new IllegalArgumentException("Method argument to must not be null.");
        }

        String[] result = new String[appendTo.length];
        for (int i = 0; i  < appendTo.length; ++i) {
            result[i] = appendTo[i] == null ? null : appendTo[i] + append;
        }

        return result;
    }
    private StringUtil() {}
}
