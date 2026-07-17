package com.pos.factura.service;

import com.pos.factura.dto.EventoResponse;
import com.pos.factura.entity.dbtpviv.EvCont;
import com.pos.factura.entity.dbtpviv.Evento;
import com.pos.factura.repository.dbtpviv.EvContRepository;
import com.pos.factura.repository.dbtpviv.EventoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;
    private final EvContRepository evcontRepository;
    private static final Logger log = LoggerFactory.getLogger(EventoService.class);

    public EventoService(EventoRepository repository, EvContRepository evcontRepository) {
        this.eventoRepository = repository;
        this.evcontRepository =evcontRepository;
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
        Long cajaZ = Long.valueOf(100190);
        Long id_evento =   Long.valueOf(62);
        Evento ticket = eventoRepository.findByCajaZAndIdEvento(cajaZ, id_evento);
                //findTopByOrderByCajaZAndIdIdEventoDesc();
/*                .orElseThrow(() -> new NoSuchElementException(
                        "No se encontró el ticket ID: "));*/
        //     ticketRepository.findConPagosById(ticketId);
        return ticket;
        //return procesarFacturacion(ticket);
    }
    @Transactional("dbtpvivTransactionManager")
    public List<EvCont> buscarEvCont(Long cajaZ,Long idEvento) {
        log.info("Iniciando la busqueda del ev_cont: {}");
//        Long cajaZ = Long.valueOf(1100172);
//        Long id_evento =   Long.valueOf(2);
        List<EvCont>  evCont= evcontRepository.findByCajaZAndIdEvento(cajaZ,idEvento);
        //findTopByOrderByCajaZAndIdIdEventoDesc();
/*                .orElseThrow(() -> new NoSuchElementException(
                        "No se encontró el ticket ID: "));*/
        //     ticketRepository.findConPagosById(ticketId);
        return evCont;

    }
}