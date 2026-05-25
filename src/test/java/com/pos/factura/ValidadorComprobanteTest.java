package com.pos.factura;

import com.pos.factura.exception.FacturaValidationException;
import com.pos.factura.model.*;
import com.pos.factura.util.CalculadorComprobante;
import com.pos.factura.util.ValidadorComprobante;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidadorComprobanteTest {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ValidadorComprobante validador;
    private CalculadorComprobante calculador;

    @BeforeEach
    void setUp() {
        validador  = new ValidadorComprobante();
        calculador = new CalculadorComprobante();
    }

    // =========================================================================
    // CUIT — dígito verificador
    // =========================================================================

    @Nested
    @DisplayName("Validación de CUIT")
    class CuitTests {

        @Test
        @DisplayName("CUIT válido — 20-12345678-9 debe pasar")
        void cuitValido() {
            // 20-12345678-9 es el ejemplo canónico de AFIP
            assertDoesNotThrow(() ->
                    validador.validar(
                            comprobanteFacturaA_conCalcular(),
                            clienteCuit("20123456789")
                    ));
        }

        @Test
        @DisplayName("CUIT con dígito verificador incorrecto debe fallar")
        void cuitDigitoInvalido() {
            FacturaValidationException ex = assertThrows(FacturaValidationException.class, () ->
                    validador.validar(
                            comprobanteFacturaA_conCalcular(),
                            clienteCuit("20123456780") // dígito cambiado
                    ));
            assertTrue(ex.getErrores().stream().anyMatch(e -> e.contains("dígito verificador")));
        }

        @Test
        @DisplayName("CUIT con longitud incorrecta debe fallar")
        void cuitLongitudInvalida() {
            FacturaValidationException ex = assertThrows(FacturaValidationException.class, () ->
                    validador.validar(
                            comprobanteFacturaA_conCalcular(),
                            clienteCuit("2012345678") // 10 dígitos
                    ));
            assertTrue(ex.getErrores().stream().anyMatch(e -> e.contains("11 dígitos")));
        }

        @Test
        @DisplayName("CUIT con prefijo inválido (99) debe fallar")
        void cuitPrefijoInvalido() {
            FacturaValidationException ex = assertThrows(FacturaValidationException.class, () ->
                    validador.validar(
                            comprobanteFacturaA_conCalcular(),
                            clienteCuit("99123456789")
                    ));
            assertTrue(ex.getErrores().stream().anyMatch(e -> e.contains("prefijo")));
        }

        @Test
        @DisplayName("DNI en Factura B no valida CUIT")
        void dniNoValidaCuit() {
            // Para Factura B (consumidor final) se usa DNI — no debe validar CUIT
            assertDoesNotThrow(() ->
                    validador.validar(
                            comprobanteFacturaB_conCalcular(),
                            clienteDni("30111222")
                    ));
        }
    }

    // =========================================================================
    // Fecha — retroactiva y futura
    // =========================================================================

    @Nested
    @DisplayName("Validación de fecha")
    class FechaTests {

        @Test
        @DisplayName("Fecha de hoy debe pasar")
        void fechaHoyValida() {
            String hoy = LocalDate.now().format(FMT);
            assertDoesNotThrow(() ->
                    validador.validar(comprobanteB_conFecha(hoy), clienteDni("30111222")));
        }

        @Test
        @DisplayName("Fecha de hace 3 días debe pasar (dentro del límite AFIP)")
        void fechaTresDiasAtrasValida() {
            String fecha = LocalDate.now().minusDays(3).format(FMT);
            assertDoesNotThrow(() ->
                    validador.validar(comprobanteB_conFecha(fecha), clienteDni("30111222")));
        }

        @Test
        @DisplayName("Fecha de hace 6 días debe fallar (supera límite AFIP de 5 días)")
        void fechaSeisDiasAtrasInvalida() {
            String fecha = LocalDate.now().minusDays(6).format(FMT);
            FacturaValidationException ex = assertThrows(FacturaValidationException.class, () ->
                    validador.validar(comprobanteB_conFecha(fecha), clienteDni("30111222")));
            assertTrue(ex.getErrores().stream().anyMatch(e -> e.contains("retroactiva")));
        }

        @Test
        @DisplayName("Fecha futura debe fallar")
        void fechaFuturaInvalida() {
            String fecha = LocalDate.now().plusDays(1).format(FMT);
            FacturaValidationException ex = assertThrows(FacturaValidationException.class, () ->
                    validador.validar(comprobanteB_conFecha(fecha), clienteDni("30111222")));
            assertTrue(ex.getErrores().stream().anyMatch(e -> e.contains("futura")));
        }

        @Test
        @DisplayName("Formato de fecha inválido debe fallar")
        void fechaFormatoInvalido() {
            FacturaValidationException ex = assertThrows(FacturaValidationException.class, () ->
                    validador.validar(comprobanteB_conFecha("2026-04-24"), clienteDni("30111222")));
            assertTrue(ex.getErrores().stream().anyMatch(e -> e.contains("Formato de fecha")));
        }
    }

    // =========================================================================
    // Cuadre de totales
    // =========================================================================

    @Nested
    @DisplayName("Cuadre de totales")
    class TotalesTests {

        @Test
        @DisplayName("Totales calculados correctamente deben pasar")
        void totalesCorrectos() {
            Comprobante c = comprobanteFacturaB_conCalcular();
            assertDoesNotThrow(() -> validador.validar(c, clienteDni("30111222")));
        }

        @Test
        @DisplayName("Total manipulado manualmente debe fallar")
        void totalManipuladoFalla() {
            Comprobante c = comprobanteFacturaB_conCalcular();
            // Alterar el total declarado luego del cálculo
            c.setTotal(c.getTotal().add(new BigDecimal("100.00")));

            FacturaValidationException ex = assertThrows(FacturaValidationException.class, () ->
                    validador.validar(c, clienteDni("30111222")));
            assertTrue(ex.getErrores().stream().anyMatch(e -> e.contains("no cuadran")));
        }

        @Test
        @DisplayName("Total de pagos que no coincide con total del comprobante debe fallar")
        void totalPagosNoCoincideFalla() {
            Comprobante c = comprobanteFacturaB_conCalcular();

            // Agregar pago con importe diferente al total
            Pagos pagos = new Pagos();
            pagos.setFormasPago(List.of(new Pagos.FormaPago("Efectivo", new BigDecimal("1.00"))));
            pagos.setTotal(new BigDecimal("1.00"));
            c.setPagos(pagos);

            FacturaValidationException ex = assertThrows(FacturaValidationException.class, () ->
                    validador.validar(c, clienteDni("30111222")));
            assertTrue(ex.getErrores().stream().anyMatch(e -> e.contains("formas de pago")));
        }

        @Test
        @DisplayName("Forma de pago con importe cero debe fallar")
        void formaPagoImporteCeroFalla() {
            Comprobante c = comprobanteFacturaB_conCalcular();

            Pagos pagos = new Pagos();
            pagos.setFormasPago(List.of(new Pagos.FormaPago("Efectivo", BigDecimal.ZERO)));
            pagos.setTotal(BigDecimal.ZERO);
            c.setPagos(pagos);

            FacturaValidationException ex = assertThrows(FacturaValidationException.class, () ->
                    validador.validar(c, clienteDni("30111222")));
            assertTrue(ex.getErrores().stream().anyMatch(e -> e.contains("importe debe ser mayor")));
        }
    }

    // =========================================================================
    // Helpers — construcción de objetos de prueba
    // =========================================================================

    private Comprobante comprobanteFacturaB_conCalcular() {
        Comprobante c = comprobanteB_conFecha(LocalDate.now().format(FMT));
        calculador.calcularTotales(c);
        return c;
    }

    private Comprobante comprobanteFacturaA_conCalcular() {
        Producto p = new Producto();
        p.setDescripcion("Monitor");
        p.setPrecioUnitarioSinIva(new BigDecimal("80000.00"));
        p.setAlicuota(21.0);
        p.setUnidadMedida("7");

        DetalleItem item = new DetalleItem();
        item.setCantidad(BigDecimal.ONE);
        item.setBonificacionPorcentaje(BigDecimal.ZERO);
        item.setProducto(p);

        Comprobante c = new Comprobante();
        c.setTipo("FACTURA A");
        c.setFecha(LocalDate.now().format(FMT));
        c.setBonificacion(BigDecimal.ZERO);
        c.setDetalle(List.of(item));
        calculador.calcularTotales(c);
        return c;
    }

    private Comprobante comprobanteB_conFecha(String fecha) {
        Producto p = new Producto();
        p.setDescripcion("Auriculares");
        p.setPrecioUnitarioSinIva(new BigDecimal("10000.00"));
        p.setAlicuota(21.0);
        p.setUnidadMedida("7");

        DetalleItem item = new DetalleItem();
        item.setCantidad(BigDecimal.ONE);
        item.setBonificacionPorcentaje(BigDecimal.ZERO);
        item.setProducto(p);

        Comprobante c = new Comprobante();
        c.setTipo("FACTURA B");
        c.setFecha(fecha);
        c.setBonificacion(BigDecimal.ZERO);
        c.setDetalle(List.of(item));
        calculador.calcularTotales(c);
        return c;
    }

    private Cliente clienteDni(String nro) {
        Cliente c = new Cliente();
        c.setDocumentoTipo("DNI");
        c.setDocumentoNro(nro);
        c.setRazonSocial("Juan Pérez");
        c.setCondicionIva("CF");
        return c;
    }

    private Cliente clienteCuit(String cuit) {
        Cliente c = new Cliente();
        c.setDocumentoTipo("CUIT");
        c.setDocumentoNro(cuit);
        c.setRazonSocial("Empresa S.A.");
        c.setCondicionIva("RI");
        return c;
    }
}
