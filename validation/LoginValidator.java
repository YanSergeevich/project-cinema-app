package by.academy.project.validation;

import java.util.regex.Pattern;

public class LoginValidator {
    private static final String[] BANNED_ROOTS = {
            "admin", "админ", "adm", "адм"
    };

    private static final char[][] SUBSTITUTIONS = {
            {'a', '@', '4'},
            {'d', 'D'},
            {'m', 'M'},
            {'i', '1', '!', '|'},
            {'n', 'N'},
            {'а', 'А', '@'},
            {'д', 'Д'},
            {'м', 'М'},
            {'и', 'И', '1', '!'},
            {'н', 'Н'}
    };

    private static final String SEPARATORS = "[._\\-\\s]";

    public static boolean isValid(String login) {
        if (login == null || login.trim().isEmpty()) {
            return false;
        }

        String lowerLogin = login.toLowerCase();

        for (String root : BANNED_ROOTS) {
            if (lowerLogin.equals(root)) {
                return false;
            }
        }

        String normalized = login.replaceAll(SEPARATORS, "");

        return !containsBannedWord(normalized);
    }

    private static boolean containsBannedWord(String text) {
        for (String root : BANNED_ROOTS) {
            if (containsWithSubstitutions(text, root)) {
                return true;
            }

            String upperRoot = root.toUpperCase();
            if (containsWithSubstitutions(text, upperRoot)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsWithSubstitutions(String text, String pattern) {
        if (text.length() < pattern.length()) {
            return false;
        }

        StringBuilder regex = new StringBuilder();
        for (char c : pattern.toCharArray()) {
            regex.append(getCharClass(c));
        }

        Pattern p = Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
        return p.matcher(text).find();
    }

    private static String getCharClass(char c) {
        return switch (Character.toLowerCase(c)) {
            case 'a', 'а' -> "[aа@4]";
            case 'd', 'д' -> "[dд]";
            case 'm', 'м' -> "[mм]";
            case 'i', 'и' -> "[iи1!|]";
            case 'n', 'н' -> "[nн]";
            default -> String.valueOf(c);
        };
    }

    public static boolean canBeAdmin(String login, boolean isMainAdmin) {
        if (isMainAdmin) {
            return true;
        }
        return !containsBannedWord(login.replaceAll(SEPARATORS, ""));
    }
}