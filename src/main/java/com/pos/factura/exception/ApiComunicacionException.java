package com.pos.factura.exception;

public class ApiComunicacionException extends RuntimeException {

    private final int statusCode;
    private final String code;
    private final String detail;

    public ApiComunicacionException(String mensaje, int statusCode) {
        super(mensaje);
        this.statusCode = statusCode;
        this.code = null;
        this.detail = null;
    }

    public ApiComunicacionException(String code, String detail, int statusCode) {
        super(detail != null && !detail.isBlank() ? code + ": " + detail : code);
        this.statusCode = statusCode;
        this.code = code;
        this.detail = detail;
    }

    public ApiComunicacionException(String mensaje, Throwable cause) {
        super(mensaje, cause);
        this.statusCode = 503;
        this.code = null;
        this.detail = null;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getCode() {
        return code;
    }

    public String getDetail() {
        return detail;
    }
}
