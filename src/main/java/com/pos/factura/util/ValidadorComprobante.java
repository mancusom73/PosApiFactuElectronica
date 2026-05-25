package com.pos.factura.util;

import com.pos.factura.exception.FacturaValidationException;
import com.pos.factura.model.Cliente;
import com.pos.factura.model.Comprobante;
import com.pos.factura.model.DetalleItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validaciones de negocio específicas de AFIP Argentina.
 * Se ejecutan ANTES de enviar el request a la API externa.
 *
 * Validaciones incluidas:
 *  1. Tipo de comprobante + coherencia con condición IVA del cliente
 *  2. Fecha no retroactiva (máx. 5 días hacia atrás, regla AFIP)
 *  3. Fecha no futura (AFIP no acepta comprobantes con fecha posterior a hoy)
 *  4. CUIT válido mediante algoritmo de dígito verificador (módulo 11)
 *  5. Alícuotas de IVA válidas según tabla AFIP
 *  6. Totales cuadran: neto + IVA + exentos - bonificación == total declarado
 *  7. Total de formas de pago == total del comprobante
 *  8. Documento del cliente coherente con tipo de comprobante
 */
@Component
public class ValidadorComprobante {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Máxima cantidad de días hacia atrás permitida por AFIP
    private static final int MAX_DIAS_RETROACTIVO = 5;

    // Tolerancia de redondeo para comparación de totales ($0.01)
    private static final BigDecimal TOLERANCIA_TOTAL = new BigDecimal("0.01");

    private static final Set<String> TIPOS_VALIDOS = Set.of(
            "FACTURA A", "FACTURA B", "FACTURA C", "FACTURA M",
            "NOTA DE CREDITO A", "NOTA DE CREDITO B", "NOTA DE CREDITO C",
            "NOTA DE DEBITO A", "NOTA DE DEBITO B", "NOTA DE DEBITO C"
    );

    private static final Set<Double> ALICUOTAS_VALIDAS = Set.of(0.0, 10.5, 21.0, 27.0);

    // Pesos del algoritmo módulo 11 para validar CUIT/CUIL
    private static final int[] PESOS_CUIT = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    // =========================================================================
    // Punto de entrada público
    // =========================================================================

    /**
     * Ejecuta todas las validaciones de negocio sobre el comprobante y el cliente.
     * Acumula TODOS los errores antes de lanzar la excepción, para que el POS
     * reciba la lista completa en un solo request.
     *
     * @throws FacturaValidationException con la lista de errores encontrados
     */
    public void validar(Comprobante comprobante, Cliente cliente) {
        List<String> errores = new ArrayList<>();

        validarTipoComprobante(comprobante, cliente, errores);
        validarFechaComprobante(comprobante.getFecha(), errores);
        validarFechaVencimiento(comprobante, errores);
        validarCuitCliente(comprobante, cliente, errores);
        validarDocumentoCliente(comprobante, cliente, errores);
        validarAlicuotas(comprobante, errores);
        validarTotalesCuadran(comprobante, errores);
        validarTotalesPagos(comprobante, errores);

        if (!errores.isEmpty()) {
            throw new FacturaValidationException(errores);
        }
    }

    // =========================================================================
    // 1. Tipo de comprobante + coherencia con condición IVA
    // =========================================================================

    private void validarTipoComprobante(Comprobante c, Cliente cliente, List<String> errores) {
        String tipo = c.getTipo();
        if (tipo == null || !TIPOS_VALIDOS.contains(tipo.toUpperCase())) {
            errores.add("Tipo de comprobante inválido: '" + tipo + "'. Valores aceptados: " + TIPOS_VALIDOS);
            return;
        }

        String condIva = cliente.getCondicionIva();
        if (tipo.contains(" A") && !"RI".equalsIgnoreCase(condIva)) {
            errores.add("Comprobante tipo A requiere cliente con condición IVA 'RI' (Responsable Inscripto). Recibido: '" + condIva + "'");
        }
        if (tipo.contains(" B") && !"CF".equalsIgnoreCase(condIva)) {
            errores.add("Comprobante tipo B requiere cliente con condición IVA 'CF' (Consumidor Final). Recibido: '" + condIva + "'");
        }
        if (tipo.contains(" C") && !"MO".equalsIgnoreCase(condIva)) {
            errores.add("Comprobante tipo C requiere cliente con condición IVA 'MO' (Monotributista). Recibido: '" + condIva + "'");
        }
    }

    // =========================================================================
    // 2 & 3. Fecha del comprobante: no retroactiva, no futura
    // =========================================================================

    /**
     * AFIP rechaza comprobantes con fecha:
     *  - Anterior a más de 5 días corridos desde hoy
     *  - Posterior a la fecha actual (fecha futura)
     */
    private void validarFechaComprobante(String fechaStr, List<String> errores) {
        if (fechaStr == null || fechaStr.isBlank()) {
            errores.add("La fecha del comprobante es obligatoria");
            return;
        }

        LocalDate fecha;
        try {
            fecha = LocalDate.parse(fechaStr, FMT);
        } catch (DateTimeParseException e) {
            errores.add("Formato de fecha inválido: '" + fechaStr + "'. Use dd/MM/yyyy");
            return;
        }

        LocalDate hoy = LocalDate.now();
        LocalDate limiteRetroactivo = hoy.minusDays(MAX_DIAS_RETROACTIVO);

        if (fecha.isBefore(limiteRetroactivo)) {
            errores.add(String.format(
                    "La fecha del comprobante (%s) es retroactiva. AFIP permite un máximo de %d días hacia atrás (mínimo aceptado: %s)",
                    fechaStr, MAX_DIAS_RETROACTIVO, limiteRetroactivo.format(FMT)));
        }

        if (fecha.isAfter(hoy)) {
            errores.add(String.format(
                    "La fecha del comprobante (%s) no puede ser futura. Fecha máxima permitida: %s",
                    fechaStr, hoy.format(FMT)));
        }
    }

    /**
     * La fecha de vencimiento del pago debe ser >= fecha del comprobante.
     */
    private void validarFechaVencimiento(Comprobante comprobante, List<String> errores) {
        String vencimientoStr = comprobante.getVencimiento();
        String fechaStr = comprobante.getFecha();

        if (vencimientoStr == null || vencimientoStr.isBlank()) return; // opcional

        try {
            LocalDate vencimiento = LocalDate.parse(vencimientoStr, FMT);
            LocalDate fecha = LocalDate.parse(fechaStr, FMT);

            if (vencimiento.isBefore(fecha)) {
                errores.add(String.format(
                        "La fecha de vencimiento (%s) no puede ser anterior a la fecha del comprobante (%s)",
                        vencimientoStr, fechaStr));
            }
        } catch (DateTimeParseException e) {
            errores.add("Formato de fecha de vencimiento inválido: '" + vencimientoStr + "'. Use dd/MM/yyyy");
        }
    }

    // =========================================================================
    // 4. Validación de CUIT mediante dígito verificador (módulo 11)
    // =========================================================================

    /**
     * Valida el CUIT del cliente cuando el tipo de documento es CUIT o CUIL.
     * Aplica el algoritmo oficial de AFIP: módulo 11 con pesos [5,4,3,2,7,6,5,4,3,2].
     *
     * Estructura CUIT: TT-NNNNNNNN-D
     *   TT = tipo (20=masculino, 23=extranjero, 24=extranjero, 27=femenino, 30=empresa, 33=empresa, 34=empresa)
     *   NNNNNNNN = número
     *   D = dígito verificador
     */
    private void validarCuitCliente(Comprobante comprobante, Cliente cliente, List<String> errores) {
        String docTipo = cliente.getDocumentoTipo();
        if (docTipo == null) return;

        boolean esCuitOCuil = "CUIT".equalsIgnoreCase(docTipo) || "CUIL".equalsIgnoreCase(docTipo);
        if (!esCuitOCuil) return;

        String docNro = cliente.getDocumentoNro();
        if (docNro == null || docNro.isBlank()) return; // ya validado en otro método

        // Limpiar guiones y espacios
        String cuitLimpio = docNro.replaceAll("[-\\s]", "");

        if (!cuitLimpio.matches("\\d{11}")) {
            errores.add("El " + docTipo + " '" + docNro + "' debe tener exactamente 11 dígitos numéricos");
            return;
        }

        if (!esCuitDigitoVerificadorValido(cuitLimpio)) {
            errores.add("El " + docTipo + " '" + docNro + "' es inválido: el dígito verificador no corresponde");
            return;
        }

        // Validar prefijo de tipo de CUIT
        int prefijo = Integer.parseInt(cuitLimpio.substring(0, 2));
        Set<Integer> prefijosValidos = Set.of(20, 23, 24, 27, 30, 33, 34);
        if (!prefijosValidos.contains(prefijo)) {
            errores.add("El " + docTipo + " '" + docNro + "' tiene un prefijo inválido (" + prefijo + "). Prefijos válidos: " + prefijosValidos);
        }
    }

    /**
     * Algoritmo oficial AFIP — módulo 11:
     *  1. Multiplicar cada uno de los primeros 10 dígitos por su peso [5,4,3,2,7,6,5,4,3,2]
     *  2. Sumar los productos
     *  3. Calcular resto = suma % 11
     *  4. Dígito verificador = (resto == 0) ? 0 : (resto == 1) ? 9 : 11 - resto
     *  5. Comparar con el 11° dígito del CUIT
     */
    private boolean esCuitDigitoVerificadorValido(String cuit) {
        int suma = 0;
        for (int i = 0; i < 10; i++) {
            suma += Character.getNumericValue(cuit.charAt(i)) * PESOS_CUIT[i];
        }

        int resto = suma % 11;
        int digitoEsperado;

        if (resto == 0) {
            digitoEsperado = 0;
        } else if (resto == 1) {
            digitoEsperado = 9;  // Caso especial: CUIT con este resultado es inválido en la práctica,
                                  // pero se acepta como 9 en algunos contextos. Ajustar si el proveedor difiere.
        } else {
            digitoEsperado = 11 - resto;
        }

        int digitoReal = Character.getNumericValue(cuit.charAt(10));
        return digitoReal == digitoEsperado;
    }

    // =========================================================================
    // 5. Alícuotas de IVA válidas
    // =========================================================================

    private void validarAlicuotas(Comprobante comprobante, List<String> errores) {
        List<DetalleItem> detalle = comprobante.getDetalle();
        if (detalle == null || detalle.isEmpty()) {
            errores.add("El comprobante debe tener al menos un ítem en el detalle");
            return;
        }

        for (int i = 0; i < detalle.size(); i++) {
            DetalleItem item = detalle.get(i);
            if (item.getProducto() == null) continue;

            Double alicuota = item.getProducto().getAlicuota();
            if (alicuota == null || !ALICUOTAS_VALIDAS.contains(alicuota)) {
                errores.add(String.format(
                        "Ítem %d — producto '%s': alícuota IVA inválida (%.1f). Valores aceptados: %s",
                        i + 1, item.getProducto().getDescripcion(), alicuota, ALICUOTAS_VALIDAS));
            }

            if (item.getProducto().getPrecioUnitarioSinIva() != null
                    && item.getProducto().getPrecioUnitarioSinIva().compareTo(BigDecimal.ZERO) < 0) {
                errores.add(String.format(
                        "Ítem %d — producto '%s': el precio unitario no puede ser negativo",
                        i + 1, item.getProducto().getDescripcion()));
            }

            if (item.getCantidad() != null && item.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
                errores.add(String.format(
                        "Ítem %d — producto '%s': la cantidad debe ser mayor a 0",
                        i + 1, item.getProducto().getDescripcion()));
            }
        }
    }

    // =========================================================================
    // 6. Cuadre de totales: neto + IVA + exentos - bonificación == total
    // =========================================================================

    /**
     * Verifica que el total declarado en el comprobante coincida con la suma
     * de sus componentes calculados: neto gravado + IVA + exentos - bonificación general.
     *
     * Se permite una tolerancia de $0.01 para absorber diferencias de redondeo.
     */
    private void validarTotalesCuadran(Comprobante comprobante, List<String> errores) {
        BigDecimal neto       = orCero(comprobante.getNetoGravado());
        BigDecimal iva        = orCero(comprobante.getIvaTotal());
        BigDecimal exentos    = orCero(comprobante.getExentos());
        BigDecimal bonif      = orCero(comprobante.getBonificacion());
        BigDecimal totalDecl  = comprobante.getTotal();

        if (totalDecl == null) {
            errores.add("El total del comprobante no está calculado. Llamar a CalculadorComprobante.calcularTotales() antes de validar");
            return;
        }

        BigDecimal totalCalculado = neto.add(iva).add(exentos).subtract(bonif)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal diferencia = totalDecl.subtract(totalCalculado).abs();

        if (diferencia.compareTo(TOLERANCIA_TOTAL) > 0) {
            errores.add(String.format(
                    "Los totales no cuadran: neto(%.2f) + IVA(%.2f) + exentos(%.2f) - bonificación(%.2f) = %.2f, " +
                    "pero el total declarado es %.2f (diferencia: %.2f)",
                    neto, iva, exentos, bonif, totalCalculado, totalDecl, diferencia));
        }

        if (totalDecl.compareTo(BigDecimal.ZERO) <= 0) {
            errores.add(String.format(
                    "El total del comprobante (%.2f) debe ser mayor a cero", totalDecl));
        }
    }

    // =========================================================================
    // 7. Total de formas de pago == total del comprobante
    // =========================================================================

    private void validarTotalesPagos(Comprobante comprobante, List<String> errores) {
        if (comprobante.getPagos() == null
                || comprobante.getPagos().getFormasPago() == null
                || comprobante.getPagos().getFormasPago().isEmpty()) {
            return; // Pagos es opcional
        }

        BigDecimal totalPagos     = orCero(comprobante.getPagos().getTotal());
        BigDecimal totalComprobante = orCero(comprobante.getTotal());
        BigDecimal diferencia     = totalPagos.subtract(totalComprobante).abs();

        if (diferencia.compareTo(TOLERANCIA_TOTAL) > 0) {
            errores.add(String.format(
                    "El total de formas de pago (%.2f) no coincide con el total del comprobante (%.2f) — diferencia: %.2f",
                    totalPagos, totalComprobante, diferencia));
        }

        // Verificar que no haya formas de pago con importe <= 0
        comprobante.getPagos().getFormasPago().forEach(fp -> {
            if (fp.getImporte() == null || fp.getImporte().compareTo(BigDecimal.ZERO) <= 0) {
                errores.add("Forma de pago '" + fp.getDescripcion() + "': el importe debe ser mayor a cero");
            }
        });
    }

    // =========================================================================
    // 8. Documento del cliente coherente con tipo de comprobante
    // =========================================================================

    private void validarDocumentoCliente(Comprobante comprobante, Cliente cliente, List<String> errores) {
        if (comprobante.getTipo() != null
                && comprobante.getTipo().toUpperCase().contains("FACTURA A")
                && !"CUIT".equalsIgnoreCase(cliente.getDocumentoTipo())) {
            errores.add("Comprobante tipo A requiere que el cliente tenga documento tipo CUIT. Recibido: '"
                    + cliente.getDocumentoTipo() + "'");
        }

        if (cliente.getDocumentoNro() == null || cliente.getDocumentoNro().isBlank()) {
            errores.add("El número de documento del cliente es obligatorio");
        }
    }

    // =========================================================================
    // Utilidades
    // =========================================================================

    private BigDecimal orCero(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }
}
