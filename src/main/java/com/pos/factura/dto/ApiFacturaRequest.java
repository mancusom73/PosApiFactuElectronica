package com.pos.factura.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * JSON que se envía a la API externa de factura electrónica.
 * Formato plano con los nombres/códigos de ARCA (ex-AFIP).
 */
@Data
@Builder
public class ApiFacturaRequest {

    @JsonProperty("cuit")
    private String cuit;

    @JsonProperty("ptoVta")
    private Integer ptoVta;

    @JsonProperty("cbteTipo")
    private Integer cbteTipo;

    @JsonProperty("concepto")
    private Integer concepto;

    @JsonProperty("docTipo")
    private Integer docTipo;

    @JsonProperty("docNro")
    private String docNro;

    @JsonProperty("cbteFch")
    private String cbteFch;

    @JsonProperty("condicionIvaReceptor")
    private Integer condicionIvaReceptor;

    @JsonProperty("impNeto")
    private BigDecimal impNeto;

    @JsonProperty("impIVA")
    private BigDecimal impIVA;

    @JsonProperty("impTrib")
    private BigDecimal impTrib;

    @JsonProperty("impTotal")
    private BigDecimal impTotal;

    @JsonProperty("monId")
    private String monId;

    @JsonProperty("monCotiz")
    private BigDecimal monCotiz;

    @JsonProperty("iva")
    private List<IvaDetalle> iva;

    @JsonProperty("tributos")
    private List<TributoDetalle> tributos;

    @JsonProperty("cbtesAsoc")
    private List<CbteAsociado> cbtesAsoc;
}
