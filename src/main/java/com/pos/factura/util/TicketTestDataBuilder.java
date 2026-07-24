package com.pos.factura.util;

import com.pos.factura.entity.dbtpviv.Evento;
import com.pos.factura.entity.posfe.TicketEntity;
import com.pos.factura.entity.posfe.TicketItemEntity;
import com.pos.factura.entity.posfe.TicketPagoEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Clase utilitaria para construir un TicketEntity con datos de prueba.
 *
 * Uso típico:
 *   // Factura B con 2 productos y pago en efectivo
 *   TicketEntity ticket = TicketTestDataBuilder.facturaB();
 *
 *   // Factura A con cliente RI
 *   TicketEntity ticket = TicketTestDataBuilder.facturaA();
 *
 *   // Nota de crédito B
 *   TicketEntity ticket = TicketTestDataBuilder.notaCreditoB();
 *
 *   // Personalizado
 *   TicketEntity ticket = TicketTestDataBuilder.builder()
 *           .tipo(TicketEntity.TipoComprobante.FACTURA_C)
 *           .codCliente(99L)
 *           .agregarItem(500, new BigDecimal("1000.00"), 21.0)
 *           .pagoEfectivo()
 *           .build();
 */
public class TicketTestDataBuilder {

    // ── Métodos de fábrica rápidos ────────────────────────────────────────────

    /**
     * Ticket de prueba para Factura B — Consumidor Final.
     * 2 ítems con IVA 21%, pago mixto efectivo + tarjeta.
     */
    public static TicketEntity facturaB() {
        return builder()
                .numeroTicket("TK-TEST-001")
                .tipo(TicketEntity.TipoComprobante.FACTURA_B)
                .codCliente(1L)
                .fecha(LocalDate.now())
                .agregarItem(1001, new BigDecimal("10000.00"), 21.0, new BigDecimal("2"))
                .agregarItem(1002, new BigDecimal("500.00"),   21.0, new BigDecimal("3"))
                .pagoEfectivo(new BigDecimal("15000.00"))
                .pagoTarjeta("VISA DB", new BigDecimal("6050.50"))
                .build();
    }

    /**
     * Ticket de prueba para Factura A — Responsable Inscripto.
     * 1 ítem con IVA 21%, pago por transferencia.
     */
    public static TicketEntity facturaA() {
        return builder()
                .numeroTicket("TK-TEST-002")
                .tipo(TicketEntity.TipoComprobante.FACTURA_A)
                .codCliente(2L)
                .fecha(LocalDate.now())
                .agregarItem(2001, new BigDecimal("80000.00"), 21.0, new BigDecimal("1"))
                .pagoTarjeta("Transferencia bancaria", new BigDecimal("96800.00"))
                .build();
    }

    /**
     * Ticket de prueba para Factura C — Monotributista.
     */
    public static TicketEntity facturaC() {
        return builder()
                .numeroTicket("TK-TEST-003")
                .tipo(TicketEntity.TipoComprobante.FACTURA_C)
                .codCliente(3L)
                .fecha(LocalDate.now())
                .agregarItem(3001, new BigDecimal("5000.00"), 0.0, new BigDecimal("1"))
                .pagoEfectivo(new BigDecimal("5000.00"))
                .build();
    }

    /**
     * Ticket de prueba para Nota de Crédito B.
     */
    public static TicketEntity notaCreditoB() {
        return builder()
                .numeroTicket("TK-TEST-NC-001")
                .tipo(TicketEntity.TipoComprobante.NOTA_CREDITO_B)
                .codCliente(1L)
                .fecha(LocalDate.now())
                .agregarItem(1001, new BigDecimal("10000.00"), 21.0, new BigDecimal("1"))
                .pagoEfectivo(new BigDecimal("12100.00"))
                .build();
    }

    /**
     * Inicia el builder para construcción personalizada.
     */
    public static Builder builder() {
        return new Builder();
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static class Builder {

        private String numeroTicket                      = "TK-TEST-" + System.currentTimeMillis();
        private TicketEntity.TipoComprobante tipo        = TicketEntity.TipoComprobante.FACTURA_B;
        private TicketEntity.EstadoTicket estado         = TicketEntity.EstadoTicket.PENDIENTE;
        private Long codCliente                          = 1L;
        private LocalDate fecha                          = LocalDate.now();
        private LocalDate fechaVencimiento               = LocalDate.now();
        private String moneda                            = "PES";
        private BigDecimal cotizacion                    = BigDecimal.ONE;
        private BigDecimal bonificacion                  = BigDecimal.ZERO;
        private java.util.List<TicketItemEntity> items   = new java.util.ArrayList<>();
        private java.util.List<TicketPagoEntity> pagos   = new java.util.ArrayList<>();

        public Builder numeroTicket(String numeroTicket) {
            this.numeroTicket = numeroTicket;
            return this;
        }

        public Builder tipo(TicketEntity.TipoComprobante tipo) {
            this.tipo = tipo;
            return this;
        }

        public Builder codCliente(Long codCliente) {
            this.codCliente = codCliente;
            return this;
        }

        public Builder fecha(LocalDate fecha) {
            this.fecha = fecha;
            this.fechaVencimiento = fecha;
            return this;
        }

        public Builder bonificacion(BigDecimal bonificacion) {
            this.bonificacion = bonificacion;
            return this;
        }

        /**
         * Agrega un ítem al ticket.
         *
         * @param codInterno           Código interno del artículo (COD_INTERNO en DBTPVIV)
         * @param precioUnitarioSinIva Precio neto sin IVA
         * @param alicuotaIva          Alícuota: 0.0, 10.5 o 21.0
         * @param cantidad             Cantidad
         */
        public Builder agregarItem(Integer codInterno,
                                   BigDecimal precioUnitarioSinIva,
                                   double alicuotaIva,
                                   BigDecimal cantidad) {
            TicketItemEntity item = new TicketItemEntity();
            item.setCodInterno(codInterno);
            item.setPrecioUnitarioSinIva(precioUnitarioSinIva);
            item.setAlicuotaIva(BigDecimal.valueOf(alicuotaIva));
            item.setCantidad(cantidad);
            item.setBonificacionPorcentaje(BigDecimal.ZERO);
            item.setAfectaStock("S");
            item.setLeyenda("");
            item.setOrden(items.size() + 1);
            this.items.add(item);
            return this;
        }

        /**
         * Agrega un ítem con cantidad 1 por defecto.
         */
        public Builder agregarItem(Integer codInterno,
                                   BigDecimal precioUnitarioSinIva,
                                   double alicuotaIva) {
            return agregarItem(codInterno, precioUnitarioSinIva, alicuotaIva, BigDecimal.ONE);
        }

        /**
         * Agrega un ítem con bonificación porcentual.
         */
        public Builder agregarItemConDescuento(Integer codInterno,
                                               BigDecimal precioUnitarioSinIva,
                                               double alicuotaIva,
                                               BigDecimal cantidad,
                                               BigDecimal bonificacionPorcentaje) {
            TicketItemEntity item = new TicketItemEntity();
            item.setCodInterno(codInterno);
            item.setPrecioUnitarioSinIva(precioUnitarioSinIva);
            item.setAlicuotaIva(BigDecimal.valueOf(alicuotaIva));
            item.setCantidad(cantidad);
            item.setBonificacionPorcentaje(bonificacionPorcentaje);
            item.setAfectaStock("S");
            item.setLeyenda("");
            item.setOrden(items.size() + 1);
            this.items.add(item);
            return this;
        }

        /**
         * Agrega una forma de pago en efectivo.
         */
        public Builder pagoEfectivo(BigDecimal importe) {
            return agregarPago("Efectivo", importe);
        }

        /**
         * Agrega una forma de pago con tarjeta.
         */
        public Builder pagoTarjeta(String descripcion, BigDecimal importe) {
            return agregarPago(descripcion, importe);
        }

        /**
         * Agrega una forma de pago genérica.
         */
        public Builder agregarPago(String descripcion, BigDecimal importe) {
            TicketPagoEntity pago = new TicketPagoEntity();
            pago.setDescripcion(descripcion);
            pago.setImporte(importe);
            this.pagos.add(pago);
            return this;
        }

        /**
         * Armado de ticket con datos de la mysql de la caja
         */
        public static TicketEntity eventos(Evento ev) {

            return builder()
                    .numeroTicket(ev.getNroTicket().toString() /*"TK-TEST-001"*/)
                    .tipo(TicketEntity.TipoComprobante.FACTURA_B)
                    .codCliente(1L)
                    .fecha(LocalDate.now())
                    .agregarItem(1001, new BigDecimal("10000.00"), 21.0, new BigDecimal("2"))
                    .agregarItem(1002, new BigDecimal("500.00"),   21.0, new BigDecimal("3"))
                    .pagoEfectivo(new BigDecimal("15000.00"))
                    .pagoTarjeta("VISA DB", new BigDecimal("6050.50"))
                    .build();
        }

        /**
         * Construye el TicketEntity con todos los valores configurados.
         * Los totales se dejan en cero para que los calcule CalculadorComprobante.
         */
        public TicketEntity build() {
            TicketEntity ticket = new TicketEntity();

            ticket.setNumeroTicket(numeroTicket);
            ticket.setTipoComprobante(tipo);
            ticket.setEstado(estado);
            ticket.setCodCliente(codCliente);
            ticket.setFecha(fecha);
            ticket.setFechaVencimiento(fechaVencimiento);
            ticket.setMoneda(moneda);
            ticket.setCotizacion(cotizacion);
            ticket.setBonificacion(bonificacion);
            ticket.setFechaAlta(LocalDateTime.now());

            // Totales en cero — los calcula CalculadorComprobante antes de enviar a AFIP
            ticket.setNetoGravado(BigDecimal.ZERO);
            ticket.setIvaTotal(BigDecimal.ZERO);
            ticket.setExentos(BigDecimal.ZERO);
            ticket.setTotal(BigDecimal.ZERO);

            // Vincular items y pagos al ticket
            for (TicketItemEntity item : items) {
                item.setTicket(ticket);
            }
            for (TicketPagoEntity pago : pagos) {
                pago.setTicket(ticket);
            }

            ticket.setItems(items);
            ticket.setPagos(pagos);

            return ticket;
        }
    }
}
