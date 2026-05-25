package com.pos.factura.entity;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entidad JPA que representa la cabecera de un ticket del punto de venta.
 * Tabla: `tickets`
 *
 * Cada ticket puede tener N ítems (TicketItemEntity) y N formas de pago (TicketPagoEntity).
 * Cuando se emite la factura, se almacena el CAE y número de comprobante devueltos por AFIP.
 */
@Getter
@Setter
@Entity
@Table(name = "tickets",
        indexes = {
            @Index(name = "idx_ticket_fecha", columnList = "fecha"),
            @Index(name = "idx_ticket_cliente", columnList = "cliente_id"),
            @Index(name = "idx_ticket_estado", columnList = "estado")
        })
public class TicketEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Número correlativo del ticket generado por el POS.
     */
    @Column(name = "numero_ticket", nullable = false, unique = true, length = 20)
    private String numeroTicket;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    // Relación N:1 — el ticket pertenece a un cliente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private ClienteEntity cliente;

    /**
     * Tipo de comprobante a emitir:
     * FACTURA_A, FACTURA_B, FACTURA_C, NOTA_CREDITO_B, etc.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante", nullable = false, length = 30)
    private TipoComprobante tipoComprobante;

    /**
     * Estado del ticket:
     * PENDIENTE   → generado por el POS, aún no facturado
     * FACTURADO   → CAE obtenido exitosamente
     * ERROR       → intento de facturación fallido
     * ANULADO     → ticket anulado (genera NC)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    private EstadoTicket estado = EstadoTicket.PENDIENTE;

    @Column(name = "moneda", length = 5)
    private String moneda = "PES";

    @Column(name = "cotizacion", precision = 10, scale = 4)
    private BigDecimal cotizacion = BigDecimal.ONE;

    // --- Totales (calculados al cerrar el ticket) ---

    @Column(name = "neto_gravado", precision = 12, scale = 2)
    private BigDecimal netoGravado = BigDecimal.ZERO;

    @Column(name = "iva_total", precision = 12, scale = 2)
    private BigDecimal ivaTotal = BigDecimal.ZERO;

    @Column(name = "exentos", precision = 12, scale = 2)
    private BigDecimal exentos = BigDecimal.ZERO;

    @Column(name = "bonificacion", precision = 12, scale = 2)
    private BigDecimal bonificacion = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    // --- Datos de la factura emitida (se completan tras respuesta de AFIP) ---

    @Column(name = "cae", length = 20)
    private String cae;

    @Column(name = "cae_vencimiento")
    private LocalDate caeVencimiento;

    @Column(name = "comprobante_nro", length = 30)
    private String comprobanteNro;

    @Column(name = "afip_qr", length = 500)
    private String afipQr;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    @Column(name = "fecha_facturacion")
    private LocalDateTime fechaFacturacion;

    @Column(name = "error_descripcion", length = 1000)
    private String errorDescripcion;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    // --- Relaciones ---

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("orden ASC")
    private List<TicketItemEntity> items;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TicketPagoEntity> pagos;

    // --- Enums ---

    public enum TipoComprobante {
        FACTURA_A("FACTURA A"),
        FACTURA_B("FACTURA B"),
        FACTURA_C("FACTURA C"),
        NOTA_CREDITO_A("NOTA DE CREDITO A"),
        NOTA_CREDITO_B("NOTA DE CREDITO B"),
        NOTA_CREDITO_C("NOTA DE CREDITO C"),
        NOTA_DEBITO_A("NOTA DE DEBITO A"),
        NOTA_DEBITO_B("NOTA DE DEBITO B"),
        NOTA_DEBITO_C("NOTA DE DEBITO C");

        private final String descripcionAfip;

        TipoComprobante(String descripcionAfip) {
            this.descripcionAfip = descripcionAfip;
        }

        public String getDescripcionAfip() {
            return descripcionAfip;
        }
    }

    public enum EstadoTicket {
        PENDIENTE, FACTURADO, ERROR, ANULADO
    }

    @PrePersist
    protected void onCreate() {
        fechaAlta = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaModificacion = LocalDateTime.now();
    }
}
