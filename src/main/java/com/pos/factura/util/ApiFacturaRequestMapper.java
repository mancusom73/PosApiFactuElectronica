package com.pos.factura.util;

import com.pos.factura.config.EmisorConfig;
import com.pos.factura.dto.ApiFacturaRequest;
import com.pos.factura.dto.CbteAsociado;
import com.pos.factura.dto.IvaDetalle;
import com.pos.factura.dto.TributoDetalle;
import com.pos.factura.model.Cliente;
import com.pos.factura.model.Comprobante;
import com.pos.factura.model.ComprobanteAsociado;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Construye el ApiFacturaRequest (formato ARCA/AFIP plano) a partir del
 * Comprobante y Cliente del dominio, una vez calculados los totales.
 *
 * Códigos AFIP: ver tablas cbte_tipo, doc_tipo, iva_alicuota, condicion_iva_cliente.
 */
@Component
public class ApiFacturaRequestMapper {

    private static final DateTimeFormatter FMT_ENTRADA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_SALIDA = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Siempre "Percepción de IVA" — hoy el comprobante no discrimina el tipo de percepción */
    private static final int TRIBUTO_ID_PERCEPCION_IVA = 6;

    public ApiFacturaRequest construir(Comprobante comp, Cliente cliente, EmisorConfig emisorConfig) {
        return ApiFacturaRequest.builder()
                .cuit(emisorConfig.getCuit())
                .ptoVta(Integer.valueOf(comp.getPuntoVenta()))
                .cbteTipo(cbteTipoDesde(comp.getTipo()))
                .concepto(1)
                .docTipo(docTipoDesde(cliente.getDocumentoTipo()))
                .docNro(soloDigitos(cliente.getDocumentoNro()))
                .cbteFch(fechaApi(comp.getFecha()))
                .condicionIvaReceptor(condicionIvaReceptorDesde(cliente.getCondicionIva()))
                .impNeto(orCero(comp.getNetoGravado()))
                .impIVA(orCero(comp.getIvaTotal()))
                .impTrib(orCero(comp.getPercepcion()))
                .impTotal(orCero(comp.getTotal()))
                .monId(comp.getMoneda())
                .monCotiz(new BigDecimal(comp.getCotizacion()))
                .iva(ivaDesde(comp))
                .tributos(tributosDesde(comp))
                .cbtesAsoc(cbtesAsocDesde(comp.getComprobantesAsociados()))
                .build();
    }

    // =========================================================================
    // iva[] — un item por alícuota, usando netoPorAlicuota / ivaPorAlicuota
    // =========================================================================

    private List<IvaDetalle> ivaDesde(Comprobante comp) {
        List<IvaDetalle> lista = new ArrayList<>();
        Map<Double, BigDecimal> ivaPorAlicuota = comp.getIvaPorAlicuota();
        Map<Double, BigDecimal> netoPorAlicuota = comp.getNetoPorAlicuota();
        if (ivaPorAlicuota == null) return lista;

        for (Map.Entry<Double, BigDecimal> entry : ivaPorAlicuota.entrySet()) {
            BigDecimal baseImp = netoPorAlicuota != null
                    ? netoPorAlicuota.getOrDefault(entry.getKey(), BigDecimal.ZERO)
                    : BigDecimal.ZERO;
            lista.add(new IvaDetalle(ivaIdDesde(entry.getKey()), baseImp, entry.getValue()));
        }
        return lista;
    }

    // =========================================================================
    // tributos[] — hoy sólo existe el monto genérico de percepción
    // =========================================================================

    private List<TributoDetalle> tributosDesde(Comprobante comp) {
        List<TributoDetalle> lista = new ArrayList<>();
        BigDecimal percepcion = comp.getPercepcion();
        if (percepcion != null && percepcion.compareTo(BigDecimal.ZERO) > 0) {
            lista.add(new TributoDetalle(TRIBUTO_ID_PERCEPCION_IVA, null, percepcion));
        }
        return lista;
    }

    // =========================================================================
    // cbtesAsoc[]
    // =========================================================================

    private List<CbteAsociado> cbtesAsocDesde(List<ComprobanteAsociado> asociados) {
        List<CbteAsociado> lista = new ArrayList<>();
        if (asociados == null) return lista;

        for (ComprobanteAsociado a : asociados) {
            CbteAsociado ca = new CbteAsociado();
            ca.setTipo(cbteTipoDesde(a.getTipoComprobante()));
            ca.setPtoVta(Integer.valueOf(a.getPuntoVenta()));
            ca.setNro(Long.valueOf(a.getNumero()));
            ca.setCuit(a.getCuit());
            lista.add(ca);
        }
        return lista;
    }

    // =========================================================================
    // Mapeos de código AFIP
    // =========================================================================

    private int cbteTipoDesde(String tipo) {
        if (tipo == null) throw new IllegalArgumentException("Tipo de comprobante nulo");
        switch (tipo.trim().toUpperCase()) {
            case "FACTURA A":          return 1;
            case "NOTA DE DEBITO A":   return 2;
            case "NOTA DE CREDITO A":  return 3;
            case "FACTURA B":          return 6;
            case "NOTA DE DEBITO B":   return 7;
            case "NOTA DE CREDITO B":  return 8;
            case "FACTURA C":          return 11;
            case "NOTA DE DEBITO C":   return 12;
            case "NOTA DE CREDITO C":  return 13;
            default:
                throw new IllegalArgumentException("No hay código AFIP (cbteTipo) mapeado para el tipo: " + tipo);
        }
    }

    private int docTipoDesde(String documentoTipo) {
        if (documentoTipo == null) throw new IllegalArgumentException("Tipo de documento nulo");
        switch (documentoTipo.trim().toUpperCase()) {
            case "CUIT":             return 80;
            case "CUIL":             return 86;
            case "DNI":              return 96;
            case "PASAPORTE":        return 94;
            case "SIN_IDENTIFICAR":  return 99;
            default:
                throw new IllegalArgumentException("No hay código AFIP (docTipo) mapeado para el tipo de documento: " + documentoTipo);
        }
    }

    private int condicionIvaReceptorDesde(String condicionIva) {
        if (condicionIva == null) throw new IllegalArgumentException("Condición IVA del cliente nula");
        switch (condicionIva.trim().toUpperCase()) {
            case "RI": return 1;
            case "CF": return 5;
            case "MO": return 6;
            default:
                throw new IllegalArgumentException("No hay código AFIP (condicionIvaReceptor) mapeado para: " + condicionIva);
        }
    }

    private int ivaIdDesde(double alicuota) {
        if (alicuota == 0.0) return 3;
        if (alicuota == 10.5) return 4;
        if (alicuota == 21.0) return 5;
        if (alicuota == 27.0) return 6;
        throw new IllegalArgumentException("No hay código AFIP (iva.id) mapeado para la alícuota: " + alicuota);
    }

    // =========================================================================
    // Utilidades
    // =========================================================================

    private String fechaApi(String fechaDdMmYyyy) {
        return LocalDate.parse(fechaDdMmYyyy, FMT_ENTRADA).format(FMT_SALIDA);
    }

    private String soloDigitos(String valor) {
        return valor != null ? valor.replaceAll("[-\\s]", "") : null;
    }

    private BigDecimal orCero(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }
}
