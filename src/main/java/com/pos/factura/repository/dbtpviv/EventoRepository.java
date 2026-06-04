package com.pos.factura.repository.dbtpviv;

import com.pos.factura.entity.dbtpviv.Evento;
import com.pos.factura.entity.dbtpviv.EventoId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventoRepository
        extends JpaRepository<Evento, EventoId> {

    // =========================================
    // FIND POR CLAVE
    // =========================================
    Evento findByIdCajaZAndIdIdEvento(
            Integer cajaZ,
            Integer idEvento
    );

    // =========================================
    // FIND POR NRO TICKET
    // =========================================
    List<Evento> findByNroTicket(
            Integer nroTicket
    );
}