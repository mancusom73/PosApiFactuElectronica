package com.pos.factura.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class Comprobante {

    /**
     * Tipo de comprobante:
     * "FACTURA A", "FACTURA B", "FACTURA C",
     * "NOTA DE CREDITO A", "NOTA DE CREDITO B", "NOTA DE CREDITO C",
     * "NOTA DE DEBITO A", "NOTA DE DEBITO B", "NOTA DE DEBITO C"
     */
    @NotBlank(message = "El tipo de comprobante es obligatorio")
    @JsonProperty("tipo")
    private String tipo;

    /**
     * Formato: dd/MM/yyyy
     */
    @NotBlank(message = "La fecha es obligatoria")
    @JsonProperty("fecha")
    private String fecha;

    @JsonProperty("punto_venta")
    private String puntoVenta;

    /**
     * "0" para que la API asigne el próximo número automáticamente
     */
    @JsonProperty("numero")
    private String numero = "0";

    /**
     * "PES" = Pesos Argentinos, "DOL" = Dólares
     */
    @JsonProperty("moneda")
    private String moneda = "PES";

    /**
     * "1" para pesos. Si es dólares, poner cotización del día.
     */
    @JsonProperty("cotizacion")
    private String cotizacion = "1";

    /**
     * "V" = Venta
     */
    @JsonProperty("operacion")
    private String operacion = "V";

    /**
     * Fecha de vencimiento del pago. Formato: dd/MM/yyyy
     */
    @JsonProperty("vencimiento")
    private String vencimiento;

    @JsonProperty("rubro")
    private String rubro = "Venta de mercadería";

    // --- Totales calculados automáticamente ---

    @JsonProperty("total")
    private BigDecimal total;

    @JsonProperty("exentos")
    private BigDecimal exentos = BigDecimal.ZERO;

    @JsonProperty("bonificacion")
    private BigDecimal bonificacion = BigDecimal.ZERO;

    // Campos internos usados para construir el JSON de la API
    // (algunos proveedores los requieren explícitamente)
    @JsonProperty("neto_gravado")
    private BigDecimal netoGravado;

    @JsonProperty("iva_total")
    private BigDecimal ivaTotal;

    // --- Detalle de items ---

    @Valid
    @NotEmpty(message = "El comprobante debe tener al menos un item")
    @JsonProperty("detalle")
    private List<DetalleItem> detalle;

    // --- Pagos ---

    @JsonProperty("pagos")
    private Pagos pagos;

    // --- Solo para Notas de Crédito/Débito ---

    @JsonProperty("comprobantes_asociados")
    private List<ComprobanteAsociado> comprobantesAsociados;

    // --- Campo interno: no se serializa al JSON de la API ---

    @JsonIgnore
    private Map<Double, BigDecimal> ivaPorAlicuota;
}
