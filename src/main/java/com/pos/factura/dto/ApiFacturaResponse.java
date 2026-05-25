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

    /**
     * "OK" si la factura fue emitida correctamente, "ERROR" si falló
     */
    @JsonProperty("error")
    private String error;

    @JsonProperty("errores")
    private String[] errores;

    /**
     * CAE = Código de Autorización Electrónico (AFIP)
     */
    @JsonProperty("cae")
    private String cae;

    @JsonProperty("cae_vencimiento")
    private String caeVencimiento;

    /**
     * Número de comprobante asignado por AFIP
     */
    @JsonProperty("comprobante_nro")
    private String comprobanteNro;

    /**
     * PDF del comprobante en Base64
     */
    @JsonProperty("comprobante_pdf_b64")
    private String comprobantePdfB64;

    /**
     * URL pública del PDF (algunos proveedores lo ofrecen)
     */
    @JsonProperty("comprobante_pdf_url")
    private String comprobantePdfUrl;

    /**
     * Código QR según normativa AFIP 2024
     */
    @JsonProperty("afip_qr")
    private String afipQr;

    @JsonProperty("afip_codigo_barras")
    private String afipCodigoBarras;

    public boolean isExitoso() {
        return error != null && error.equalsIgnoreCase("N");
    }
}
