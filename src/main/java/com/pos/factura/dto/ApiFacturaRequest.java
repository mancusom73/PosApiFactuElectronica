package com.pos.factura.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pos.factura.model.Cliente;
import com.pos.factura.model.Comprobante;
import lombok.Builder;
import lombok.Data;

/**
 * JSON completo que se envía al proveedor de factura electrónica.
 * Incluye credenciales de autenticación + datos del comprobante.
 */
@Data
@Builder
public class ApiFacturaRequest {

    @JsonProperty("usertoken")
    private String usertoken;

    @JsonProperty("apikey")
    private String apikey;

    @JsonProperty("apitoken")
    private String apitoken;

    @JsonProperty("cliente")
    private Cliente cliente;

    @JsonProperty("comprobante")
    private Comprobante comprobante;
}
