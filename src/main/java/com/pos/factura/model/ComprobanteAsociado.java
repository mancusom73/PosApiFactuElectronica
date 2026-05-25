package com.pos.factura.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ComprobanteAsociado {

    /**
     * Tipo del comprobante original: "FACTURA A", "FACTURA B", "FACTURA C"
     */
    @JsonProperty("tipo_comprobante")
    private String tipoComprobante;

    @JsonProperty("punto_venta")
    private String puntoVenta;

    @JsonProperty("numero")
    private String numero;

    @JsonProperty("comprobante_fecha")
    private String comprobanteFecha;

    @JsonProperty("cuit")
    private Long cuit;
}
