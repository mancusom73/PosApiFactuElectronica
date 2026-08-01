package com.pos.factura;

import com.pos.factura.client.FacturaApiClient;
import com.pos.factura.config.ApiConfig;
import com.pos.factura.dto.ApiFacturaRequest;
import com.pos.factura.dto.ApiFacturaResponse;
import com.pos.factura.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FacturaApiClientMockTest {

    @Test
    void emitirFactura_contraMockBeeceptor() {
        ApiConfig apiConfig = new ApiConfig();
        apiConfig.setUrl("https://prisma.free.beeceptor.com");
        apiConfig.setUsertoken("TU_USERTOKEN");
        apiConfig.setApikey("TU_APIKEY");
        apiConfig.setApitoken("TU_APITOKEN");

        FacturaApiClient client = new FacturaApiClient(new RestTemplate(), apiConfig);

        Producto producto = new Producto();
        producto.setDescripcion("Producto de prueba");
        producto.setCodigo("COD001");
        producto.setPrecioUnitarioSinIva(new BigDecimal("500.00"));
        producto.setAlicuota(21.0);

        DetalleItem item = new DetalleItem();
        item.setCantidad(new BigDecimal("2"));
        item.setProducto(producto);

        Pagos.FormaPago formaPago = new Pagos.FormaPago("Efectivo", new BigDecimal("1210.00"));
        Pagos pagos = new Pagos();
        pagos.setFormasPago(List.of(formaPago));
        pagos.setTotal(new BigDecimal("1210.00"));

        Comprobante comprobante = new Comprobante();
        comprobante.setTipo("FACTURA B");
        comprobante.setFecha("25/07/2026");
        comprobante.setPuntoVenta("0001");
        comprobante.setTotal(new BigDecimal("1210.00"));
        comprobante.setNetoGravado(new BigDecimal("1000.00"));
        comprobante.setIvaTotal(new BigDecimal("210.00"));
        comprobante.setDetalle(List.of(item));
        comprobante.setPagos(pagos);

        Cliente cliente = new Cliente();
        cliente.setDocumentoTipo("DNI");
        cliente.setDocumentoNro("30111222");
        cliente.setRazonSocial("Juan Perez");
        cliente.setCondicionIva("CF");

        ApiFacturaRequest request = ApiFacturaRequest.builder()
                .usertoken(apiConfig.getUsertoken())
                .apikey(apiConfig.getApikey())
                .apitoken(apiConfig.getApitoken())
                .cliente(cliente)
                .comprobante(comprobante)
                .build();

        ApiFacturaResponse response = client.emitirFactura(request);

        System.out.println("Respuesta mock: " + response);
        assertNotNull(response);
    }
}
