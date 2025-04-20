package com.shade.platform.model.util;

import com.shade.util.NotNull;

import java.util.Comparator;

public class AlphanumericComparator implements Comparator<CharSequence> {
    private static final AlphanumericComparator INSTANCE = new AlphanumericComparator();

    private AlphanumericComparator() {
        // prevents instantiation
    }

    @NotNull
    public static AlphanumericComparator getInstance() {
        return INSTANCE;
    }

    @Override
    public int compare(CharSequence o1, CharSequence o2) {
        final int len1 = o1.length();
        final int len2 = o2.length();

        int i = 0;
        int j = 0;

        while (i < len1 && j < len2) {
            final char ch1 = o1.charAt(i);
            final char ch2 = o2.charAt(i);

            if (Character.isDigit(ch1) && Character.isDigit(ch2)) {
                int num1 = 0;
                int num2 = 0;

                while (i < len1 && Character.isDigit(o1.charAt(i))) {
                    num1 = num1 * 10 + Character.digit(o1.charAt(i), 10);
                    i += 1;
                }

                while (j < len2 && Character.isDigit(o2.charAt(j))) {
                    num2 = num2 * 10 + Character.digit(o2.charAt(j), 10);
                    j += 1;
                }

                if (num1 != num2) {
                    return num1 - num2;
                }
            } else {
                if (ch1 != ch2) {
                    return ch1 - ch2;
                }

                i += 1;
                j += 1;
            }
        }

        return len1 - len2;
    }
}
