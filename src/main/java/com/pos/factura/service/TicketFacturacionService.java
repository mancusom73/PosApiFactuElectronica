package com.pos.factura.service;

import com.pos.factura.dto.FacturaResponse;
import com.pos.factura.dto.SolicitudFacturaRequest;
import com.pos.factura.entity.TicketEntity;
import com.pos.factura.entity.TicketEntity.EstadoTicket;
import com.pos.factura.mapper.TicketMapper;
import com.pos.factura.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;

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

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final FacturaService facturaService;

    public TicketFacturacionService(TicketRepository ticketRepository,
                                    TicketMapper ticketMapper,
                                    FacturaService facturaService) {
        this.ticketRepository = ticketRepository;
        this.ticketMapper     = ticketMapper;
        this.facturaService   = facturaService;
    }

    // =========================================================================
    // Facturar por número de ticket
    // =========================================================================

    /**
     * Lee el ticket de MySQL por su número correlativo y emite la factura electrónica.
     * Actualiza el estado del ticket con el resultado (FACTURADO o ERROR).
     *
     * @param numeroTicket Número correlativo generado por el POS
     * @return Respuesta con CAE, PDF, QR
     */
    @Transactional
    public FacturaResponse facturarTicket(String numeroTicket) {
        log.info("Iniciando facturación del ticket: {}", numeroTicket);

        // 1. Leer ticket con todas las relaciones
        TicketEntity ticket = ticketRepository
                .findByNumeroTicketConDetalle(numeroTicket)
                .orElseThrow(() -> new NoSuchElementException(
                        "No se encontró el ticket con número: " + numeroTicket));

        return procesarFacturacion(ticket);
    }

    /**
     * Lee el ticket de MySQL por ID interno y emite la factura electrónica.
     *
     * @param ticketId ID interno de la tabla tickets
     */
    @Transactional
    public FacturaResponse facturarTicketPorId(Long ticketId) {
        log.info("Iniciando facturación del ticket ID: {}", ticketId);

        TicketEntity ticket = ticketRepository
                .findByIdConDetalle(ticketId)
                .orElseThrow(() -> new NoSuchElementException(
                        "No se encontró el ticket con ID: " + ticketId));

        return procesarFacturacion(ticket);
    }

    /**
     * Procesa todos los tickets en estado PENDIENTE (útil para cron o recuperación de errores).
     */
    @Transactional
    public List<FacturaResponse> facturarPendientes() {
        List<TicketEntity> pendientes = ticketRepository.findPendientes();
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
                .collect(java.util.stream.Collectors.toList());
    }

    // =========================================================================
    // Lógica interna
    // =========================================================================

    private FacturaResponse procesarFacturacion(TicketEntity ticket) {
        // 2. Validar estado del ticket
        validarEstadoTicket(ticket);

        try {
            // 3. Mapear entidad → DTO del servicio de facturación
            SolicitudFacturaRequest solicitud = ticketMapper.toSolicitud(ticket);

            log.debug("Ticket {} mapeado: tipo={}, cliente={}, items={}",
                    ticket.getNumeroTicket(),
                    ticket.getTipoComprobante(),
                    ticket.getCliente().getRazonSocial(),
                    ticket.getItems().size());

            // 4. Delegar al servicio de facturación (calcula totales, valida, llama a AFIP)
            FacturaResponse respuesta = facturaService.emitir(solicitud);

            // 5. Actualizar ticket en MySQL según resultado
            if (respuesta.isExitoso()) {
                marcarFacturado(ticket, respuesta);
            } else {
                marcarError(ticket, String.join(" | ", respuesta.getErrores() != null
                        ? java.util.Arrays.asList(respuesta.getErrores())
                        : java.util.Arrays.asList(respuesta.getMensaje())));
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

    private void validarEstadoTicket(TicketEntity ticket) {
        if (ticket.getEstado() == EstadoTicket.FACTURADO) {
            throw new IllegalStateException(
                    "El ticket " + ticket.getNumeroTicket() +
                    " ya fue facturado. CAE: " + ticket.getCae());
        }
        if (ticket.getEstado() == EstadoTicket.ANULADO) {
            throw new IllegalStateException(
                    "El ticket " + ticket.getNumeroTicket() + " está anulado y no puede facturarse");
        }
        if (ticket.getItems() == null || ticket.getItems().isEmpty()) {
            throw new IllegalStateException(
                    "El ticket " + ticket.getNumeroTicket() + " no tiene ítems cargados");
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
            ticket.setCaeVencimiento(LocalDate.parse(respuesta.getCaeVencimiento(), FMT_FECHA));
        }

        log.info("Ticket {} marcado como FACTURADO — CAE: {}, Comprobante: {}",
                ticket.getNumeroTicket(), respuesta.getCae(), respuesta.getComprobanteNro());
    }

    private void marcarError(TicketEntity ticket, String descripcionError) {
        ticket.setEstado(EstadoTicket.ERROR);
        ticket.setErrorDescripcion(descripcionError);
        log.warn("Ticket {} marcado como ERROR: {}", ticket.getNumeroTicket(), descripcionError);
    }
}
