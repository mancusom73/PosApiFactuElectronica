package com.pos.factura;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.pos.factura.client.FacturaApiClient;
import com.pos.factura.dto.ApiFacturaRequest;
import com.pos.factura.dto.ApiFacturaResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Factura el evento real cajaZ=202804 / idEvento=48 de DBTPVIV contra una API mockeada
 * y valida que el JSON enviado tenga 2 decimales y montos consistentes según las reglas de ARCA.
 * Los cambios en posFE se descartan con rollback.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional("posfeTransactionManager")
class FacturarEventoArcaTest {

    private static final BigDecimal TOLERANCIA_IVA = new BigDecimal("0.01");
    private static final Map<Integer, BigDecimal> ALICUOTAS = Map.of(
            3, new BigDecimal("0"),
            4, new BigDecimal("10.5"),
            5, new BigDecimal("21"),
            6, new BigDecimal("27"),
            8, new BigDecimal("5"),
            9, new BigDecimal("2.5"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FacturaApiClient apiClient;

    @Test
    void facturar_cajaZ202804_idEvento48_cumpleReglasArca() throws Exception {
        ApiFacturaResponse respuestaMock = new ApiFacturaResponse();
        respuestaMock.setId("TEST-202804-48");
        respuestaMock.setEstado("Encolada");
        when(apiClient.emitirFactura(any(ApiFacturaRequest.class), anyString())).thenReturn(respuestaMock);

        mockMvc.perform(post("/api/v1/tickets/facturar")
                        .param("cajaZ", "202804")
                        .param("idEvento", "48")
                        .param("hora", "111112")
                        .param("ptoVta", "1"))
                .andExpect(status().isOk());

        ArgumentCaptor<ApiFacturaRequest> captor = ArgumentCaptor.forClass(ApiFacturaRequest.class);
        verify(apiClient).emitirFactura(captor.capture(), anyString());

        String json = objectMapper.writeValueAsString(captor.getValue());
        System.out.println("JSON enviado: " + json);
        JsonNode req = objectMapper.copy()
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true))
                .readTree(json);

        // ── 2 decimales ────────────────────────────────────────────────────────
        BigDecimal impNeto  = dosDecimales(req, "impNeto");
        BigDecimal impIVA   = dosDecimales(req, "impIVA");
        BigDecimal impTrib  = dosDecimales(req, "impTrib");
        BigDecimal impTotal = dosDecimales(req, "impTotal");

        // ── Consistencia de totales ────────────────────────────────────────────
        assertTrue(impTotal.signum() > 0, "impTotal debe ser mayor a 0");
        assertEquals(0, impTotal.compareTo(impNeto.add(impIVA).add(impTrib)),
                "impTotal debe ser impNeto + impIVA + impTrib");

        BigDecimal sumaBase = BigDecimal.ZERO;
        BigDecimal sumaIva = BigDecimal.ZERO;
        for (JsonNode iva : req.path("iva")) {
            int id = iva.path("id").asInt();
            BigDecimal base = dosDecimales(iva, "baseImp");
            BigDecimal importe = dosDecimales(iva, "importe");
            assertTrue(ALICUOTAS.containsKey(id), "Id de alícuota inválido: " + id);
            BigDecimal esperado = base.multiply(ALICUOTAS.get(id)).divide(new BigDecimal("100"));
            assertTrue(importe.subtract(esperado).abs().compareTo(TOLERANCIA_IVA) <= 0,
                    "IVA id " + id + ": importe " + importe + " no coincide con base " + base + " (esperado " + esperado + ")");
            sumaBase = sumaBase.add(base);
            sumaIva = sumaIva.add(importe);
        }

        int cbteTipo = req.path("cbteTipo").asInt();
        if (cbteTipo == 11 || cbteTipo == 12 || cbteTipo == 13) {
            assertEquals(0, req.path("iva").size(), "Comprobantes C no deben informar iva[]");
            assertEquals(0, impIVA.signum(), "Comprobantes C deben tener impIVA = 0");
        } else {
            assertTrue(req.path("iva").size() > 0, "Comprobantes A/B deben informar iva[]");
            assertEquals(0, sumaBase.compareTo(impNeto), "La suma de iva[].baseImp debe ser igual a impNeto");
            assertEquals(0, sumaIva.compareTo(impIVA), "La suma de iva[].importe debe ser igual a impIVA");
        }

        BigDecimal sumaTrib = BigDecimal.ZERO;
        for (JsonNode trib : req.path("tributos")) {
            sumaTrib = sumaTrib.add(dosDecimales(trib, "importe"));
        }
        assertEquals(0, sumaTrib.compareTo(impTrib), "La suma de tributos[].importe debe ser igual a impTrib");
        if (impTrib.signum() == 0) {
            assertTrue(req.path("tributos").isArray() && req.path("tributos").size() == 0,
                    "Si impTrib es 0 tributos[] debe enviarse vacío");
        }

        // ── Receptor ───────────────────────────────────────────────────────────
        if (req.path("docTipo").asInt() == 99) {
            assertEquals("0", req.path("docNro").asText(), "docTipo 99 requiere docNro 0");
        }
        assertTrue(req.path("cbteFch").asText().matches("\\d{4}-\\d{2}-\\d{2}"), "cbteFch con formato inválido");
    }

    private BigDecimal dosDecimales(JsonNode nodo, String campo) {
        JsonNode valor = nodo.path(campo);
        assertTrue(valor.isNumber(), campo + " debe ser numérico: " + valor);
        BigDecimal numero = valor.decimalValue();
        assertEquals(2, numero.scale(), campo + " debe tener exactamente 2 decimales: " + valor);
        return numero;
    }
}
