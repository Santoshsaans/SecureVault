package src.crypto;

public class PasswordStrength {

    public enum Level { VERY_WEAK, WEAK, MEDIUM, STRONG, VERY_STRONG }

    public static Level evaluate(String pw) {
        if (pw == null || pw.isEmpty()) return Level.VERY_WEAK;
        int score = 0;
        if (pw.length() >= 8)  score++;
        if (pw.length() >= 12) score++;
        if (pw.length() >= 16) score++;
        if (pw.matches(".*[A-Z].*")) score++;
        if (pw.matches(".*[a-z].*")) score++;
        if (pw.matches(".*[0-9].*")) score++;
        if (pw.matches(".*[^A-Za-z0-9].*")) score++;

        // Penalties
        if (pw.matches(".*(password|123456|qwerty|admin|letmein|welcome).*")) score -= 3;
        if (pw.matches("(.)\\1{3,}.*")) score -= 2;              // aaaa
        if (pw.matches(".*(0123|1234|abcd|abcd|9876).*")) score -= 2;

        if (score <= 1) return Level.VERY_WEAK;
        if (score <= 3) return Level.WEAK;
        if (score <= 4) return Level.MEDIUM;
        if (score <= 5) return Level.STRONG;
        return Level.VERY_STRONG;
    }

    public static String describe(Level l) {
        switch (l) {
            case VERY_WEAK:   return "Very Weak";
            case WEAK:        return "Weak";
            case MEDIUM:      return "Medium";
            case STRONG:      return "Strong";
            case VERY_STRONG: return "Very Strong";
            default:          return "?";
        }
    }

    public static boolean isAcceptableForMaster(String pw) {
        if (pw == null || pw.length() < 8) return false;
        Level l = evaluate(pw);
        return l == Level.MEDIUM || l == Level.STRONG || l == Level.VERY_STRONG;
    }
}