package com.pos.factura.controller;

import com.pos.factura.dto.FacturaResponse;
import com.pos.factura.service.TicketFacturacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Endpoints para facturar tickets leídos desde la base de datos MySQL del POS.
 *
 * Base URL: http://localhost:8080/api/v1/tickets
 */
@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private static final Logger log = LoggerFactory.getLogger(TicketController.class);

    private final TicketFacturacionService ticketFacturacionService;

    public TicketController(TicketFacturacionService ticketFacturacionService) {
        this.ticketFacturacionService = ticketFacturacionService;
    }

    /**
     * POST /api/v1/tickets/{numeroTicket}/facturar
     *
     * Lee el ticket de MySQL por su número correlativo y emite la factura electrónica.
     * Actualiza el estado del ticket en la base de datos.
     *
     * Ejemplo: POST /api/v1/tickets/TK-00001/facturar
     */
    @PostMapping("/{numeroTicket}/facturar")
    public ResponseEntity<FacturaResponse> facturarPorNumero(
            @PathVariable String numeroTicket) {

        log.info("Request de facturación para ticket: {}", numeroTicket);

        try {
            FacturaResponse response = ticketFacturacionService.facturarTicket(numeroTicket);
            return response.isExitoso()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.badRequest().body(response);

        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(
                    FacturaResponse.builder()
                            .exitoso(false)
                            .mensaje(e.getMessage())
                            .build()
            );
        }
    }

    /**
     * POST /api/v1/tickets/id/{id}/facturar
     *
     * Lee el ticket de MySQL por su ID interno y emite la factura electrónica.
     *
     * Ejemplo: POST /api/v1/tickets/id/42/facturar
     */
    @PostMapping("/id/{id}/facturar")
    public ResponseEntity<FacturaResponse> facturarPorId(@PathVariable Long id) {

        log.info("Request de facturación para ticket ID: {}", id);

        try {
            FacturaResponse response = ticketFacturacionService.facturarTicketPorId(id);
            return response.isExitoso()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.badRequest().body(response);

        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(
                    FacturaResponse.builder()
                            .exitoso(false)
                            .mensaje(e.getMessage())
                            .build()
            );
        }
    }

    /**
     * POST /api/v1/tickets/pendientes/facturar
     *
     * Procesa todos los tickets en estado PENDIENTE.
     * Útil para recuperación de errores o procesamiento batch.
     */
    @PostMapping("/pendientes/facturar")
    public ResponseEntity<List<FacturaResponse>> facturarPendientes() {
        log.info("Procesando tickets pendientes");
        List<FacturaResponse> resultados = ticketFacturacionService.facturarPendientes();
        return ResponseEntity.ok(resultados);
    }
}
