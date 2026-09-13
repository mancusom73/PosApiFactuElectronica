package com.pos.factura.entity.dbtpviv;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entidad JPA que representa la tabla `medios` de DBTPVIV.
 * Motor MyISAM — sin FK ni transacciones.
 */
@Getter
@Setter
@Entity
@Table(name = "medios")
public class MedioEntity {

    @Id
    @Column(name = "COD_MEDIO", nullable = false, columnDefinition = "TINYINT(4)")
    private Byte codMedio;

    @Column(name = "NOMBRE", length = 8)
    private String nombre;

    @Column(name = "VUELTO", columnDefinition = "TINYINT(4)")
    private Byte vuelto;

    @Column(name = "NUM_AUT", length = 1)
    private String numAut;

    @Column(name = "MUTUAL", length = 1)
    private String mutual;

    @Column(name = "TARJ_CRED", length = 1)
    private String tarjCred;

    @Column(name = "T_COMPRA", length = 1)
    private String tCompra;

    @Column(name = "EFECTIVO", length = 1)
    private String efectivo;

    @Column(name = "CTACTE", length = 1)
    private String ctacte;

    @Column(name = "CHEQUE", length = 1)
    private String cheque;

    @Column(name = "TIPO", columnDefinition = "TINYINT(4)")
    private Byte tipo;

    @Column(name = "ID_REGISTRO", columnDefinition = "TINYINT(4)")
    private Byte idRegistro;
}
