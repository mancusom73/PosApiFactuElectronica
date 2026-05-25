package com.pos.factura.util;

import com.pos.factura.model.Comprobante;
import com.pos.factura.model.DetalleItem;
import com.pos.factura.model.Pagos;
import com.pos.factura.model.Producto;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Calcula automáticamente todos los totales del comprobante
 * siguiendo las reglas de AFIP Argentina.
 *
 * Fórmulas:
 *   subtotal_linea     = precio_sin_iva × cantidad
 *   descuento_linea    = subtotal × (bonif% / 100)
 *   neto_linea         = subtotal - descuento
 *   iva_linea          = neto_linea × (alicuota / 100)
 *   total_linea        = neto_linea + iva_linea
 *   ─────────────────────────────────────────
 *   TOTAL COMPROBANTE  = Σneto_gravado + Σiva + exentos - bonif_general
 */
@Component
public class CalculadorComprobante {

    private static final int ESCALA = 2;
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    /**
     * Calcula y asigna todos los totales al objeto Comprobante.
     * Debe llamarse ANTES de construir el JSON para la API.
     */
    public void calcularTotales(Comprobante comprobante) {

        BigDecimal netoGravadoTotal = BigDecimal.ZERO;
        BigDecimal ivaTotal         = BigDecimal.ZERO;
        BigDecimal exentosTotal     = BigDecimal.ZERO;

        // Mapa IVA por alícuota (útil para Factura A donde AFIP lo exige discriminado)
        Map<Double, BigDecimal> ivaPorAlicuota = new LinkedHashMap<>();

        for (DetalleItem item : comprobante.getDetalle()) {
            Producto p = item.getProducto();

            // 1. Subtotal sin descuento
            BigDecimal subtotal = p.getPrecioUnitarioSinIva()
                    .multiply(item.getCantidad())
                    .setScale(ESCALA, REDONDEO);

            // 2. Descuento por línea
            BigDecimal bonifPct = item.getBonificacionPorcentaje() != null
                    ? item.getBonificacionPorcentaje()
                    : BigDecimal.ZERO;
            BigDecimal descuentoLinea = subtotal
                    .multiply(bonifPct.divide(BigDecimal.valueOf(100), 4, REDONDEO))
                    .setScale(ESCALA, REDONDEO);

            // 3. Neto de la línea
            BigDecimal netoLinea = subtotal.subtract(descuentoLinea);

            // 4. IVA de la línea
            Double alicuota = p.getAlicuota() != null ? p.getAlicuota() : 0.0;

            if (alicuota == 0.0) {
                exentosTotal = exentosTotal.add(netoLinea);
            } else {
                BigDecimal alicuotaBD = BigDecimal.valueOf(alicuota)
                        .divide(BigDecimal.valueOf(100), 4, REDONDEO);
                BigDecimal ivaLinea = netoLinea.multiply(alicuotaBD)
                        .setScale(ESCALA, REDONDEO);

                netoGravadoTotal = netoGravadoTotal.add(netoLinea);
                ivaTotal = ivaTotal.add(ivaLinea);

                // Acumular IVA por alícuota
                ivaPorAlicuota.merge(alicuota, ivaLinea, BigDecimal::add);
            }
        }

        // 5. Descuento general del comprobante (monto fijo)
        BigDecimal descuentoGeneral = comprobante.getBonificacion() != null
                ? comprobante.getBonificacion()
                : BigDecimal.ZERO;

        // 6. Total final
        BigDecimal total = netoGravadoTotal
                .add(ivaTotal)
                .add(exentosTotal)
                .subtract(descuentoGeneral)
                .setScale(ESCALA, REDONDEO);

        // 7. Asignar resultados
        comprobante.setNetoGravado(netoGravadoTotal.setScale(ESCALA, REDONDEO));
        comprobante.setIvaTotal(ivaTotal.setScale(ESCALA, REDONDEO));
        comprobante.setExentos(exentosTotal.setScale(ESCALA, REDONDEO));
        comprobante.setTotal(total);
        comprobante.setIvaPorAlicuota(ivaPorAlicuota);
    }

    /**
     * Calcula el total de los pagos sumando todas las formas de pago.
     * El total de pagos DEBE coincidir con comprobante.getTotal().
     */
    public void calcularTotalPagos(Pagos pagos) {
        if (pagos == null || pagos.getFormasPago() == null) return;

        BigDecimal totalPagos = pagos.getFormasPago().stream()
                .map(fp -> fp.getImporte() != null ? fp.getImporte() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(ESCALA, REDONDEO);

        pagos.setTotal(totalPagos);
    }
}
