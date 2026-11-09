package com.pos.factura.util;

import com.pos.factura.dto.ApiFacturaRequest;
import com.pos.factura.dto.IvaDetalle;
import com.pos.factura.dto.TributoDetalle;
import com.pos.factura.exception.FacturaValidationException;
import com.pos.factura.model.Cliente;
import com.pos.factura.model.Comprobante;
import com.pos.factura.model.DetalleItem;
import com.pos.factura.repository.dbtpviv.ArticuloRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    private static final Logger log = LoggerFactory.getLogger(ValidadorComprobante.class);

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Máxima cantidad de días hacia atrás permitida por AFIP
    private static final int MAX_DIAS_RETROACTIVO = 5;

    // Tolerancia de redondeo para comparación de totales ($0.01)
    private static final BigDecimal TOLERANCIA_TOTAL = new BigDecimal("0.01");

    private static final Set<Integer> CBTE_CLASE_A = Set.of(1, 2, 3);
    private static final Set<Integer> CBTE_CLASE_B = Set.of(6, 7, 8);
    private static final Set<Integer> CBTE_CLASE_C = Set.of(11, 12, 13);
    private static final Set<Integer> COND_IVA_CLASE_A = Set.of(1, 6, 13, 16);
    private static final Set<Integer> COND_IVA_CLASE_B = Set.of(4, 5, 7, 8, 9, 10, 15);
    private static final BigDecimal MONTO_IDENTIFICACION_B = new BigDecimal("10000000");

    private static final Set<String> TIPOS_VALIDOS = Set.of(
            "FACTURA A", "FACTURA B", "FACTURA C", "FACTURA M",
            "NOTA DE CREDITO A", "NOTA DE CREDITO B", "NOTA DE CREDITO C",
            "NOTA DE DEBITO A", "NOTA DE DEBITO B", "NOTA DE DEBITO C"
    );

    private static final Set<Double> ALICUOTAS_VALIDAS = Set.of(0.0, 10.5, 21.0, 27.0);

    // Pesos del algoritmo módulo 11 para validar CUIT/CUIL
    private static final int[] PESOS_CUIT = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    private final ArticuloRepository articuloRepository;

    public ValidadorComprobante(ArticuloRepository articuloRepository) {
        this.articuloRepository = articuloRepository;
    }

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
        validarIvaPorAlicuota(comprobante, errores);
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
    public static boolean esCuitDigitoVerificadorValido(String cuit) {
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
     * de los importes que se envían a la API: impTotal = impNeto + impIVA + impTrib + impOpEx.
     *
     * Se permite una tolerancia de $0.01 para absorber diferencias de redondeo.
     */
    private void validarTotalesCuadran(Comprobante comprobante, List<String> errores) {
        BigDecimal neto       = orCero(comprobante.getNetoGravado());
        BigDecimal iva        = orCero(comprobante.getIvaTotal());
        BigDecimal exentos    = orCero(comprobante.getExentos());
        BigDecimal bonif      = orCero(comprobante.getBonificacion());
        BigDecimal percepcion = orCero(comprobante.getPercepcion());
        BigDecimal totalDecl  = comprobante.getTotal();

        if (totalDecl == null) {
            errores.add("El total del comprobante no está calculado. Llamar a CalculadorComprobante.calcularTotales() antes de validar");
            return;
        }
        totalDecl = totalDecl.setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalCalculado = neto.add(iva).add(percepcion).add(exentos)
                .add(orCero(comprobante.getImpuestosInternos()))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal diferencia = totalDecl.subtract(totalCalculado).abs();

        log.info("Validando cuadre de totales — neto: {} + iva: {} + percepcion: {} + exentos: {} = totalCalculado: {} | bonificacion (no sumada): {} | totalDeclarado: {} | diferencia: {}",
                neto, iva, percepcion, exentos, totalCalculado, bonif, totalDecl, diferencia);

        logDetalleItems(comprobante, percepcion);

        if (diferencia.compareTo(TOLERANCIA_TOTAL) > 0) {
            errores.add(String.format(
                    "Los totales no cuadran: impNeto(%.2f) + impIVA(%.2f) + impTrib(%.2f) + impOpEx(%.2f) = %.2f, " +
                    "pero impTotal es %.2f (diferencia: %.2f)",
                    neto, iva, percepcion, exentos, totalCalculado, totalDecl, diferencia));
        }

        if (totalDecl.compareTo(BigDecimal.ZERO) <= 0) {
            errores.add(String.format(
                    "El total del comprobante (%.2f) debe ser mayor a cero", totalDecl));
        }
    }

    /**
     * Valida el array tributos[] del request ya construido: la suma de importe debe
     * ser igual a impTrib, y si impTrib es 0 debe enviarse vacío.
     *
     * @throws FacturaValidationException si no cumple
     */
    /**
     * Reglas ARCA sobre el request ya construido, según clase de comprobante y datos del receptor.
     *
     * @throws FacturaValidationException si no cumple
     */
    public void validarReglasArca(ApiFacturaRequest request) {
        List<String> errores = new ArrayList<>();
        Integer cbteTipo = request.getCbteTipo();
        boolean claseA = cbteTipo != null && CBTE_CLASE_A.contains(cbteTipo);
        boolean claseB = cbteTipo != null && CBTE_CLASE_B.contains(cbteTipo);
        boolean claseC = cbteTipo != null && CBTE_CLASE_C.contains(cbteTipo);
        Integer docTipo = request.getDocTipo();
        String docNro = request.getDocNro();
        boolean docNroMayorACero = docNro != null && docNro.matches("\\d+") && new BigDecimal(docNro).signum() > 0;
        BigDecimal impTotal = orCero(request.getImpTotal());

        // Condición IVA del receptor según clase (RG 5616)
        Integer condIva = request.getCondicionIvaReceptor();
        if (condIva == null) {
            errores.add("condicionIvaReceptor es obligatorio (RG 5616)");
        } else if (claseA && !COND_IVA_CLASE_A.contains(condIva)) {
            errores.add("Comprobante A: condicionIvaReceptor " + condIva + " inválida. Valores permitidos: " + COND_IVA_CLASE_A);
        } else if (claseB && !COND_IVA_CLASE_B.contains(condIva)) {
            errores.add("Comprobante B: condicionIvaReceptor " + condIva + " inválida. Valores permitidos: " + COND_IVA_CLASE_B);
        }

        // Identificación del receptor
        if (docTipo != null && docTipo == 99 && !"0".equals(docNro)) {
            errores.add("DocTipo 99 (sin identificar) requiere DocNro 0. Recibido: '" + docNro + "'");
        }
        if (claseB && impTotal.compareTo(MONTO_IDENTIFICACION_B) >= 0
                && (docTipo == null || docTipo == 99 || !docNroMayorACero)) {
            errores.add(String.format(
                    "Comprobante B de $%.2f (igual o mayor a $10.000.000): el receptor debe estar identificado (DocTipo distinto a 99 y DocNro mayor a 0)",
                    impTotal));
        }
        if (claseB && impTotal.compareTo(MONTO_IDENTIFICACION_B) < 0
                && docTipo != null && docTipo != 99 && !docNroMayorACero) {
            errores.add(String.format(
                    "ARCA 10015: para comprobantes B menores a $10.000.000 con DocTipo %d (distinto a 99), DocNro debe ser mayor a 0 (DocNro recibido: '%s')",
                    docTipo, docNro));
        }

        BigDecimal impNeto = orCero(request.getImpNeto()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal impIva = orCero(request.getImpIVA()).setScale(2, RoundingMode.HALF_UP);
        List<IvaDetalle> iva = request.getIva() != null ? request.getIva() : List.of();

        // Comprobantes C: sin IVA discriminado ni operaciones exentas
        if (claseC) {
            if (impIva.signum() != 0) errores.add("Comprobante C: impIVA debe ser 0. Recibido: " + impIva);
            if (!iva.isEmpty()) errores.add("Comprobante C: iva[] debe enviarse vacío");
            if (orCero(request.getImpOpEx()).signum() != 0) {
                errores.add("Comprobante C: impOpEx debe ser 0 (todo el importe va en impNeto). Recibido: " + request.getImpOpEx());
            }
        }

        // Comprobantes A/B: iva[] obligatorio y consistente con impNeto / impIVA
        if ((claseA || claseB) && impNeto.signum() > 0) {
            if (iva.isEmpty()) {
                errores.add("Comprobante A/B con impNeto mayor a 0: iva[] es obligatorio");
            } else {
                BigDecimal sumaBase = iva.stream().map(i -> orCero(i.getBaseImp()).setScale(2, RoundingMode.HALF_UP))
                        .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
                BigDecimal sumaImporte = iva.stream().map(i -> orCero(i.getImporte()).setScale(2, RoundingMode.HALF_UP))
                        .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
                if (sumaBase.compareTo(impNeto) != 0) {
                    errores.add(String.format("La suma de iva[].baseImp (%.2f) no es igual a impNeto (%.2f)", sumaBase, impNeto));
                }
                if (sumaImporte.compareTo(impIva) != 0) {
                    errores.add(String.format("La suma de iva[].importe (%.2f) no es igual a impIVA (%.2f)", sumaImporte, impIva));
                }
            }
        }

        if (!errores.isEmpty()) {
            errores.forEach(log::warn);
            throw new FacturaValidationException(errores);
        }
    }

    public void validarTributos(ApiFacturaRequest request) {
        List<String> errores = new ArrayList<>();
        BigDecimal impTrib = orCero(request.getImpTrib()).setScale(2, RoundingMode.HALF_UP);
        List<TributoDetalle> tributos = request.getTributos();

        if (tributos == null) {
            errores.add("tributos[] no puede ser nulo: enviar [] si impTrib es 0");
        } else if (impTrib.signum() == 0) {
            if (!tributos.isEmpty()) {
                errores.add("impTrib es 0 pero tributos[] tiene " + tributos.size() + " elemento(s): debe enviarse vacío");
            }
        } else {
            BigDecimal sumaTrib = tributos.stream()
                    .map(t -> orCero(t.getImporte()).setScale(2, RoundingMode.HALF_UP))
                    .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
            if (sumaTrib.compareTo(impTrib) != 0) {
                errores.add(String.format(
                        "La suma de tributos[].importe (%.2f) no es igual a impTrib (%.2f)", sumaTrib, impTrib));
            }
        }

        if (!errores.isEmpty()) {
            throw new FacturaValidationException(errores);
        }
    }

    /**
     * La suma de iva[].importe del request debe ser igual a impIVA.
     * iva[] solo puede ir vacío si impIVA es 0.
     */
    private void validarIvaPorAlicuota(Comprobante comprobante, List<String> errores) {
        BigDecimal impIva = orCero(comprobante.getIvaTotal()).setScale(2, RoundingMode.HALF_UP);
        Map<Double, BigDecimal> ivaPorAlicuota = comprobante.getIvaPorAlicuota();

        if (ivaPorAlicuota == null || ivaPorAlicuota.isEmpty()) {
            if (impIva.signum() != 0) {
                errores.add(String.format(
                        "iva[] está vacío pero impIVA es %.2f: solo puede ir vacío si impIVA es 0", impIva));
            }
            return;
        }

        BigDecimal sumaIva = ivaPorAlicuota.values().stream()
                .map(importe -> orCero(importe).setScale(2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        if (sumaIva.compareTo(impIva) != 0) {
            errores.add(String.format(
                    "La suma de iva[].importe (%.2f) no es igual a impIVA (%.2f)", sumaIva, impIva));
        }
    }

    /**
     * Loguea el detalle línea por línea (ticket_items) y su suma, para comparar
     * a simple vista contra los totales de cabecera calculados arriba.
     */
    private void logDetalleItems(Comprobante comprobante,  BigDecimal percepcion) {
        List<DetalleItem> detalle = comprobante.getDetalle();
        if (detalle == null || detalle.isEmpty()) return;

        BigDecimal sumaSubtotal = BigDecimal.ZERO;
        BigDecimal sumaNeto     = BigDecimal.ZERO;
        BigDecimal sumaIva      = BigDecimal.ZERO;
        BigDecimal sumaTotal    = BigDecimal.ZERO;
        BigDecimal sumaBonif    = BigDecimal.ZERO;

        for (int i = 0; i < detalle.size(); i++) {
            DetalleItem item = detalle.get(i);
            if (item.getProducto() == null || item.getProducto().getPrecioUnitarioSinIva() == null
                    || item.getCantidad() == null) continue;

            BigDecimal precio    = item.getProducto().getPrecioUnitarioSinIva();
            BigDecimal cantidad  = item.getCantidad();
            BigDecimal bonifPct  = orCero(item.getBonificacionPorcentaje());
            Double alicuota      = item.getProducto().getAlicuota() != null ? item.getProducto().getAlicuota() : 0.0;

  //TODO CREO QUE NO DEBERIA MULTIPLICAR LA CANTIDAD --pero en pesable si ???
            boolean pesable = articuloRepository.findActivoByCodInterno(Integer.valueOf(item.getProducto().getCodigo()))
                    .map(a -> "S".equals(a.getPesable()))
                    .orElse(false);
            BigDecimal subtotal   = (pesable ? precio.multiply(cantidad) : precio).setScale(4, RoundingMode.HALF_UP);
           // BigDecimal subtotal   = precio.setScale(4, RoundingMode.HALF_UP);
            BigDecimal descuento  = subtotal.multiply(bonifPct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
                    .setScale(4, RoundingMode.HALF_UP);
            BigDecimal netoLinea  = subtotal.subtract(descuento);
            BigDecimal ivaLinea   = netoLinea.multiply(BigDecimal.valueOf(alicuota).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
                    .setScale(4, RoundingMode.HALF_UP);
            BigDecimal impInt     = orCero(item.getImpuestoInterno());
            if (pesable) { //ver si esto hace falta
                impInt = impInt.multiply(cantidad).setScale(4, RoundingMode.HALF_UP);
            }
            BigDecimal totalLinea = netoLinea.add(ivaLinea).add(impInt);
            if (!pesable) {
                totalLinea = totalLinea.multiply(cantidad);
            }


            log.info("Item {} — '{}': precio: {} x cantidad: {} = subtotal: {} | bonif%: {} (-{}) | neto: {} | iva({}%): {} | impInt: {} | totalLinea: {}",
                    i + 1, item.getProducto().getCodigo(), precio, cantidad, subtotal,
                    bonifPct, descuento, netoLinea, alicuota, ivaLinea, impInt, totalLinea);

            sumaSubtotal = sumaSubtotal.add(subtotal);
            sumaNeto     = sumaNeto.add(netoLinea);
            sumaIva      = sumaIva.add(ivaLinea);
            sumaTotal    = sumaTotal.add(totalLinea);
            sumaBonif    = sumaBonif.add(pesable ? descuento : descuento.multiply(cantidad));
        }
        sumaTotal    = sumaTotal.add(percepcion);

        log.info("Suma del detalle (ticket_items) — subtotal: {} | neto: {} | iva: {} | Percepcion: {} | bonificaciones: {} | totalDetalle: {}",
                sumaSubtotal.setScale(2, RoundingMode.HALF_UP),
                sumaNeto.setScale(2, RoundingMode.HALF_UP),
                sumaIva.setScale(2, RoundingMode.HALF_UP),
                percepcion,
                sumaBonif.setScale(2, RoundingMode.HALF_UP),
                sumaTotal.setScale(2, RoundingMode.HALF_UP));
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

        BigDecimal totalPagos     = orCero(comprobante.getPagos().getTotal()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalComprobante = orCero(comprobante.getTotal()).setScale(2, RoundingMode.HALF_UP);
        String tipoNc = comprobante.getTipo() == null ? "" : comprobante.getTipo().toUpperCase().replace('_', ' ');
        if (tipoNc.matches("NOTA (DE )?CREDITO [AB]")) {
            totalPagos = totalPagos.abs();
            totalComprobante = totalComprobante.abs();
        }
        BigDecimal diferencia     = totalPagos.subtract(totalComprobante).abs();

        if (diferencia.compareTo(TOLERANCIA_TOTAL) > 0) {
            errores.add(String.format(
                    "El total de formas de pago (%.2f) no coincide con el total del comprobante (%.2f) — diferencia: %.2f",
                    totalPagos, totalComprobante, diferencia));
        }

        // Verificar que no haya formas de pago con importe <= 0
        comprobante.getPagos().getFormasPago().forEach(fp -> {
            if (fp.getImporte() == null
                    || (tipoNc.matches("NOTA (DE )?CREDITO [AB]") ? fp.getImporte().signum() == 0
                                                                  : fp.getImporte().compareTo(BigDecimal.ZERO) <= 0)) {
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
