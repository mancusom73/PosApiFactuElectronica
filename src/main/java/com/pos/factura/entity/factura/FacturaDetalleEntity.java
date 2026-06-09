package com.pos.factura.entity.factura;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Tabla: factura_detalle
 *
 * Cada fila representa una línea de producto del JSON enviado a la API AFIP.
 * Registra exactamente los campos del objeto "detalle" → "producto"
 * más los campos de cantidad y bonificación del ítem.
 */
@Getter
@Setter
@Entity
@Table(name = "factura_detalle",
        indexes = {
            @Index(name = "idx_fd_cabecera", columnList = "cabecera_id"),
            @Index(name = "idx_fd_codigo",   columnList = "producto_codigo")
        })
public class FacturaDetalleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Relación con la cabecera de la factura */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cabecera_id", nullable = false)
    private FacturaCabeceraEntity cabecera;

    /** Orden del ítem en el comprobante (para mantener el orden de impresión) */
    @Column(name = "orden", nullable = false)
    private Integer orden;

    // ── Datos del ítem (campo "detalle" del JSON) ────────────────────────────

    @Column(name = "cantidad", nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad;

    /** S = afecta stock, N = no afecta */
    @Column(name = "afecta_stock", columnDefinition = "CHAR(1)")
    private String afectaStock;

    /** Descuento aplicado a esta línea (0.00 a 100.00) */
    @Column(name = "bonificacion_porcentaje", precision = 5, scale = 2)
    private BigDecimal bonificacionPorcentaje;

    /** Leyenda adicional del ítem */
    @Column(name = "leyenda", length = 200)
    private String leyenda;

    // ── Datos del producto (campo "producto" dentro de cada ítem) ────────────

    @Column(name = "producto_codigo", length = 50)
    private String productoCodigo;

    @Column(name = "producto_descripcion", nullable = false, length = 250)
    private String productoDescripcion;

    /**
     * Precio neto unitario sin IVA — exactamente como se envió a AFIP.
     * Es el precio histórico al momento de la emisión.
     */
    @Column(name = "precio_unitario_sin_iva", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitarioSinIva;

    /** Alícuota de IVA: 0, 10.5 o 21 */
    @Column(name = "alicuota_iva", nullable = false, precision = 5, scale = 1)
    private BigDecimal alicuotaIva;

    /** Código de unidad de medida AFIP: 7 = Unidades */
    @Column(name = "unidad_medida", length = 10)
    private String unidadMedida;

    @Column(name = "lista_precios", length = 50)
    private String listaPrecios;

    @Column(name = "impuestos_internos_alicuota", precision = 5, scale = 2)
    private BigDecimal impuestosInternosAlicuota;

    // ── Totales calculados de la línea (para auditoría) ──────────────────────

    /** cantidad × precio_unitario_sin_iva − descuento */
    @Column(name = "neto_linea", precision = 12, scale = 2)
    private BigDecimal netoLinea;

    /** neto_linea × (alicuota_iva / 100) */
    @Column(name = "iva_linea", precision = 12, scale = 2)
    private BigDecimal ivaLinea;

    /** neto_linea + iva_linea */
    @Column(name = "total_linea", precision = 12, scale = 2)
    private BigDecimal totalLinea;
}
