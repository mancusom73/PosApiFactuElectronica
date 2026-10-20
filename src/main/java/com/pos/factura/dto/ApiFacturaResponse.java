package com.pos.factura.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Respuesta del POST a la API externa de factura electrónica.
 * La API responde 202 (Accepted): el comprobante queda encolado y se
 * procesa de forma asincrónica. El resultado final (CAE, etc.) se
 * consulta aparte, en el endpoint indicado por links.self.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiFacturaResponse {

    /** Id del comprobante encolado, usado luego para consultar el estado */
    @JsonProperty("id")
    private String id;

    /** Ej: "Encolada" */
    @JsonProperty("estado")
    private String estado;

    @JsonProperty("links")
    private Links links;

    public boolean isExitoso() {
        return id != null;
    }

    @Data
    public static class Links {
        @JsonProperty("self")
        private String self;
    }
}
