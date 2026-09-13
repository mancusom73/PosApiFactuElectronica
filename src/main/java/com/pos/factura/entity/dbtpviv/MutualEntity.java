package com.pos.factura.entity.dbtpviv;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entidad JPA que representa la tabla `mutuales` de DBTPVIV.
 * Motor MyISAM — sin FK ni transacciones.
 */
@Getter
@Setter
@Entity
@Table(name = "mutuales")
public class MutualEntity {

    @Id
    @Column(name = "COD_MUTUAL", nullable = false, columnDefinition = "TINYINT(4)")
    private Byte codMutual;

    @Column(name = "DES_MUTUAL", length = 11)
    private String desMutual;
}
