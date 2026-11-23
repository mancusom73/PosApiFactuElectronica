package com.pos.factura.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Línea del array "cbtesAsoc": comprobante asociado (para ND/NC).
 */
@Data
public class CbteAsociado {

    /** Código AFIP del tipo de comprobante asociado (ver cbte_tipo) */
    @JsonProperty("tipoAsociado")
    private Integer tipo;

    @JsonProperty("ptoVtaAsociado")
    private Integer ptoVta;

    @JsonProperty("nroAsociado")
    private Long nro;

    @JsonProperty("cuitAsociado")
    private String cuit;
}
