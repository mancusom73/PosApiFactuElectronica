package com.pos.factura.entity.dbtpviv;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entidad JPA que representa la tabla `factu_mem` de DBTPVIV.
 * Motor MyISAM — sin FK ni transacciones.
 */
@Getter
@Setter
@Entity
@Table(name = "factu_mem")
public class FactuMemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false, columnDefinition = "SMALLINT(6) UNSIGNED")
    private Integer id;

    @Column(name = "STATUS", columnDefinition = "TINYINT(4)")
    private Byte status;

    @Column(name = "DESCRIPCION", length = 30)
    private String descripcion;

    @Column(name = "TIPO_DATO", columnDefinition = "SMALLINT(6)")
    private Short tipoDato;

    @Column(name = "VALOR", length = 80)
    private String valor;

    @Column(name = "FIN", columnDefinition = "TINYINT(4)")
    private Byte fin;
}
