package com.pos.factura.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Línea del array "cbtesAsoc": comprobante asociado (para ND/NC).
 */
@Data
public class CbteAsociado {

    /** Código AFIP del tipo de comprobante asociado (ver cbte_tipo) */
    @JsonProperty("tipo")
    private Integer tipo;

    @JsonProperty("ptoVta")
    private Integer ptoVta;

    @JsonProperty("nro")
    private Long nro;

    @JsonProperty("cuit")
    private Long cuit;
}
