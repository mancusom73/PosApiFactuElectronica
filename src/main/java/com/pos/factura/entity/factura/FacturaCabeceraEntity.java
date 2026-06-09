package com.pos.factura.entity.factura;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Tabla: factura_cabecera
 *
 * Registra exactamente los datos del JSON enviado a la API de factura
 * electrónica de AFIP: datos del comprobante y datos del cliente.
 * También almacena la respuesta recibida (CAE, pdf, qr).
 *
 * Es el registro de auditoría completo de cada operación con AFIP.
 */
@Getter
@Setter
@Entity
@Table(name = "factura_cabecera",
        indexes = {
            @Index(name = "idx_fc_fecha",           columnList = "fecha"),
            @Index(name = "idx_fc_tipo",            columnList = "tipo_comprobante"),
            @Index(name = "idx_fc_doc_cliente",     columnList = "cliente_documento_nro"),
            @Index(name = "idx_fc_estado",          columnList = "estado"),
            @Index(name = "idx_fc_cae",             columnList = "cae")
        })
public class FacturaCabeceraEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // ── Comprobante ──────────────────────────────────────────────────────────

    /** FACTURA A / FACTURA B / FACTURA C / NOTA DE CREDITO B / etc. */
    @Column(name = "tipo_comprobante", nullable = false, length = 30)
    private String tipoComprobante;

    /** Número de punto de venta habilitado en AFIP */
    @Column(name = "punto_venta", nullable = false, length = 10)
    private String puntoVenta;

    /** Número de comprobante asignado por AFIP (lo devuelve la API) */
    @Column(name = "numero_comprobante", length = 20)
    private String numeroComprobante;

    /** Fecha del comprobante en formato dd/MM/yyyy (tal como fue enviada) */
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    /** Fecha de vencimiento del pago */
    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    /** PES = Pesos Argentinos, DOL = Dólares */
    @Column(name = "moneda", length = 5)
    private String moneda;

    /** Cotización: 1 para pesos, valor del día para otras monedas */
    @Column(name = "cotizacion", precision = 10, scale = 4)
    private BigDecimal cotizacion;

    /** V = Venta */
    @Column(name = "operacion", length = 2)
    private String operacion;

    @Column(name = "rubro", length = 100)
    private String rubro;

    // ── Totales del comprobante ──────────────────────────────────────────────

    @Column(name = "neto_gravado", precision = 12, scale = 2)
    private BigDecimal netoGravado;

    @Column(name = "iva_total", precision = 12, scale = 2)
    private BigDecimal ivaTotal;

    @Column(name = "exentos", precision = 12, scale = 2)
    private BigDecimal exentos;

    @Column(name = "bonificacion", precision = 12, scale = 2)
    private BigDecimal bonificacion;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    // ── Cliente (copiados del JSON para auditoria independiente) ─────────────

    /** DNI / CUIT / CUIL / PASAPORTE */
    @Column(name = "cliente_documento_tipo", length = 20)
    private String clienteDocumentoTipo;

    @Column(name = "cliente_documento_nro", length = 20)
    private String clienteDocumentoNro;

    @Column(name = "cliente_razon_social", length = 200)
    private String clienteRazonSocial;

    @Column(name = "cliente_email", length = 150)
    private String clienteEmail;

    @Column(name = "cliente_domicilio", length = 250)
    private String clienteDomicilio;

    /** Código de provincia AFIP: 1=CABA, 13=Córdoba, etc. */
    @Column(name = "cliente_provincia", length = 5)
    private String clienteProvincia;

    /** CF = Consumidor Final / RI = Resp. Inscripto / MO = Monotributista */
    @Column(name = "cliente_condicion_iva", length = 5)
    private String clienteCondicionIva;

    @Column(name = "cliente_envia_por_mail", columnDefinition = "CHAR(1)")
    private String clienteEnviaPorMail;

    // ── Respuesta de AFIP ────────────────────────────────────────────────────

    /**
     * PENDIENTE = antes de llamar a la API
     * ENVIADO   = JSON enviado, esperando respuesta
     * APROBADO  = CAE obtenido
     * RECHAZADO = AFIP rechazó el comprobante
     * ERROR     = error de comunicación
     */
    @Column(name = "estado", nullable = false, length = 15)
    private String estado = "PENDIENTE";

    /** Código de Autorización Electrónico devuelto por AFIP */
    @Column(name = "cae", length = 20)
    private String cae;

    @Column(name = "cae_vencimiento")
    private LocalDate caeVencimiento;

    /** URL del PDF del comprobante */
    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    /** PDF del comprobante en Base64 (puede ser nulo si se prefiere la URL) */
    @Column(name = "pdf_base64", columnDefinition = "MEDIUMTEXT")
    private String pdfBase64;

    /** QR code de AFIP */
    @Column(name = "afip_qr", length = 500)
    private String afipQr;

    @Column(name = "afip_codigo_barras", length = 100)
    private String afipCodigoBarras;

    /** Descripción del error en caso de rechazo o falla de comunicación */
    @Column(name = "error_descripcion", length = 1000)
    private String errorDescripcion;

    // ── Trazabilidad ─────────────────────────────────────────────────────────

    /** Número del ticket del POS que originó esta factura */
    @Column(name = "numero_ticket_pos", length = 20)
    private String numeroTicketPos;

    @Column(name = "fecha_envio_api")
    private LocalDateTime fechaEnvioApi;

    @Column(name = "fecha_respuesta_api")
    private LocalDateTime fechaRespuestaApi;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    // ── Relaciones ───────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "cabecera", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("orden ASC")
    private List<FacturaDetalleEntity> detalle;

    @OneToMany(mappedBy = "cabecera", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FacturaMedioPagoEntity> mediosPago;

    @PrePersist
    protected void onCreate() {
        fechaAlta = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaModificacion = LocalDateTime.now();
    }
}
