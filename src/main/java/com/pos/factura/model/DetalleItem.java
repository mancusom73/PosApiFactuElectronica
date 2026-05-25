package com.pos.factura.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class DetalleItem {

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.0", inclusive = false, message = "La cantidad debe ser mayor a 0")
    @JsonProperty("cantidad")
    private BigDecimal cantidad;

    /**
     * "S" o "N" — si este item afecta el stock
     */
    @JsonProperty("afecta_stock")
    private String afectaStock = "S";

    @JsonProperty("bonificacion_porcentaje")
    private BigDecimal bonificacionPorcentaje = BigDecimal.ZERO;

    @JsonProperty("leyenda")
    private String leyenda = "";

    @Valid
    @NotNull(message = "El producto es obligatorio en cada item")
    @JsonProperty("producto")
    private Producto producto;
}
