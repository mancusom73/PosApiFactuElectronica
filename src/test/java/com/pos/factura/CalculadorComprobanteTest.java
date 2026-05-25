package com.pos.factura;

import com.pos.factura.model.*;
import com.pos.factura.util.CalculadorComprobante;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CalculadorComprobanteTest {

    private CalculadorComprobante calculador;

    @BeforeEach
    void setUp() {
        calculador = new CalculadorComprobante();
    }

    @Test
    @DisplayName("Factura B — 2 productos con IVA 21%")
    void testFacturaB_DosProductos_IVA21() {
        // Producto 1: 2 x $10.000 sin IVA → neto $20.000 → IVA $4.200 → total $24.200
        Producto p1 = producto("Auriculares", "10000.00", 21.0);
        DetalleItem item1 = item(p1, "2", "0");

        // Producto 2: 3 x $500 sin IVA, 10% bonif → neto $1.350 → IVA $283.50 → total $1.633.50
        Producto p2 = producto("Cable USB-C", "500.00", 21.0);
        DetalleItem item2 = item(p2, "3", "10");

        Comprobante comprobante = comprobante(List.of(item1, item2), "0");
        calculador.calcularTotales(comprobante);

        assertEquals(new BigDecimal("21350.00"), comprobante.getNetoGravado());
        assertEquals(new BigDecimal("4483.50"),  comprobante.getIvaTotal());
        assertEquals(new BigDecimal("25833.50"), comprobante.getTotal());
        assertEquals(BigDecimal.ZERO.setScale(2), comprobante.getExentos());
    }

    @Test
    @DisplayName("Producto exento — sin IVA")
    void testProductoExento() {
        Producto p = producto("Libro educativo", "1000.00", 0.0);
        DetalleItem item = item(p, "1", "0");

        Comprobante comprobante = comprobante(List.of(item), "0");
        calculador.calcularTotales(comprobante);

        assertEquals(new BigDecimal("0.00"),    comprobante.getNetoGravado());
        assertEquals(new BigDecimal("0.00"),    comprobante.getIvaTotal());
        assertEquals(new BigDecimal("1000.00"), comprobante.getExentos());
        assertEquals(new BigDecimal("1000.00"), comprobante.getTotal());
    }

    @Test
    @DisplayName("Con descuento general del comprobante")
    void testDescuentoGeneral() {
        Producto p = producto("Notebook", "50000.00", 21.0);
        DetalleItem item = item(p, "1", "0");

        // Descuento general de $500
        Comprobante comprobante = comprobante(List.of(item), "500.00");
        calculador.calcularTotales(comprobante);

        // Neto $50.000, IVA $10.500, total bruto $60.500 - $500 desc = $60.000
        assertEquals(new BigDecimal("50000.00"), comprobante.getNetoGravado());
        assertEquals(new BigDecimal("10500.00"), comprobante.getIvaTotal());
        assertEquals(new BigDecimal("60000.00"), comprobante.getTotal());
    }

    @Test
    @DisplayName("Calcular total de pagos")
    void testCalcularTotalPagos() {
        Pagos pagos = new Pagos();
        pagos.setFormasPago(List.of(
                new Pagos.FormaPago("Efectivo",  new BigDecimal("5000.00")),
                new Pagos.FormaPago("VISA DB",   new BigDecimal("10000.00")),
                new Pagos.FormaPago("MercadoPago", new BigDecimal("5833.50"))
        ));

        calculador.calcularTotalPagos(pagos);

        assertEquals(new BigDecimal("20833.50"), pagos.getTotal());
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private Producto producto(String descripcion, String precio, double alicuota) {
        Producto p = new Producto();
        p.setDescripcion(descripcion);
        p.setPrecioUnitarioSinIva(new BigDecimal(precio));
        p.setAlicuota(alicuota);
        p.setUnidadMedida("7");
        return p;
    }

    private DetalleItem item(Producto producto, String cantidad, String bonifPct) {
        DetalleItem item = new DetalleItem();
        item.setCantidad(new BigDecimal(cantidad));
        item.setBonificacionPorcentaje(new BigDecimal(bonifPct));
        item.setAfectaStock("S");
        item.setProducto(producto);
        return item;
    }

    private Comprobante comprobante(List<DetalleItem> items, String bonificacion) {
        Comprobante c = new Comprobante();
        c.setTipo("FACTURA B");
        c.setFecha("24/04/2026");
        c.setBonificacion(new BigDecimal(bonificacion));
        c.setDetalle(items);
        return c;
    }
}
