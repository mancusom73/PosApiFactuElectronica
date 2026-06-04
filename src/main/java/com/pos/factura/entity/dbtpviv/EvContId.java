package com.pos.factura.entity.dbtpviv;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class EvContId implements Serializable {

    @Column(name = "CAJA_Z")
    private Integer cajaZ;

    @Column(name = "ID_EVENTO")
    private Integer idEvento;

    @Column(name = "POSICION")
    private Short posicion;

    public EvContId() {
    }

    public EvContId(
            Integer cajaZ,
            Integer idEvento,
            Short posicion
    ) {
        this.cajaZ = cajaZ;
        this.idEvento = idEvento;
        this.posicion = posicion;
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

    public Short getPosicion() {
        return posicion;
    }

    public void setPosicion(Short posicion) {
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