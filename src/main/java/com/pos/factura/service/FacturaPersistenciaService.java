package com.pos.factura.service;

import com.pos.factura.dto.ApiFacturaRequest;
import com.pos.factura.dto.ApiFacturaResponse;
import com.pos.factura.entity.factura.FacturaCabeceraEntity;
import com.pos.factura.entity.factura.FacturaDetalleEntity;
import com.pos.factura.entity.factura.FacturaMedioPagoEntity;
import com.pos.factura.model.Cliente;
import com.pos.factura.model.Comprobante;
import com.pos.factura.model.DetalleItem;
import com.pos.factura.model.Pagos;
import com.pos.factura.model.Producto;
import com.pos.factura.repository.FacturaCabeceraRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Persiste en MySQL exactamente los datos del JSON enviado a la API de
 * factura electrónica, divididos en 3 tablas:
 *
 *   factura_cabecera   → datos del comprobante + datos del cliente
 *   factura_detalle    → líneas de productos/servicios
 *   factura_medios_pago → formas de pago
 *
 * Se invoca en dos momentos:
 *   1. ANTES de llamar a la API → guarda el JSON con estado PENDIENTE
 *   2. DESPUÉS de recibir la respuesta → actualiza con CAE, estado y respuesta
 *
 * Esto garantiza trazabilidad completa incluso ante errores de comunicación.
 */
@Service
public class FacturaPersistenciaService {

    private static final Logger log = LoggerFactory.getLogger(FacturaPersistenciaService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int ESCALA = 2;
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private final FacturaCabeceraRepository cabeceraRepository;

    public FacturaPersistenciaService(FacturaCabeceraRepository cabeceraRepository) {
        this.cabeceraRepository = cabeceraRepository;
    }

    // =========================================================================
    // Paso 1 — Guardar el JSON ANTES de enviarlo a la API
    // =========================================================================

    /**
     * Persiste el request completo con estado PENDIENTE antes de llamar a AFIP.
     * Retorna el ID de la cabecera para poder actualizar el estado después.
     *
     * @param request       JSON completo que se va a enviar a la API
     * @param numeroTicket  Número del ticket del POS (para trazabilidad)
     * @return              ID de la FacturaCabeceraEntity creada
     */
    @Transactional("posfeTransactionManager")
    public Long guardarAntesDeLlamar(ApiFacturaRequest request, String numeroTicket) {
        log.debug("Persistiendo request de factura antes de enviar a AFIP. Ticket: {}", numeroTicket);

        Comprobante comp = request.getComprobante();
        Cliente cliente = request.getCliente();

        // 1. Cabecera
        FacturaCabeceraEntity cabecera = mapearCabecera(comp, cliente);
        cabecera.setNumeroTicketPos(numeroTicket);
        cabecera.setEstado("PENDIENTE");
        cabecera.setFechaEnvioApi(LocalDateTime.now());
        cabecera = cabeceraRepository.save(cabecera);

        // 2. Detalle de ítems
        List<FacturaDetalleEntity> detalle = mapearDetalle(comp, cabecera);
        cabecera.setDetalle(detalle);

        // 3. Medios de pago
        List<FacturaMedioPagoEntity> medios = mapearMediosPago(comp, cabecera);
        cabecera.setMediosPago(medios);

        cabeceraRepository.save(cabecera);

        log.info("Request persistido con ID={}, ticket={}, tipo={}, total={}",
                cabecera.getId(), numeroTicket, comp.getTipo(), comp.getTotal());

        return cabecera.getId();
    }

    // =========================================================================
    // Paso 2 — Actualizar con la respuesta de AFIP
    // =========================================================================

    /**
     * Actualiza la cabecera con el resultado devuelto por la API de AFIP.
     * Registra CAE, número de comprobante, PDF, QR y estado final.
     *
     * @param cabeceraId    ID retornado por guardarAntesDeLlamar()
     * @param apiResponse   Respuesta recibida de la API externa
     */
    @Transactional("posfeTransactionManager")
    public void actualizarConRespuesta(Long cabeceraId, ApiFacturaResponse apiResponse) {
        FacturaCabeceraEntity cabecera = cabeceraRepository.findById(cabeceraId)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró la cabecera con ID: " + cabeceraId));

        cabecera.setFechaRespuestaApi(LocalDateTime.now());

        if (apiResponse.isExitoso()) {
            cabecera.setEstado("APROBADO");
            cabecera.setCae(apiResponse.getCae());
            cabecera.setNumeroComprobante(apiResponse.getComprobanteNro());
            cabecera.setPdfUrl(apiResponse.getComprobantePdfUrl());
            cabecera.setPdfBase64(apiResponse.getComprobantePdfB64());
            cabecera.setAfipQr(apiResponse.getAfipQr());
            cabecera.setAfipCodigoBarras(apiResponse.getAfipCodigoBarras());
            cabecera.setErrorDescripcion(null);

            if (apiResponse.getCaeVencimiento() != null) {
                try {
                    cabecera.setCaeVencimiento(
                            LocalDate.parse(apiResponse.getCaeVencimiento(), FMT));
                } catch (DateTimeParseException e) {
                    log.warn("No se pudo parsear la fecha de vencimiento del CAE: {}",
                            apiResponse.getCaeVencimiento());
                }
            }

            log.info("Cabecera ID={} actualizada como APROBADA — CAE: {}, Nro: {}",
                    cabeceraId, apiResponse.getCae(), apiResponse.getComprobanteNro());

        } else {
            String errores = apiResponse.getErrores() != null
                    ? String.join(" | ", apiResponse.getErrores())
                    : "Error desconocido";

            cabecera.setEstado("RECHAZADO");
            cabecera.setErrorDescripcion(errores);

            log.warn("Cabecera ID={} marcada como RECHAZADA: {}", cabeceraId, errores);
        }

        cabeceraRepository.save(cabecera);
    }

    /**
     * Marca la cabecera como ERROR ante una falla de comunicación con la API
     * (timeout, red caída, 5xx del servidor externo).
     *
     * @param cabeceraId        ID retornado por guardarAntesDeLlamar()
     * @param mensajeError      Descripción del error de comunicación
     */
    @Transactional("posfeTransactionManager")
    public void marcarError(Long cabeceraId, String mensajeError) {
        cabeceraRepository.findById(cabeceraId).ifPresent(cabecera -> {
            cabecera.setEstado("ERROR");
            cabecera.setFechaRespuestaApi(LocalDateTime.now());
            cabecera.setErrorDescripcion(mensajeError);
            cabeceraRepository.save(cabecera);
            log.warn("Cabecera ID={} marcada como ERROR: {}", cabeceraId, mensajeError);
        });
    }

    // =========================================================================
    // Mapeo cabecera
    // =========================================================================

    private FacturaCabeceraEntity mapearCabecera(Comprobante comp, Cliente cliente) {
        FacturaCabeceraEntity e = new FacturaCabeceraEntity();

        // Comprobante
        e.setTipoComprobante(comp.getTipo());
        e.setPuntoVenta(comp.getPuntoVenta());
        e.setFecha(parseFecha(comp.getFecha()));
        e.setFechaVencimiento(parseFecha(comp.getVencimiento()));
        e.setMoneda(comp.getMoneda());
        e.setCotizacion(parseBigDecimal(comp.getCotizacion()));
        e.setOperacion(comp.getOperacion());
        e.setRubro(comp.getRubro());

        // Totales
        e.setNetoGravado(comp.getNetoGravado());
        e.setIvaTotal(comp.getIvaTotal());
        e.setExentos(orCero(comp.getExentos()));
        e.setBonificacion(orCero(comp.getBonificacion()));
        e.setTotal(comp.getTotal());

        // Cliente (snapshot del momento de la facturación)
        e.setClienteDocumentoTipo(cliente.getDocumentoTipo());
        e.setClienteDocumentoNro(cliente.getDocumentoNro());
        e.setClienteRazonSocial(cliente.getRazonSocial());
        e.setClienteEmail(cliente.getEmail());
        e.setClienteDomicilio(cliente.getDomicilio());
        e.setClienteProvincia(cliente.getProvincia());
        e.setClienteCondicionIva(cliente.getCondicionIva());
        e.setClienteEnviaPorMail(cliente.getEnviaPorMail());

        return e;
    }

    // =========================================================================
    // Mapeo detalle
    // =========================================================================

    private List<FacturaDetalleEntity> mapearDetalle(Comprobante comp,
                                                      FacturaCabeceraEntity cabecera) {
        List<FacturaDetalleEntity> lista = new ArrayList<>();

        if (comp.getDetalle() == null) {
            return lista;
        }

        int orden = 1;
        for (DetalleItem item : comp.getDetalle()) {
            FacturaDetalleEntity d = new FacturaDetalleEntity();
            d.setCabecera(cabecera);
            d.setOrden(orden++);

            // Datos del ítem
            d.setCantidad(item.getCantidad());
            d.setAfectaStock(item.getAfectaStock());
            d.setBonificacionPorcentaje(orCero(item.getBonificacionPorcentaje()));
            d.setLeyenda(item.getLeyenda());

            // Datos del producto
            Producto p = item.getProducto();
            if (p != null) {
                d.setProductoCodigo(p.getCodigo());
                d.setProductoDescripcion(p.getDescripcion());
                d.setPrecioUnitarioSinIva(p.getPrecioUnitarioSinIva());
                d.setAlicuotaIva(BigDecimal.valueOf(p.getAlicuota()));
                d.setUnidadMedida(p.getUnidadMedida());
                d.setListaPrecios(p.getListaPrecios());
                d.setImpuestosInternosAlicuota(
                        p.getImpuestosInternosAlicuota() != null
                                ? BigDecimal.valueOf(p.getImpuestosInternosAlicuota())
                                : BigDecimal.ZERO);

                // Calcular totales de línea para auditoría
                calcularTotalesLinea(d, item, p);
            }

            lista.add(d);
        }

        return lista;
    }

    private void calcularTotalesLinea(FacturaDetalleEntity d, DetalleItem item, Producto p) {
        if (item.getCantidad() == null || p.getPrecioUnitarioSinIva() == null) {
            return;
        }

        BigDecimal subtotal = p.getPrecioUnitarioSinIva()
                .multiply(item.getCantidad())
                .setScale(ESCALA, REDONDEO);

        BigDecimal bonif = orCero(item.getBonificacionPorcentaje());
        BigDecimal descuento = subtotal
                .multiply(bonif.divide(BigDecimal.valueOf(100), 4, REDONDEO))
                .setScale(ESCALA, REDONDEO);

        BigDecimal neto = subtotal.subtract(descuento);

        BigDecimal alicuota = p.getAlicuota() != null
                ? BigDecimal.valueOf(p.getAlicuota()).divide(BigDecimal.valueOf(100), 4, REDONDEO)
                : BigDecimal.ZERO;

        BigDecimal iva = neto.multiply(alicuota).setScale(ESCALA, REDONDEO);
        BigDecimal total = neto.add(iva).setScale(ESCALA, REDONDEO);

        d.setNetoLinea(neto);
        d.setIvaLinea(iva);
        d.setTotalLinea(total);
    }

    // =========================================================================
    // Mapeo medios de pago
    // =========================================================================

    private List<FacturaMedioPagoEntity> mapearMediosPago(Comprobante comp,
                                                           FacturaCabeceraEntity cabecera) {
        List<FacturaMedioPagoEntity> lista = new ArrayList<>();

        Pagos pagos = comp.getPagos();
        if (pagos == null || pagos.getFormasPago() == null) {
            return lista;
        }

        for (Pagos.FormaPago fp : pagos.getFormasPago()) {
            FacturaMedioPagoEntity mp = new FacturaMedioPagoEntity();
            mp.setCabecera(cabecera);
            mp.setDescripcion(fp.getDescripcion());
            mp.setImporte(fp.getImporte());
            lista.add(mp);
        }

        return lista;
    }

    // =========================================================================
    // Utilidades
    // =========================================================================

    private LocalDate parseFecha(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(fecha, FMT);
        } catch (DateTimeParseException e) {
            log.warn("No se pudo parsear la fecha '{}', se guarda como null", fecha);
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String valor) {
        if (valor == null || valor.isBlank()) {
            return BigDecimal.ONE;
        }
        try {
            return new BigDecimal(valor);
        } catch (NumberFormatException e) {
            return BigDecimal.ONE;
        }
    }

    private BigDecimal orCero(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }
}
