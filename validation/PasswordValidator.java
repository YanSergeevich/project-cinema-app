package by.academy.project.validation;

public class PasswordValidator {
    private static final int MIN_LENGTH = 8;

    public static boolean isValid(String password) {
        return password != null &&
                password.length() >= MIN_LENGTH &&
                password.matches(".*[a-zA-Zа-яА-Я].*");
    }

    public static String validateWithMessage(String password) {
        if (password == null || password.isEmpty()) {
            return "Пароль не может быть пустым";
        }
        if (password.length() < MIN_LENGTH) {
            return "Пароль должен содержать минимум " + MIN_LENGTH + " символов";
        }
        if (!password.matches(".*[a-zA-Zа-яА-Я].*")) {
            return "Пароль должен содержать хотя бы одну букву";
        }
        return null;
    }
}