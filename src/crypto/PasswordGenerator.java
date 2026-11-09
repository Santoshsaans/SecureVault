package src.crypto;

import java.security.SecureRandom;

public class PasswordGenerator {

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{}";
    private static final String ALL = UPPER + LOWER + DIGITS + SYMBOLS;

    public static String generate(int length) {
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        sb.append(UPPER.charAt(rng.nextInt(UPPER.length())));
        sb.append(LOWER.charAt(rng.nextInt(LOWER.length())));
        sb.append(DIGITS.charAt(rng.nextInt(DIGITS.length())));
        sb.append(SYMBOLS.charAt(rng.nextInt(SYMBOLS.length())));
        for (int i = 4; i < length; i++) {
            sb.append(ALL.charAt(rng.nextInt(ALL.length())));
        }
        return shuffle(sb.toString(), rng);
    }

    private static String shuffle(String s, SecureRandom rng) {
        char[] arr = s.toCharArray();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            char tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
        }
        return new String(arr);
    }
}
