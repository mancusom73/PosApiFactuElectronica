package com.pos.factura.entity.dbtpviv;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entidad JPA que representa la tabla `t_compra` de DBTPVIV.
 * Motor MyISAM — sin FK ni transacciones.
 */
@Getter
@Setter
@Entity
@Table(name = "t_compra")
public class TCompraEntity {

    @Id
    @Column(name = "COD_T_COMPRA", nullable = false, columnDefinition = "TINYINT(4)")
    private Byte codTCompra;

    @Column(name = "DES_T_COMPRA", length = 11)
    private String desTCompra;
}
