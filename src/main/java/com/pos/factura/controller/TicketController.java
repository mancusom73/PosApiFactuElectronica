package com.pos.factura.controller;

import com.pos.factura.client.FacturaApiClient;
import com.pos.factura.dto.ContadoresResponse;
import com.pos.factura.dto.EventoResponse;
import com.pos.factura.dto.UltimoAutorizadoResponse;
import com.pos.factura.dto.FacturaResponse;
import com.pos.factura.entity.dbtpviv.EvCont;
import com.pos.factura.entity.dbtpviv.Evento;
import com.pos.factura.service.TicketFacturacionService;
import com.pos.factura.service.EventoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pos.factura.exception.FacturaValidationException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Endpoints para facturar tickets leídos desde la base de datos MySQL del POS.
 *
 * Base URL: http://localhost:8080/api/v1/tickets  ojo con el puerto puede ser 8081
 */
@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private static final Logger log = LoggerFactory.getLogger(TicketController.class);

    private final TicketFacturacionService ticketFacturacionService;
    private final EventoService eventoService;
    private final FacturaApiClient facturaApiClient;

    public TicketController(TicketFacturacionService ticketFacturacionService, EventoService eventoService,
                            FacturaApiClient facturaApiClient) {
        this.ticketFacturacionService = ticketFacturacionService;
        this.eventoService = eventoService;
        this.facturaApiClient = facturaApiClient;
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
     * POST /api/v1/tickets/facturar
     *
     * Lee el evento de MySQL  emite la factura electrónica.
     * Quizas deba tener que pasar id_evento + caja_Z por las dudas
     * por ahora busca el ultimo...creo
     *
     * Ejemplo: POST /api/v1/tickets/facturar
     */
    @PostMapping("/facturar")
    public ResponseEntity<FacturaResponse> facturarPorId(
            @RequestParam Long cajaZ,
            @RequestParam Long idEvento,
            @RequestParam Long hora,
            @RequestParam Long ptoVta
    ) {

        log.info("Request de facturación — cajaZ: {}, idEvento: {}, hora: {}, ptoVta: {}", cajaZ, idEvento, hora, ptoVta);

        try {
            FacturaResponse response = eventoService.buscarEvento(cajaZ,idEvento,hora,ptoVta);

            return/* response.isExitoso()
                    ? */ResponseEntity.ok(response);
                    //: ResponseEntity.badRequest().body(response);

        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(
                    //Evento.builder()
                    FacturaResponse.builder()
//                            .exitoso(false)
  //                          .mensaje(e.getMessage())
                            .build()
            );
        }
    }

    /**
     * POST /api/v1/tickets/eventos/facturar-lote?ptoVta=12
     *
     * Envía uno a uno los eventos TIPO_EVENTO 16 y 17 ordenados por CAJA_Z, ID_EVENTO.
     * Se detiene en el primer evento que no obtenga CAE.
     */
    @PostMapping("/eventos/facturar-lote")
    public ResponseEntity<Map<String, Object>> facturarLote(@RequestParam Long ptoVta) {
        List<Evento> eventos = eventoService.buscarEventosAFacturar();
        log.info("Facturación por lote: {} eventos a procesar", eventos.size());

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("total", eventos.size());
        int enviados = 0;

        for (Evento evento : eventos) {
            Long hora = evento.getHora() != null ? evento.getHora().longValue() : 0L;
            String error;
            FacturaResponse response = null;
            try {
                response = eventoService.buscarEvento(evento.getCajaZ(), evento.getIdEvento(), hora, ptoVta);
                if (response.isExitoso() && response.getCae() != null) {
                    enviados++;
                    log.info("Lote {}/{} — evento {} OK, CAE {}", enviados, eventos.size(), evento.getIdEvento(), response.getCae());
                    continue;
                }
                error = response.getErrores() != null ? String.join(" | ", response.getErrores()) : response.getMensaje();
            } catch (Exception e) {
                error = e instanceof FacturaValidationException
                        ? String.join(" | ", ((FacturaValidationException) e).getErrores())
                        : e.getMessage();
            }
            log.error("Lote detenido en evento cajaZ={} idEvento={}: {}", evento.getCajaZ(), evento.getIdEvento(), error);
            resultado.put("enviados", enviados);
            resultado.put("cajaZError", evento.getCajaZ());
            resultado.put("idEventoError", evento.getIdEvento());
            resultado.put("error", error);
            resultado.put("respuesta", response);
            return ResponseEntity.unprocessableEntity().body(resultado);
        }

        resultado.put("enviados", enviados);
        return ResponseEntity.ok(resultado);
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

    /**
     * GET /api/v1/tickets/puntos-venta/ultimo-autorizado?ptoVta=12&cbteTipo=6
     */
    @GetMapping("/puntos-venta/ultimo-autorizado")
    public ResponseEntity<UltimoAutorizadoResponse> ultimoAutorizado(
            @RequestParam Long ptoVta,
            @RequestParam Long cbteTipo) {
        UltimoAutorizadoResponse response = facturaApiClient.consultarUltimoAutorizado(ptoVta, cbteTipo);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/tickets/contadores
     */
    @GetMapping("/contadores")
    public ResponseEntity<ContadoresResponse> contadores() {
        ContadoresResponse response = facturaApiClient.consultarContadores();
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/tickets/ping
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("POS Factura Electronica — servicio activo");
    }
}
