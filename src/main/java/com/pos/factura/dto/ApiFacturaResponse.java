package com.pos.factura.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Respuesta del proveedor de factura electrónica.
 * Los campos pueden variar según el proveedor — ajustar según documentación.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiFacturaResponse {

    @JsonProperty("exitoso")
    private boolean exitoso;

    @JsonProperty("errores")
    private String[] errores;

    /**
     * CAE = Código de Autorización Electrónico (AFIP)
     */
    @JsonProperty("cae")
    private String cae;

    @JsonProperty("caeVencimiento")
    private String caeVencimiento;

    /**
     * Número de comprobante asignado por AFIP
     */
    @JsonProperty("comprobanteNro")
    private String comprobanteNro;

    /**
     * PDF del comprobante en Base64
     */
    @JsonProperty("pdfBase64")
    private String comprobantePdfB64;

    /**
     * URL pública del PDF (algunos proveedores lo ofrecen)
     */
    @JsonProperty("pdfUrl")
    private String comprobantePdfUrl;

    /**
     * Código QR según normativa AFIP 2024
     */
    @JsonProperty("afipQr")
    private String afipQr;

    @JsonProperty("afipCodigoBarras")
    private String afipCodigoBarras;

    public boolean isExitoso() {
        return exitoso;
    }
}
