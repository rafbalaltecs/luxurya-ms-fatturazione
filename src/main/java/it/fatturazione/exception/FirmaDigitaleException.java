package it.fatturazione.exception;

public class FirmaDigitaleException extends RuntimeException {
    public FirmaDigitaleException(String message) {
        super(message);
    }
    
    public FirmaDigitaleException(String message, Throwable cause) {
        super(message, cause);
    }
}
