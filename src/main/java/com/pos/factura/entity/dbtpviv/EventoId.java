package com.pos.factura.entity.dbtpviv;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class EventoId implements Serializable {

    @Column(name = "CAJA_Z")
    private Integer cajaZ;

    @Column(name = "ID_EVENTO")
    private Integer idEvento;

    public EventoId() {
    }

    public EventoId(
            Integer cajaZ,
            Integer idEvento
    ) {
        this.cajaZ = cajaZ;
        this.idEvento = idEvento;
    }

    public Integer getCajaZ() {
        return cajaZ;
    }

    public void setCajaZ(Integer cajaZ) {
        this.cajaZ = cajaZ;
    }

    public Integer getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(Integer idEvento) {
        this.idEvento = idEvento;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof EventoId)) {
            return false;
        }

        EventoId that = (EventoId) o;

        return Objects.equals(cajaZ, that.cajaZ)
                && Objects.equals(idEvento, that.idEvento);
    }

    @Override
    public int hashCode() {

        return Objects.hash(cajaZ, idEvento);
    }
}