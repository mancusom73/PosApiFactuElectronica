package com.pos.factura.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.factura.config.ApiConfig;
import com.pos.factura.dto.ApiFacturaConsultaResponse;
import com.pos.factura.dto.ApiFacturaRequest;
import com.pos.factura.dto.ApiFacturaResponse;
import com.pos.factura.exception.ApiComunicacionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

/**
 * Cliente HTTP que encapsula toda la comunicación con el proveedor
 * externo de factura electrónica.
 *
 * Implementa reintentos automáticos para errores transitorios de red.
 */
@Component
public class FacturaApiClient {

    private static final Logger log = LoggerFactory.getLogger(FacturaApiClient.class);

    private static final List<String> ESTADOS_PENDIENTES = List.of("recibida", "validada", "encolada", "procesando");

    private final RestTemplate restTemplate;
    private final ApiConfig apiConfig;
    private final ObjectMapper objectMapper;

    public FacturaApiClient(RestTemplate restTemplate, ApiConfig apiConfig, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.apiConfig = apiConfig;
        this.objectMapper = objectMapper;
    }

    /**
     * Envía el request de factura a la API externa y retorna la respuesta.
     *
     * @param request JSON completo con credenciales + comprobante
     * @return Respuesta del proveedor con CAE, PDF, QR
     * @throws ApiComunicacionException si falla la comunicación
     */
    public ApiFacturaResponse emitirFactura(ApiFacturaRequest request) {
        String url = apiConfig.getUrl() + "/facturas";

        log.info("Enviando comprobante cbteTipo={} ptoVta={} a la API externa: {}",
                request.getCbteTipo(), request.getPtoVta(), url);

        String idempotencyKey = java.util.UUID.randomUUID().toString();
        HttpHeaders headers = buildHeaders(idempotencyKey);
        HttpEntity<ApiFacturaRequest> entity = new HttpEntity<>(request, headers);

        int intento = 0;
        Exception ultimoError = null;

        int maxReintentos = apiConfig.getMaxReintentos();
        while (intento < maxReintentos) {
            intento++;
            try {
                log.debug("Intento {} de {} hacia API externa", intento, maxReintentos);

                ResponseEntity<ApiFacturaResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        ApiFacturaResponse.class
                );

                ApiFacturaResponse body = response.getBody();
                if (body == null) {
                    throw new ApiComunicacionException("La API externa retornó una respuesta vacía", 500);
                }

                log.info("Respuesta recibida de la API externa — id: {}, estado: {}",
                        body.getId(), body.getEstado());

                return body;

            } catch (HttpClientErrorException ex) {
                int status = ex.getStatusCode().value();
                log.error("Error del cliente HTTP {}: {}", status, ex.getResponseBodyAsString());
                throw new ApiComunicacionException(extraerCode(ex), status);

            } catch (HttpServerErrorException ex) {
                if (ex.getStatusCode().value() == 503) {
                    log.error("Error HTTP 503: {}", ex.getResponseBodyAsString());
                    throw new ApiComunicacionException(extraerCode(ex), 503);
                }
                // Error 5xx del servidor externo: reintentable
                log.warn("Error del servidor externo HTTP {} en intento {}: {}",
                        ex.getStatusCode(), intento, ex.getResponseBodyAsString());
                ultimoError = ex;

            } catch (ResourceAccessException ex) {
                // Timeout o error de red: reintentable
                log.warn("Error de red en intento {}: {}", intento, ex.getMessage());
                ultimoError = ex;
            }

            if (intento < maxReintentos) {
                esperarEntreReintentos(intento);
            }
        }

        // Agotados los reintentos
        throw new ApiComunicacionException(
                "No se pudo conectar con la API externa luego de " + maxReintentos + " intentos: "
                        + (ultimoError != null ? ultimoError.getMessage() : "error desconocido"),
                ultimoError
        );
    }

    private String extraerCode(HttpStatusCodeException ex) {
        try {
            JsonNode body = objectMapper.readTree(ex.getResponseBodyAsString());
            String code = body.path("code").asText(null);
            String detail = body.path("detail").asText(null);
            if (code != null && !code.isBlank()) {
                return detail != null && !detail.isBlank() ? code + ": " + detail : code;
            }
        } catch (Exception e) {
            log.warn("No se pudo leer el code del body de error: {}", e.getMessage());
        }
        return "Error HTTP " + ex.getStatusCode().value();
    }

    public static String extraerIdDeSelf(ApiFacturaResponse response) {
        String self = response.getLinks() != null ? response.getLinks().getSelf() : null;
        if (self == null || self.isBlank()) {
            return response.getId();
        }
        return self.substring(self.lastIndexOf('/') + 1);
    }

    public ApiFacturaConsultaResponse consultarFactura(String id) {
        String url = apiConfig.getUrl() + "/facturas/" + id;
        log.info("Consultando factura en la API externa: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("X-Api-Key", apiConfig.getApikey());

        long limite = System.currentTimeMillis() + apiConfig.getConsultaTimeoutMs();
        ApiFacturaConsultaResponse ultima = null;
        Exception ultimoError = null;
        int intento = 0;

        while (true) {
            intento++;
            try {
                ResponseEntity<ApiFacturaConsultaResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        ApiFacturaConsultaResponse.class
                );
                ultima = response.getBody();
                if (ultima != null) {
                    log.info("Factura consultada (intento {}) — id: {}, estado: {}", intento, ultima.getId(), ultima.getEstado());
                    boolean pendiente = ultima.getEstado() != null
                            && ESTADOS_PENDIENTES.contains(ultima.getEstado().toLowerCase());
                    if (!pendiente) {
                        return ultima;
                    }
                }
            } catch (HttpClientErrorException ex) {
                log.error("Error HTTP {} consultando factura {}: {}", ex.getStatusCode(), id, ex.getResponseBodyAsString());
                throw new ApiComunicacionException(
                        "Error consultando factura " + id + ": " + ex.getResponseBodyAsString(),
                        ex.getStatusCode().value()
                );
            } catch (HttpServerErrorException | ResourceAccessException ex) {
                log.warn("Error consultando factura {} en intento {}: {}", id, intento, ex.getMessage());
                ultimoError = ex;
            }

            if (System.currentTimeMillis() + apiConfig.getEsperaEntreReintentosMs() > limite) {
                break;
            }
            esperar(apiConfig.getEsperaEntreReintentosMs());
        }

        if (ultima == null) {
            throw new ApiComunicacionException(
                    "No se pudo consultar la factura " + id + " dentro de " + apiConfig.getConsultaTimeoutMs() + "ms: "
                            + (ultimoError != null ? ultimoError.getMessage() : "respuesta vacía"),
                    ultimoError
            );
        }
        log.warn("Factura {} no autorizada luego de {}ms, último estado: {}", id, apiConfig.getConsultaTimeoutMs(), ultima.getEstado());
        return ultima;
    }

    private void esperar(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private HttpHeaders buildHeaders(String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("X-Api-Key", apiConfig.getApikey());
        headers.set("Idempotency-Key", idempotencyKey);
        return headers;
    }

    private void esperarEntreReintentos(int intento) {
        try {
            long espera = apiConfig.getEsperaEntreReintentosMs() * intento;
            log.debug("Esperando {}ms antes del siguiente intento...", espera);
            Thread.sleep(espera);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
