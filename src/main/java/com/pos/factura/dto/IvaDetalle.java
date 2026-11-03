package com.pos.factura.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.pos.factura.util.DosDecimalesSerializer;
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
    @JsonSerialize(using = DosDecimalesSerializer.class)
    private BigDecimal baseImp;

    @JsonProperty("importe")
    @JsonSerialize(using = DosDecimalesSerializer.class)
    private BigDecimal importe;
}
