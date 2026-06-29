package com.pos.factura.service;

import com.pos.factura.dto.EventoResponse;
import com.pos.factura.entity.dbtpviv.Evento;
import com.pos.factura.repository.dbtpviv.EventoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;
    private static final Logger log = LoggerFactory.getLogger(EventoService.class);

    public EventoService(EventoRepository repository) {
        this.eventoRepository = repository;
    }

   /* public Evento buscar(Integer cajaZ,Integer idEvento) {
        return eventoRepository
                .findByIdCajaZAndIdIdEvento(
                        cajaZ,
                        idEvento
                );
    }*/

    @Transactional("dbtpvivTransactionManager")
    public Evento buscarEvento() {
        log.info("Iniciando la busqueda del evento: {}");
        Long cajaZ = Long.valueOf(1100172);
        Long id_evento =   Long.valueOf(2);
        Evento ticket = eventoRepository.findByCajaZAndIdEvento(cajaZ, id_evento);
                //findTopByOrderByCajaZAndIdIdEventoDesc();
/*                .orElseThrow(() -> new NoSuchElementException(
                        "No se encontró el ticket ID: "));*/
        //     ticketRepository.findConPagosById(ticketId);
        return ticket;
        //return procesarFacturacion(ticket);
    }
}