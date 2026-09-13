package com.pos.factura.service;

import com.pos.factura.dto.FacturaResponse;
import com.pos.factura.entity.dbtpviv.EvCont;
import com.pos.factura.entity.dbtpviv.EvMedios;
import com.pos.factura.entity.dbtpviv.Evento;
import com.pos.factura.entity.posfe.TicketEntity;
import com.pos.factura.mapper.TicketMapper;
import com.pos.factura.repository.TicketRepository;
import com.pos.factura.repository.dbtpviv.EvContRepository;
import com.pos.factura.repository.dbtpviv.EventoRepository;
import com.pos.factura.repository.dbtpviv.EvMediosRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;
    private final EvContRepository evcontRepository;
    private final EvMediosRepository evmediosRepository;
    private final TicketFacturacionService ticketFacturacionService;
    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private static final Logger log = LoggerFactory.getLogger(EventoService.class);

    public EventoService(EventoRepository repository, EvContRepository evcontRepository, EvMediosRepository evmediosRepository, FacturaService facturaService, TicketFacturacionService ticketFacturacionService, TicketRepository ticketRepository, TicketMapper ticketMapper) {
        this.eventoRepository = repository;
        this.evcontRepository =evcontRepository;
        this.evmediosRepository = evmediosRepository;
        this.ticketFacturacionService = ticketFacturacionService;
        this.ticketRepository = ticketRepository;
        this.ticketMapper = ticketMapper;
    }

   /* public Evento buscar(Integer cajaZ,Integer idEvento) {
        return eventoRepository
                .findByIdCajaZAndIdIdEvento(
                        cajaZ,
                        idEvento
                );
    }*/

    @Transactional("dbtpvivTransactionManager")
    public FacturaResponse buscarEvento(Long cajaZ, Long idEvento) {
        log.info("Iniciando la busqueda del evento: {}");
        Evento ticket = eventoRepository.findByCajaZAndIdEvento(cajaZ, idEvento);
        List<EvCont> evCont = this.buscarEvCont(ticket.getCajaZ(), ticket.getIdEvento());
        List<EvMedios> evMedios = this.buscarEvMedios(ticket.getCajaZ(), ticket.getIdEvento());
        //TODO MAPEAR TODO DESDE DBTPVIV A posfe
        TicketEntity ticketCabecera = TicketMapper.mapEventoTicketEntity(ticket, evCont);
        ticketCabecera.setPagos(ticketMapper.mapEvMediosToPagos(evMedios, ticketCabecera));
        ticketCabecera = ticketRepository.save(ticketCabecera);

        return ticketFacturacionService.facturar(ticketCabecera);
    }
    @Transactional("dbtpvivTransactionManager")
    public List<EvCont> buscarEvCont(Long cajaZ,Long idEvento) {
        log.info("Iniciando la busqueda del ev_cont: {}");
        List<EvCont>  evCont= evcontRepository.findByCajaZAndIdEvento(cajaZ,idEvento);
        return evCont;

    }

    @Transactional("dbtpvivTransactionManager")
    public List<EvMedios> buscarEvMedios(Long cajaZ,Long idEvento) {
        log.info("Iniciando la busqueda del ev_medios: {}");
        List<EvMedios>  evmedios= evmediosRepository.findByEvento(cajaZ,idEvento);
        //findTopByOrderByCajaZAndIdIdEventoDesc();
/*                .orElseThrow(() -> new NoSuchElementException(
                        "No se encontró el ticket ID: "));*/
        //     ticketRepository.findConPagosById(ticketId);
        return evmedios;

    }
}