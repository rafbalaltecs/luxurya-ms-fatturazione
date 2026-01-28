package it.fatturazione.exception;

public class FatturaNotFoundException extends RuntimeException {
    public FatturaNotFoundException(String message) {
        super(message);
    }
}
