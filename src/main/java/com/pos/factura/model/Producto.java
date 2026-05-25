package com.pos.factura.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class Producto {

    @NotBlank(message = "La descripción del producto es obligatoria")
    @JsonProperty("descripcion")
    private String descripcion;

    @JsonProperty("codigo")
    private String codigo;

    @NotNull(message = "El precio unitario sin IVA es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    @JsonProperty("precio_unitario_sin_iva")
    private BigDecimal precioUnitarioSinIva;

    /**
     * Alícuota de IVA: 0, 10.5 o 21
     */
    @NotNull(message = "La alícuota de IVA es obligatoria")
    @JsonProperty("alicuota")
    private Double alicuota;

    /**
     * Código unidad de medida AFIP. 7 = Unidades (más común)
     */
    @JsonProperty("unidad_medida")
    private String unidadMedida = "7";

    @JsonProperty("unidad_bulto")
    private String unidadBulto = "1";

    @JsonProperty("lista_precios")
    private String listaPrecios = "LISTA GENERAL";

    @JsonProperty("impuestos_internos_alicuota")
    private Double impuestosInternosAlicuota = 0.0;
}
