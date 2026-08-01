package com.pos.factura.entity.dbtpviv;

import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

@Data
public class EvBPromoId implements Serializable {

    private Long cajaZ;
    private Long idEvento;

    public EvBPromoId() {
    }

    public EvBPromoId(
            Long cajaZ,
            Long idEvento
    ) {
        this.cajaZ = cajaZ;
        this.idEvento = idEvento;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof EvBPromoId)) {
            return false;
        }

        EvBPromoId that = (EvBPromoId) o;

        return Objects.equals(cajaZ, that.cajaZ)
                && Objects.equals(idEvento, that.idEvento);
    }

    @Override
    public int hashCode() {

        return Objects.hash(
                cajaZ,
                idEvento
        );
    }
}
