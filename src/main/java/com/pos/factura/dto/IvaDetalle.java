package com.pos.factura.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Línea del array "iva" del request a la API externa: un item por cada alícuota.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IvaDetalle {

    /** Id de alícuota según tabla AFIP (ver iva_alicuota) */
    @JsonProperty("id")
    private Integer id;

    @JsonProperty("baseImp")
    private BigDecimal baseImp;

    @JsonProperty("importe")
    private BigDecimal importe;
}
