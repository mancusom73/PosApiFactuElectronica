package com.pos.factura.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Respuesta simplificada que el middleware devuelve al punto de venta.
 */
@Data
@Builder
public class FacturaResponse {

    private boolean exitoso;
    private String mensaje;

    // Datos de la factura emitida
    private String cae;
    private String caeVencimiento;
    private String comprobanteNro;
    private String tipoComprobante;
    private String fecha;
    private BigDecimal total;

    // Recursos del comprobante
    private String pdfBase64;
    private String pdfUrl;
    private String afipQr;
    private String afipCodigoBarras;

    // En caso de error
    private String[] errores;
}
