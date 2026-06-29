package com.pos.factura.repository.dbtpviv;

import com.pos.factura.dto.EventoResponse;
import com.pos.factura.entity.dbtpviv.Evento;
import com.pos.factura.entity.dbtpviv.EventoId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventoRepository
        extends JpaRepository<Evento, EventoId> {

    // =========================================
    // FIND POR CLAVE
    // =========================================
    Evento findByCajaZAndIdEvento(
            Long cajaZ,
            Long idEvento
    );

    // =========================================
    // FIND POR CLAVE y oder
    // =========================================
/*    EventoResponse findTopByOrderByCajaZAndIdIdEventoDesc(
    );*/

    // =========================================
    // FIND POR NRO TICKET
    // =========================================
    List<Evento> findByNroTicket(
            Integer nroTicket
    );

    Optional<Evento> findTopByOrderByIdEventoDesc();
}