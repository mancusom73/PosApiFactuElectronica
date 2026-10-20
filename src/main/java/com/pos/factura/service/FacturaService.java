package com.pos.factura.service;

import com.pos.factura.client.FacturaApiClient;
import com.pos.factura.config.EmisorConfig;
import com.pos.factura.dto.*;
import com.pos.factura.model.Comprobante;
import com.pos.factura.model.Pagos;
import com.pos.factura.util.ApiFacturaRequestMapper;
import com.pos.factura.util.CalculadorComprobante;
import com.pos.factura.util.ValidadorComprobante;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orquesta el flujo completo de emisión de una factura electrónica:
 *
 *  1. Inyectar punto de venta desde config
 *  2. Calcular totales automáticamente
 *  3. Validar reglas de negocio AFIP
 *  4. Construir el JSON para la API externa
 *  5. Persistir el JSON en MySQL ANTES de enviarlo (estado PENDIENTE)
 *  6. Llamar a la API externa
 *  7. Actualizar MySQL con el resultado (APROBADO / RECHAZADO / ERROR)
 *  8. Retornar la respuesta al POS
 */
@Service
public class FacturaService {

    private static final Logger log = LoggerFactory.getLogger(FacturaService.class);

    private final CalculadorComprobante calculador;
    private final ValidadorComprobante validador;
    private final ApiFacturaRequestMapper requestMapper;
    private final FacturaApiClient apiClient;
    private final EmisorConfig emisorConfig;
    private final FacturaPersistenciaService persistenciaService;

    public FacturaService(CalculadorComprobante calculador,
                          ValidadorComprobante validador,
                          ApiFacturaRequestMapper requestMapper,
                          FacturaApiClient apiClient,
                          EmisorConfig emisorConfig,
                          FacturaPersistenciaService persistenciaService) {
        this.calculador          = calculador;
        this.validador           = validador;
        this.requestMapper       = requestMapper;
        this.apiClient           = apiClient;
        this.emisorConfig        = emisorConfig;
        this.persistenciaService = persistenciaService;
    }

    /**
     * Procesa una solicitud de factura desde el punto de venta.
     * El número de ticket es opcional; se usa para trazabilidad en la tabla factura_cabecera.
     */
    public FacturaResponse emitir(SolicitudFacturaRequest solicitud) {
        return emitir(solicitud, null);
    }

    /**
     * Procesa una solicitud de factura desde el punto de venta.
     *
     * @param solicitud    Datos del cliente y comprobante
     * @param numeroTicket Número de ticket del POS (para trazabilidad en la DB)
     */
    public FacturaResponse emitir(SolicitudFacturaRequest solicitud, String numeroTicket) {
        return emitir(solicitud, numeroTicket, null, null);
    }

    public FacturaResponse emitir(SolicitudFacturaRequest solicitud, String numeroTicket,
                                  Long cajaZ, Long idEvento) {

        Comprobante comprobante = solicitud.getComprobante();

        // ── Paso 1: Inyectar punto de venta ───────────────────────────────────
        if (comprobante.getPuntoVenta() == null || comprobante.getPuntoVenta().isBlank()) {
            comprobante.setPuntoVenta(emisorConfig.getPuntoVenta());
        }

        // ── Paso 2: Calcular totales ───────────────────────────────────────────
        log.debug("Calculando totales para {} items", comprobante.getDetalle().size());
        calculador.calcularTotales(comprobante);

        Pagos pagos = comprobante.getPagos();
        if (pagos != null && pagos.getFormasPago() != null && !pagos.getFormasPago().isEmpty()) {
            calculador.calcularTotalPagos(pagos);
        }

        log.info("Totales calculados — Neto: {}, IVA: {}, Total: {}",
                comprobante.getNetoGravado(), comprobante.getIvaTotal(), comprobante.getTotal());

        // ── Paso 3: Validar ────────────────────────────────────────────────────
        log.debug("Validando comprobante tipo '{}'", comprobante.getTipo());
        validador.validar(comprobante, solicitud.getCliente());

        // ── Paso 4: Construir request para la API externa ──────────────────────
        ApiFacturaRequest apiRequest = requestMapper.construir(comprobante, solicitud.getCliente(), emisorConfig);

        // ── Paso 5: Persistir en MySQL ANTES de llamar a AFIP ─────────────────
        Long cabeceraId = null;
        try {
            cabeceraId = persistenciaService.guardarAntesDeLlamar(comprobante, solicitud.getCliente(), numeroTicket, cajaZ, idEvento);
            log.debug("Request persistido en DB con cabecera ID={}", cabeceraId);
        } catch (Exception e) {
            // La persistencia previa no debe interrumpir el flujo de facturación
            log.error("Error persistiendo request pre-AFIP (se continúa igual): {}", e.getMessage());
        }

        // ── Paso 6: Llamar a la API externa ───────────────────────────────────
        ApiFacturaResponse apiResponse;
        try {
            apiResponse = apiClient.emitirFactura(apiRequest);
        } catch (Exception e) {
            // Error de comunicación: marcar en DB y propagar
            if (cabeceraId != null) {
                persistenciaService.marcarError(cabeceraId, e.getMessage());
            }
            throw e;
        }

        ApiFacturaConsultaResponse consulta = null;
        if (apiResponse.isExitoso()) {
            try {
                consulta = apiClient.consultarFactura(FacturaApiClient.extraerIdDeSelf(apiResponse));
            } catch (Exception e) {
                log.error("Error consultando factura {} (fue encolada igual): {}", apiResponse.getId(), e.getMessage());
            }
        }

        // ── Paso 7: Actualizar MySQL con el resultado de AFIP ──────────────────
        if (cabeceraId != null) {
            try {
                persistenciaService.actualizarConRespuesta(cabeceraId, apiResponse, consulta);
                if (tieneErrorFinal(consulta)) {
                    persistenciaService.marcarError(cabeceraId,
                            consulta.getError().getCodigoArca() + " - " + consulta.getError().getMensajeArca());
                }
            } catch (Exception e) {
                log.error("Error actualizando DB post-AFIP (la factura fue emitida): {}", e.getMessage());
            }
        }

        // ── Paso 8: Retornar respuesta al POS ─────────────────────────────────
        return mapearRespuesta(apiResponse, consulta, comprobante);
    }

    private boolean tieneErrorFinal(ApiFacturaConsultaResponse consulta) {
        return consulta != null && consulta.getError() != null
                && ("Rechazada".equalsIgnoreCase(consulta.getEstado()) || "Fallida".equalsIgnoreCase(consulta.getEstado())
                || "Rechazado".equalsIgnoreCase(consulta.getEstado()) || "Fallido".equalsIgnoreCase(consulta.getEstado() ));
    }

    private FacturaResponse mapearRespuesta(ApiFacturaResponse apiResponse,
                                            ApiFacturaConsultaResponse consulta,
                                            Comprobante comprobante) {
        if (apiResponse.isExitoso()) {
            log.info("Comprobante encolado — id: {}, estado: {}",
                    apiResponse.getId(), apiResponse.getEstado());

            FacturaResponse.FacturaResponseBuilder builder = FacturaResponse.builder()
                    .exitoso(true)
                    .mensaje("Comprobante encolado para su procesamiento")
                    .estado(apiResponse.getEstado())
                    .idExterno(apiResponse.getId())
                    .tipoComprobante(comprobante.getTipo())
                    .fecha(comprobante.getFecha())
                    .total(comprobante.getTotal());

            if (consulta != null) {
                builder.estado(consulta.getEstado());
                if (consulta.getComprobante() != null) {
                    builder.cae(consulta.getComprobante().getCae())
                            .caeVencimiento(consulta.getComprobante().getCaeVencimiento())
                            .comprobanteNro(String.valueOf(consulta.getComprobante().getNroCbte()));
                }
                if (tieneErrorFinal(consulta)) {
                    builder.codigoArca(consulta.getError().getCodigoArca())
                            .mensajeArca(consulta.getError().getMensajeArca());
                }
            }

            return builder.build();
        } else {
            log.warn("La API externa no aceptó el comprobante");

            return FacturaResponse.builder()
                    .exitoso(false)
                    .mensaje("La API de facturación no aceptó el comprobante")
                    .build();
        }
    }
}
