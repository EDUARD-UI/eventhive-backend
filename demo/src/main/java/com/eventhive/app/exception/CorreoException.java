package com.eventhive.app.exception;

public class CorreoException extends IllegalStateException {
    public CorreoException(String mensaje) { super(mensaje); }
    public CorreoException(String mensaje, Throwable causa) { super(mensaje, causa); }
}