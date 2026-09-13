package com.pos.factura.entity.dbtpviv;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entidad JPA que representa la tabla `tarjetas` de DBTPVIV.
 * Motor MyISAM — sin FK ni transacciones.
 */
@Getter
@Setter
@Entity
@Table(name = "tarjetas")
public class TarjetaEntity {

    @Id
    @Column(name = "COD_TARJETA", nullable = false, columnDefinition = "SMALLINT(6)")
    private Short codTarjeta;

    @Column(name = "NOMBRE", length = 10, nullable = false)
    private String nombre;
}
