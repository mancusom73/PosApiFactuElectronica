package com.pos.factura.entity;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad JPA que representa la tabla `productos` de la base de datos del POS.
 */
@Getter
@Setter
@Entity
@Table(name = "productos")
public class ProductoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(name = "descripcion", nullable = false, length = 250)
    private String descripcion;

    /**
     * Precio neto unitario (sin IVA).
     * AFIP exige que el precio en la factura sea sin impuestos incluidos.
     */
    @Column(name = "precio_unitario_sin_iva", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitarioSinIva;

    /**
     * Alícuota de IVA: 0, 10.5 o 21
     */
    @Column(name = "alicuota_iva", nullable = false, precision = 5, scale = 1)
    private BigDecimal alicuotaIva;

    /**
     * Código de unidad de medida AFIP. 7 = Unidades.
     */
    @Column(name = "unidad_medida", length = 10)
    private String unidadMedida = "7";

    @Column(name = "lista_precios", length = 50)
    private String listaPrecios = "LISTA GENERAL";

    @Column(name = "stock_actual", precision = 10, scale = 2)
    private BigDecimal stockActual = BigDecimal.ZERO;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @PrePersist
    protected void onCreate() {
        fechaAlta = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaModificacion = LocalDateTime.now();
    }
}
