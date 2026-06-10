package com.pos.factura.entity.posfe;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

/**
 * Entidad JPA que representa cada línea de ítem dentro de un ticket del POS.
 * Tabla: `ticket_items`
 */
@Getter
@Setter
@Entity
@Table(name = "ticket_items")
public class TicketItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // Relación N:1 — muchos ítems pertenecen a un ticket
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private TicketEntity ticket;

    // Relación N:1 — el ítem referencia al producto del catálogo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private ProductoEntity producto;

    @Column(name = "cantidad", nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad;

    /**
     * Precio unitario sin IVA al momento de la venta.
     * Se almacena para preservar el precio histórico (puede diferir del catálogo).
     */
    @Column(name = "precio_unitario_sin_iva", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitarioSinIva;

    @Column(name = "alicuota_iva", nullable = false, precision = 5, scale = 1)
    private BigDecimal alicuotaIva;

    /**
     * Descuento porcentual aplicado a este ítem: 0.00 a 100.00
     */
    @Column(name = "bonificacion_porcentaje", precision = 5, scale = 2)
    private BigDecimal bonificacionPorcentaje = BigDecimal.ZERO;

    @Column(name = "leyenda", length = 200)
    private String leyenda = "";

    @Column(name = "afecta_stock", columnDefinition = "CHAR(1)")
    private String afectaStock = "S";

    /**
     * Número de orden dentro del ticket (para mantener el orden de impresión)
     */
    @Column(name = "orden")
    private Integer orden;
}
