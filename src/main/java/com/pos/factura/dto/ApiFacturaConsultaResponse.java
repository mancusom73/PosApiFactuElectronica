package com.pos.factura.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Respuesta del GET /facturas/{id} a la API externa de factura electrónica.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiFacturaConsultaResponse {

    @JsonProperty("id")
    private String id;

    /** Ej: "Autorizada" */
    @JsonProperty("estado")
    private String estado;

    @JsonProperty("modoUsado")
    private String modoUsado;

    @JsonProperty("comprobante")
    private Comprobante comprobante;

    @JsonProperty("error")
    private Error error;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Error {
        @JsonProperty("codigoArca")
        private String codigoArca;

        @JsonProperty("mensajeArca")
        private String mensajeArca;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Comprobante {
        @JsonProperty("ptoVta")
        private Integer ptoVta;

        @JsonProperty("cbteTipo")
        private Integer cbteTipo;

        @JsonProperty("nroCbte")
        private Long nroCbte;

        @JsonProperty("cae")
        private String cae;

        @JsonProperty("caeVencimiento")
        private String caeVencimiento;
    }
}
