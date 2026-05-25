package com.pos.factura.service;

import com.pos.factura.client.FacturaApiClient;
import com.pos.factura.config.ApiConfig;
import com.pos.factura.config.EmisorConfig;
import com.pos.factura.dto.*;
import com.pos.factura.model.Comprobante;
import com.pos.factura.model.Pagos;
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
    private final FacturaApiClient apiClient;
    private final ApiConfig apiConfig;
    private final EmisorConfig emisorConfig;
    private final FacturaPersistenciaService persistenciaService;

    public FacturaService(CalculadorComprobante calculador,
                          ValidadorComprobante validador,
                          FacturaApiClient apiClient,
                          ApiConfig apiConfig,
                          EmisorConfig emisorConfig,
                          FacturaPersistenciaService persistenciaService) {
        this.calculador          = calculador;
        this.validador           = validador;
        this.apiClient           = apiClient;
        this.apiConfig           = apiConfig;
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
        ApiFacturaRequest apiRequest = ApiFacturaRequest.builder()
                .usertoken(apiConfig.getUsertoken())
                .apikey(apiConfig.getApikey())
                .apitoken(apiConfig.getApitoken())
                .cliente(solicitud.getCliente())
                .comprobante(comprobante)
                .build();

        // ── Paso 5: Persistir en MySQL ANTES de llamar a AFIP ─────────────────
        Long cabeceraId = null;
        try {
            cabeceraId = persistenciaService.guardarAntesDeLlamar(apiRequest, numeroTicket);
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

        // ── Paso 7: Actualizar MySQL con el resultado de AFIP ──────────────────
        if (cabeceraId != null) {
            try {
                persistenciaService.actualizarConRespuesta(cabeceraId, apiResponse);
            } catch (Exception e) {
                log.error("Error actualizando DB post-AFIP (la factura fue emitida): {}", e.getMessage());
            }
        }

        // ── Paso 8: Retornar respuesta al POS ─────────────────────────────────
        return mapearRespuesta(apiResponse, comprobante);
    }

    private FacturaResponse mapearRespuesta(ApiFacturaResponse apiResponse, Comprobante comprobante) {
        if (apiResponse.isExitoso()) {
            log.info("Factura emitida exitosamente — CAE: {}, Nro: {}",
                    apiResponse.getCae(), apiResponse.getComprobanteNro());

            return FacturaResponse.builder()
                    .exitoso(true)
                    .mensaje("Comprobante emitido correctamente")
                    .cae(apiResponse.getCae())
                    .caeVencimiento(apiResponse.getCaeVencimiento())
                    .comprobanteNro(apiResponse.getComprobanteNro())
                    .tipoComprobante(comprobante.getTipo())
                    .fecha(comprobante.getFecha())
                    .total(comprobante.getTotal())
                    .pdfBase64(apiResponse.getComprobantePdfB64())
                    .pdfUrl(apiResponse.getComprobantePdfUrl())
                    .afipQr(apiResponse.getAfipQr())
                    .afipCodigoBarras(apiResponse.getAfipCodigoBarras())
                    .build();
        } else {
            log.warn("La API externa rechazó el comprobante: {}", (Object) apiResponse.getErrores());

            return FacturaResponse.builder()
                    .exitoso(false)
                    .mensaje("La API de facturación rechazó el comprobante")
                    .errores(apiResponse.getErrores())
                    .build();
        }
    }
}
