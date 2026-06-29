package com.pos.factura.entity.dbtpviv;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clase de clave primaria compuesta de Evento, usada con @IdClass.
 *
 * IMPORTANTE: con @IdClass esta clase NO debe llevar @Embeddable ni @Column.
 * Los nombres de los campos (cajaZ, idEvento) deben coincidir exactamente
 * con los nombres de los campos @Id en Evento.java — eso es lo que valida
 * Hibernate al arrancar.
 */
public class EventoId implements Serializable {

    private Long cajaZ;
    private Long idEvento;

    public EventoId() {
    }

    public EventoId(Long cajaZ, Long idEvento) {
        this.cajaZ = cajaZ;
        this.idEvento = idEvento;
    }

    public Long getCajaZ() { return cajaZ; }
    public void setCajaZ(Long cajaZ) { this.cajaZ = cajaZ; }

    public Long getIdEvento() { return idEvento; }
    public void setIdEvento(Long idEvento) { this.idEvento = idEvento; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventoId)) return false;
        EventoId that = (EventoId) o;
        return Objects.equals(cajaZ, that.cajaZ)
                && Objects.equals(idEvento, that.idEvento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cajaZ, idEvento);
    }
}