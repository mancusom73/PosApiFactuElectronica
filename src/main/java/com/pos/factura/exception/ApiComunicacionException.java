package com.pos.factura.exception;

public class ApiComunicacionException extends RuntimeException {

    private final int statusCode;

    public ApiComunicacionException(String mensaje, int statusCode) {
        super(mensaje);
        this.statusCode = statusCode;
    }

    public ApiComunicacionException(String mensaje, Throwable cause) {
        super(mensaje, cause);
        this.statusCode = 503;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
