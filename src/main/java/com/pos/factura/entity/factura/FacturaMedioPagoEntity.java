package com.pos.factura.entity.factura;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Tabla: factura_medios_pago
 *
 * Registra cada forma de pago del JSON enviado a la API AFIP.
 * Corresponde al array "formas_pago" dentro del objeto "pagos" del comprobante.
 */
@Getter
@Setter
@Entity
@Table(name = "factura_medios_pago",
        indexes = {
            @Index(name = "idx_fmp_cabecera", columnList = "cabecera_id")
        })
public class FacturaMedioPagoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Relación con la cabecera de la factura */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cabecera_id", nullable = false)
    private FacturaCabeceraEntity cabecera;

    /**
     * Descripción del medio de pago — exactamente como fue enviada a la API.
     * Ej: "Efectivo", "VISA DB", "MasterCard", "MercadoPago", "Transferencia"
     */
    @Column(name = "descripcion", nullable = false, length = 100)
    private String descripcion;

    @Column(name = "importe", nullable = false, precision = 12, scale = 2)
    private BigDecimal importe;
}
