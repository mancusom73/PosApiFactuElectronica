package com.pos.factura.entity.dbtpviv;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entidad JPA que representa la tabla `articulo` de DBTPVIV.
 *
 * Notas de compatibilidad MySQL 5.1:
 *  - Motor MyISAM: no soporta transacciones ni FK, solo lectura/escritura simple.
 *  - Charset latin1: Hibernate lo maneja transparentemente.
 *  - Clave primaria compuesta: (COD_INTERNO, CODIGO_BARRA) → usa @IdClass ArticuloId.
 *  - Campos CHAR: se mapean con columnDefinition para evitar error de validación.
 *  - COD_IVA y COD_EXENTO son TINYINT(4): se mapean como Integer.
 */
@Getter
@Setter
@Entity
@IdClass(ArticuloId.class)
@Table(name = "articulo")
public class ArticuloEntity {

    // ── Clave primaria compuesta ──────────────────────────────────────────────

    @Id
    @Column(name = "COD_INTERNO", nullable = false, columnDefinition = "INT(11)")
    private Integer codInterno;

    @Id
    @Column(name = "CODIGO_BARRA", nullable = false, length = 16)
    private String codigoBarra;

    // ── Resto de columnas ─────────────────────────────────────────────────────

    @Column(name = "NOMBRE", length = 30)
    private String nombre;

    @Column(name = "PRECIO_SIN_IVA")
    private Double precioSinIva;

    @Column(name = "IMPUESTO_INTERNO")
    private Double impuestoInterno;

    /** S = gravado, N = no gravado */
    @Column(name = "GRAVADO", columnDefinition = "CHAR(1)")
    private String gravado;

    /** S = habilitado, N = deshabilitado */
    @Column(name = "HABILITADO", columnDefinition = "CHAR(1)")
    private String habilitado;

    @Column(name = "MARCA", length = 15)
    private String marca;

    @Column(name = "UNIDAD", columnDefinition = "CHAR(2)")
    private String unidad;

    @Column(name = "CONTENIDO", length = 4)
    private String contenido;

    /**
     * Código de alícuota IVA interno de DBTPVIV.
     * Mapear a alícuota AFIP (0, 10.5, 21) en el servicio según tabla de la empresa.
     */
    @Column(name = "COD_IVA", columnDefinition = "TINYINT(4)")
    private Integer codIva;

    /**
     * Código de condición de exención IVA.
     */
    @Column(name = "COD_EXENTO", columnDefinition = "TINYINT(4)")
    private Integer codExento;

    @Column(name = "COD_INTERNO_ALFA", length = 20)
    private String codInternoAlfa;

    @Column(name = "COD_CLASIFICACION", length = 35)
    private String codClasificacion;
}
