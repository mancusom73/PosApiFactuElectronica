package com.pos.factura.controller;

import com.pos.factura.dto.FacturaResponse;
import com.pos.factura.dto.SolicitudFacturaRequest;
import com.pos.factura.service.FacturaService;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint REST que expone el servicio de factura electrónica al punto de venta.
 *
 * Base URL: http://localhost:8080/api/v1/facturas
 */
@RestController
@RequestMapping("/api/v1/facturas")
public class FacturaController {

    private static final Logger log = LoggerFactory.getLogger(FacturaController.class);

    private final FacturaService facturaService;

    public FacturaController(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    /**
     * POST /api/v1/facturas/emitir
     *
     * Recibe los datos del POS, calcula totales, valida y emite la factura electrónica.
     *
     * @param solicitud Datos del cliente + comprobante (sin credenciales)
     * @return CAE, PDF en base64, QR, número de comprobante
     */
    @PostMapping("/api/v1/emitir")
    public ResponseEntity<FacturaResponse> emitirFactura(
            @Valid @RequestBody SolicitudFacturaRequest solicitud) {

        log.info("Solicitud de factura recibida — Tipo: {}, Cliente: {}",
                solicitud.getComprobante().getTipo(),
                solicitud.getCliente().getRazonSocial());

        FacturaResponse response = facturaService.emitir(solicitud);

        if (response.isExitoso()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * GET /api/v1/facturas/ping
     *
     * Health check — verificar que el servicio está activo.
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("POS Factura Electrónica — servicio activo ✓");
    }
}
