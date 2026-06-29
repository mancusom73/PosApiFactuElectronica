package com.pos.factura.entity.dbtpviv;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Data
public class EvContId implements Serializable {

    private Long cajaZ;
    private Long idEvento;
    private Short posicion;

    public EvContId() {
    }

    public EvContId(
            Long cajaZ,
            Long idEvento,
            Short posicion
    ) {
        this.cajaZ = cajaZ;
        this.idEvento = idEvento;
        this.posicion = posicion;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof EvContId)) {
            return false;
        }

        EvContId that = (EvContId) o;

        return Objects.equals(cajaZ, that.cajaZ)
                && Objects.equals(idEvento, that.idEvento)
                && Objects.equals(posicion, that.posicion);
    }

    @Override
    public int hashCode() {

        return Objects.hash(
                cajaZ,
                idEvento,
                posicion
        );
    }
}