package com.pos.factura.entity.posfe;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

/**
 * Línea de ítem dentro de un ticket del POS.
 * Tabla: ticket_items (posFE)
 *
 * El artículo vive en DBTPVIV — no se puede usar @ManyToOne cross-database.
 * Se guarda codInterno (INT) para buscarlo en ArticuloRepository cuando se necesite.
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private TicketEntity ticket;

    /**
     * Clave del artículo en la tabla articulo de DBTPVIV (campo COD_INTERNO).
     * No se usa @ManyToOne porque articulo y ticket_items están en bases distintas.
     * Usar ArticuloRepository.findActivoByCodInterno() para obtener el artículo.
     */
    @Column(name = "producto_id", nullable = false)
    private Integer codInterno;

    @Column(name = "cantidad", nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad;

    /**
     * Precio unitario sin IVA al momento de la venta.
     * Se almacena para preservar el precio histórico (puede diferir del catálogo).
     */
    @Column(name = "precio_unitario_sin_iva", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitarioSinIva;

    /**
     * Alícuota de IVA en porcentaje (0, 10.5 o 21).
     * Se convierte desde articulo.COD_IVA al cerrar el ticket.
     */
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
