package by.academy.project.exception;

public class PromoCodeException extends Exception {
    public PromoCodeException(String message) {
        super(message);
    }
    public PromoCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}