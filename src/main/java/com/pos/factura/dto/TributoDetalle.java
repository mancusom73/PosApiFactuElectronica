package com.pos.factura.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.pos.factura.util.DosDecimalesSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Línea del array "tributos" del request a la API externa (ver tributo_tipo).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TributoDetalle {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("descripcion")
    private String descripcion;

    @JsonProperty("baseImp")
    @JsonSerialize(using = DosDecimalesSerializer.class)
    private BigDecimal baseImp;

    @JsonProperty("alicuota")
    @JsonSerialize(using = DosDecimalesSerializer.class)
    private BigDecimal alicuota;

    @JsonProperty("importe")
    @JsonSerialize(using = DosDecimalesSerializer.class)
    private BigDecimal importe;
}
