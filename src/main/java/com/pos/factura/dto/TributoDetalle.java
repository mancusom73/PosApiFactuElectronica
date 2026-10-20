package com.pos.factura.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("baseImp")
    private BigDecimal baseImp;

    @JsonProperty("importe")
    private BigDecimal importe;
}
