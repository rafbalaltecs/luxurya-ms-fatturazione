package it.fatturazione.exception;

public class SdiException extends RuntimeException {
    public SdiException(String message) {
        super(message);
    }
    
    public SdiException(String message, Throwable cause) {
        super(message, cause);
    }
}
