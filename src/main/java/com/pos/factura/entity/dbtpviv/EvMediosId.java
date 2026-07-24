package com.pos.factura.entity.dbtpviv;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Clave primaria compuesta de la tabla ev_medios (DBTPVIV).
 * PRIMARY KEY (CAJA_Z, ID_EVENTO, POSICION)
 *
 * Se usa con @EmbeddedId en EvMedios.
 * Los tipos coinciden con los de la tabla:
 *   CAJA_Z    INT(11)    → Integer
 *   ID_EVENTO INT(11)    → Integer
 *   POSICION  TINYINT(4) → Byte
 */
@Embeddable
@Data
public class EvMediosId implements Serializable {

    @Column(name = "CAJA_Z")
    private Long cajaZ;

    @Column(name = "ID_EVENTO")
    private Long idEvento;

    @Column(name = "POSICION")
    private Byte posicion;

    public EvMediosId() {}

    public EvMediosId(Long cajaZ, Long idEvento, Byte posicion) {
        this.cajaZ    = cajaZ;
        this.idEvento = idEvento;
        this.posicion = posicion;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EvMediosId)) return false;
        EvMediosId that = (EvMediosId) o;
        return Objects.equals(cajaZ,    that.cajaZ)
            && Objects.equals(idEvento, that.idEvento)
            && Objects.equals(posicion, that.posicion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cajaZ, idEvento, posicion);
    }
}
