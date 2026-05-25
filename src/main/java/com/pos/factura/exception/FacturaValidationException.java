package com.pos.factura.exception;

import java.util.List;

public class FacturaValidationException extends RuntimeException {

    private final List<String> errores;

    public FacturaValidationException(List<String> errores) {
        super("Error de validación del comprobante");
        this.errores = errores;
    }

    public List<String> getErrores() {
        return errores;
    }
}
