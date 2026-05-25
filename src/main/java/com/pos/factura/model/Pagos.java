package com.pos.factura.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
public class Pagos {

    @JsonProperty("formas_pago")
    private List<FormaPago> formasPago;

    @JsonProperty("total")
    private BigDecimal total;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormaPago {
        /**
         * Descripción libre: "Efectivo", "VISA DB", "MercadoPago", "Transferencia"
         */
        @JsonProperty("descripcion")
        private String descripcion;

        @JsonProperty("importe")
        private BigDecimal importe;
    }
}
