package com.pos.factura.service;

import com.pos.factura.dto.FacturaResponse;
import com.pos.factura.dto.SolicitudFacturaRequest;
import com.pos.factura.entity.dbtpviv.EvCont;
import com.pos.factura.entity.posfe.TicketEntity;
import com.pos.factura.entity.posfe.TicketEntity.EstadoTicket;
import com.pos.factura.entity.dbtpviv.ClienteEntity;
import com.pos.factura.mapper.TicketMapper;
import com.pos.factura.repository.TicketRepository;
import com.pos.factura.repository.dbtpviv.ClienteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Servicio que integra la capa de persistencia (MySQL) con el servicio
 * de facturación electrónica.
 *
 * Flujo completo:
 *   1. Leer ticket de MySQL (con cliente, ítems y pagos)
 *   2. Validar que el ticket esté en estado PENDIENTE
 *   3. Mapear TicketEntity → SolicitudFacturaRequest
 *   4. Delegar en FacturaService (calcula, valida, llama a AFIP)
 *   5. Actualizar el estado del ticket en MySQL con el resultado
 */
@Service
public class TicketFacturacionService {

    private static final Logger log = LoggerFactory.getLogger(TicketFacturacionService.class);
    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_FECHA_AFIP = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int ERROR_DESCRIPCION_MAX_LENGTH = 1000;
    private static final BigDecimal DIFERENCIA_AJUSTABLE = new BigDecimal("0.01");

    private final TicketRepository ticketRepository;
    private final ClienteRepository clienteRepository;
    private final TicketMapper ticketMapper;
    private final FacturaService facturaService;

    public TicketFacturacionService(TicketRepository ticketRepository,
                                    ClienteRepository clienteRepository,
                                    TicketMapper ticketMapper,
                                    FacturaService facturaService) {
        this.ticketRepository  = ticketRepository;
        this.clienteRepository = clienteRepository;
        this.ticketMapper      = ticketMapper;
        this.facturaService    = facturaService;
    }

    @Transactional("posfeTransactionManager")
    public FacturaResponse facturarTicket(String numeroTicket) {
        log.info("Iniciando facturación del ticket: {}", numeroTicket);
        TicketEntity ticket = ticketRepository.findConItemsByNumeroTicket(numeroTicket)
                .orElseThrow(() -> new NoSuchElementException(
                        "No se encontró el ticket: " + numeroTicket));
        ticketRepository.findConPagosByNumeroTicket(numeroTicket);
        return procesarFacturacion(ticket);
    }

    @Transactional("posfeTransactionManager")
    public FacturaResponse facturarTicketPorId(Long ticketId) {
        log.info("Iniciando facturación del ticket ID: {}", ticketId);
        TicketEntity ticket = ticketRepository.findConDetalleById(ticketId)
                .orElseThrow(() -> new NoSuchElementException(
                        "No se encontró el ticket ID: " + ticketId));
        ticketRepository.findConPagosById(ticketId);
        return procesarFacturacion(ticket);
    }

    @Transactional("posfeTransactionManager")
    public FacturaResponse facturar(TicketEntity ticket) {
        log.info("Iniciando facturación del ticket ID: {}", ticket.getId());
        return procesarFacturacion(ticket);
    }

    @Transactional("posfeTransactionManager")
    public List<FacturaResponse> facturarPendientes() {
        List<TicketEntity> pendientes = ticketRepository.findPendientesConItems();
        // Segunda pasada para cargar pagos en las mismas instancias cacheadas
        ticketRepository.findPendientesConPagos();
        log.info("Procesando {} tickets pendientes", pendientes.size());
        return pendientes.stream()
                .map(ticket -> {
                    try {
                        return procesarFacturacion(ticket);
                    } catch (Exception e) {
                        log.error("Error procesando ticket {}: {}", ticket.getNumeroTicket(), e.getMessage());
                        return FacturaResponse.builder()
                                .exitoso(false)
                                .mensaje("Error en ticket " + ticket.getNumeroTicket() + ": " + e.getMessage())
                                .build();
                    }
                })
                .collect(Collectors.toList());
    }

    private FacturaResponse procesarFacturacion(TicketEntity ticket) {
        // 2. Validar estado del ticket
        validarEstadoTicket(ticket);

        // Cargar cliente desde DBTPVIV usando codCliente del ticket
        // Si no se encuentra, se factura como Consumidor Final genérico (valores por defecto ARCA)
        ClienteEntity cliente = clienteRepository.findById(ticket.getCodCliente())
                .orElseGet(() -> clienteGenerico(ticket.getCodCliente()));
        //Ojo que hay que controlar ciertos montos para que si o si se identifique el cliente El límite de $10.000.000 para cualquier medio de pago
        ajustarTotalPorRedondeo(ticket);
        ticketRepository.save(ticket);
        try {
            SolicitudFacturaRequest solicitud = ticketMapper.toSolicitud(ticket, cliente);

            log.debug("Ticket {} mapeado: tipo={}, cliente={}, items={}",
                    ticket.getNumeroTicket(),
                    ticket.getTipoComprobante(),
                    cliente.getNombre(),
                    ticket.getItems().size());

            FacturaResponse respuesta = facturaService.emitir(solicitud, ticket.getNumeroTicket());

            if (respuesta.isExitoso()) {
                marcarFacturado(ticket, respuesta);
            } else {
                String errores = respuesta.getErrores() != null
                        ? String.join(" | ", Arrays.asList(respuesta.getErrores()))
                        : respuesta.getMensaje();
                marcarError(ticket, errores);
            }

            ticketRepository.save(ticket);
            return respuesta;

        } catch (Exception e) {
            log.error("Error inesperado facturando ticket {}: {}", ticket.getNumeroTicket(), e.getMessage());
            marcarError(ticket, e.getMessage());
            ticketRepository.save(ticket);
            throw e;
        }
    }

    /**
     * Cliente por defecto usado cuando el ticket no tiene un cliente identificado
     * en DBTPVIV. Representa un Consumidor Final genérico según lo exigido por ARCA.
     */
    private ClienteEntity clienteGenerico(Long codCliente) {
        log.warn("Cliente ID {} no encontrado en DBTPVIV. Se factura como Consumidor Final genérico", codCliente);
        ClienteEntity cliente = new ClienteEntity();
        cliente.setCodCliente(codCliente);
        cliente.setCodDocumento("D");
        cliente.setNroDocumento("0");
        cliente.setNombre("Consumidor Final");
        cliente.setDomicilio("");
        cliente.setProvincia("13");
        cliente.setCondIva("4");
        return cliente;
    }

    private void validarEstadoTicket(TicketEntity ticket) {
        if (ticket.getEstado() == EstadoTicket.FACTURADO) {
            throw new IllegalStateException(
                    "El ticket " + ticket.getNumeroTicket() + " ya fue facturado. CAE: " + ticket.getCae());
        }
        if (ticket.getEstado() == EstadoTicket.ANULADO) {
            throw new IllegalStateException(
                    "El ticket " + ticket.getNumeroTicket() + " está anulado");
        }
        if (ticket.getItems() == null || ticket.getItems().isEmpty()) {
            throw new IllegalStateException(
                    "El ticket " + ticket.getNumeroTicket() + " no tiene ítems");
        }
    }

    private void marcarFacturado(TicketEntity ticket, FacturaResponse respuesta) {
        ticket.setEstado(EstadoTicket.FACTURADO);
        ticket.setCae(respuesta.getCae());
        ticket.setComprobanteNro(respuesta.getComprobanteNro());
        ticket.setAfipQr(respuesta.getAfipQr());
        ticket.setPdfUrl(respuesta.getPdfUrl());
        ticket.setFechaFacturacion(LocalDateTime.now());
        ticket.setTotal(respuesta.getTotal());
        ticket.setErrorDescripcion(null);
        if (respuesta.getCaeVencimiento() != null) {
            ticket.setCaeVencimiento(parseCaeVencimiento(respuesta.getCaeVencimiento()));
        }
        log.info("Ticket {} FACTURADO — CAE: {}", ticket.getNumeroTicket(), respuesta.getCae());
    }

    /**
     * Si el total difiere de neto + iva + exentos por exactamente $0.01 (redondeo
     * de la respuesta de AFIP), ajusta el total a la sumatoria antes de grabar.
     */
    private void ajustarTotalPorRedondeo(TicketEntity ticket) {
        BigDecimal suma = ticket.getNetoGravado().add(ticket.getIvaTotal()).add(ticket.getExentos());
        BigDecimal diferencia = ticket.getTotal().subtract(suma).abs();
        if (diferencia.compareTo(DIFERENCIA_AJUSTABLE) == 0) {
            ticket.setTotal(suma);
        }
    }

    private LocalDate parseCaeVencimiento(String valor) {
        try {
            return LocalDate.parse(valor, FMT_FECHA_AFIP);
        } catch (DateTimeParseException ex) {
            return LocalDate.parse(valor, FMT_FECHA);
        }
    }

    private void marcarError(TicketEntity ticket, String descripcion) {
        ticket.setEstado(EstadoTicket.ERROR);
        ticket.setErrorDescripcion(truncarErrorDescripcion(descripcion));
        log.warn("Ticket {} ERROR: {}", ticket.getNumeroTicket(), descripcion);
    }

    private String truncarErrorDescripcion(String descripcion) {
        if (descripcion != null && descripcion.length() > ERROR_DESCRIPCION_MAX_LENGTH) {
            return descripcion.substring(0, ERROR_DESCRIPCION_MAX_LENGTH);
        }
        return descripcion;
    }

  /*  private void mapearTicketEntity() {
       /* ticket.setEstado(EstadoTicket.ERROR);
        ticket.setErrorDescripcion(descripcion);*/
     /*   log.warn("Ticket {} ERROR: {}", ticket.getNumeroTicket(), descripcion);
    }*/
}
