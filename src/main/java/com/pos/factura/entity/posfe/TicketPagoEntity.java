package com.pos.factura.entity.posfe;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

/**
 * Entidad JPA que representa una forma de pago de un ticket.
 * Tabla: `ticket_pagos`
 *
 * Un ticket puede tener múltiples formas de pago (ej: efectivo + tarjeta).
 */
@Getter
@Setter
@Entity
@Table(name = "ticket_pagos")
public class TicketPagoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private TicketEntity ticket;

    /**
     * Descripción libre: "Efectivo", "VISA DB", "MasterCard", "MercadoPago", "Transferencia"
     */
    @Column(name = "descripcion", nullable = false, length = 100)
    private String descripcion;

    @Column(name = "importe", nullable = false, precision = 12, scale = 2)
    private BigDecimal importe;
}
