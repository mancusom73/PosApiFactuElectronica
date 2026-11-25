package com.pos.factura.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.factura.config.ApiConfig;
import com.pos.factura.dto.ApiFacturaConsultaResponse;
import com.pos.factura.dto.ApiFacturaRequest;
import com.pos.factura.dto.ApiFacturaResponse;
import com.pos.factura.dto.ContadoresResponse;
import com.pos.factura.dto.UltimoAutorizadoResponse;
import com.pos.factura.exception.ApiComunicacionException;
import com.pos.factura.repository.dbtpviv.FactuMemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
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

    //estan en minusculas porque comparo con un tolowercase
    private static final List<String> ESTADOS_PENDIENTES = List.of("recibida", "validada", "encolada", "procesando");

    private final RestTemplate restTemplate;
    private final ApiConfig apiConfig;
    private final ObjectMapper objectMapper;
    private final FactuMemRepository factuMemRepository;

    public FacturaApiClient(RestTemplate restTemplate, ApiConfig apiConfig, ObjectMapper objectMapper,
                            FactuMemRepository factuMemRepository) {
        this.restTemplate = restTemplate;
        this.apiConfig = apiConfig;
        this.objectMapper = objectMapper;
        this.factuMemRepository = factuMemRepository;
    }

    /**
     * Envía el request de factura a la API externa y retorna la respuesta.
     *
     * @param request JSON completo con credenciales + comprobante
     * @return Respuesta del proveedor con CAE, PDF, QR
     * @throws ApiComunicacionException si falla la comunicación
     */
    public ApiFacturaResponse emitirFactura(ApiFacturaRequest request, String idempotencyKey, String apiKey) {
        String url = apiConfig.getUrl() + "/facturas";

        log.info("Enviando comprobante cbteTipo={} ptoVta={} a la API externa: {}",
                request.getCbteTipo(), request.getPtoVta(), url);

        if (idempotencyKey == null) {
            idempotencyKey = java.util.UUID.randomUUID().toString();
        }
        HttpHeaders headers = buildHeaders(idempotencyKey, apiKey);
        HttpEntity<ApiFacturaRequest> entity = new HttpEntity<>(request, headers);
        log.info("JSON enviado a la API externa:\n{}", toJson(request));

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
                log.info("JSON recibido de la API externa:\n{}", toJson(body));

                log.info("Respuesta recibida de la API externa — id: {}, estado: {}",
                        body.getId(), body.getEstado());

                return body;

            } catch (HttpClientErrorException ex) {
                int status = ex.getStatusCode().value();
                if (status == 429) {
                    // LIMITE_EXCEDIDO: esperar Retry-After y reintentar con la misma key
                    log.warn("HTTP 429 en intento {}: {}", intento, ex.getResponseBodyAsString());
                    ultimoError = ex;
                    if (intento < maxReintentos) {
                        esperarRetryAfter(ex, intento);
                    }
                    continue;
                }
                // 400, 401, 403, 404, 409: no reintentables
                log.error("Error del cliente HTTP {}: {}", status, ex.getResponseBodyAsString());
                throw crearExcepcion(ex, status);

            } catch (HttpServerErrorException ex) {
                if (ex.getStatusCode().value() == 503) {
                    ApiComunicacionException error = crearExcepcion(ex, 503);
                    // PUNTO_VENTA_BLOQUEADO requiere intervención humana; solo SERVICIO_NO_DISPONIBLE se reintenta
                    if (!"SERVICIO_NO_DISPONIBLE".equals(error.getCode())) {
                        log.error("Error HTTP 503: {}", ex.getResponseBodyAsString());
                        throw error;
                    }
                }
                // 500 ERROR_INTERNO / 503 SERVICIO_NO_DISPONIBLE: reintentable con la misma key
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
        if (ultimoError instanceof HttpStatusCodeException) {
            HttpStatusCodeException httpError = (HttpStatusCodeException) ultimoError;
            throw crearExcepcion(httpError, httpError.getStatusCode().value());
        }
        throw new ApiComunicacionException(
                "No se pudo conectar con la API externa luego de " + maxReintentos + " intentos: "
                        + (ultimoError != null ? ultimoError.getMessage() : "error desconocido"),
                ultimoError
        );
    }

    private ApiComunicacionException crearExcepcion(HttpStatusCodeException ex, int status) {
        try {
            JsonNode body = objectMapper.readTree(ex.getResponseBodyAsString());
            String code = body.path("code").asText(null);
            String detail = body.path("detail").asText(null);
            if (code != null && !code.isBlank()) {
                return new ApiComunicacionException(code, detail, status);
            }
        } catch (Exception e) {
            log.warn("No se pudo leer el code del body de error: {}", e.getMessage());
        }
        return new ApiComunicacionException("Error HTTP " + status, status);
    }

    public static String extraerIdDeSelf(ApiFacturaResponse response) {
        String self = response.getLinks() != null ? response.getLinks().getSelf() : null;
        if (self == null || self.isBlank()) {
            return response.getId();
        }
        return self.substring(self.lastIndexOf('/') + 1);
    }

    public ApiFacturaConsultaResponse consultarFactura(String id, String apiKey) {
        String url = apiConfig.getUrl() + "/facturas/" + id;
        log.info("Consultando factura en la API externa: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("X-Api-Key", apiKey);

        long limite = System.currentTimeMillis() + apiConfig.getConsultaTimeoutMs();
        ApiFacturaConsultaResponse ultima = null;
        Exception ultimoError = null;
        int intento = 0;
        int transitorios = 0;

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
                log.info("JSON recibido de la API externa (consulta):\n{}", toJson(ultima));
                if (ultima != null) {
                    log.info("Factura consultada (intento {}) — id: {}, estado: {}", intento, ultima.getId(), ultima.getEstado());
                    if ("Error transitorio".equalsIgnoreCase(ultima.getEstado())) {
                        transitorios++;
                        if (transitorios >= apiConfig.getMaxReintentos()) {
                            log.warn("Factura {} sigue en 'Error transitorio' luego de {} intentos consecutivos", id, transitorios);
                            return ultima;
                        }
                        esperar(apiConfig.getEsperaEntreReintentosMs());
                        continue;
                    }
                    transitorios = 0;
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

    public UltimoAutorizadoResponse consultarUltimoAutorizado(Long ptoVta, Long cbteTipo, String apiKey) {
        String url = apiConfig.getUrl() + "/puntos-venta/" + ptoVta + "/ultimo-autorizado?cbteTipo=" + cbteTipo;
        log.info("Consultando último autorizado en la API externa: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("X-Api-Key", apiKey);

        try {
            ResponseEntity<UltimoAutorizadoResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    UltimoAutorizadoResponse.class
            );
            log.info("JSON recibido de la API externa (último autorizado):\n{}", toJson(response.getBody()));
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            log.error("Error HTTP {} consultando último autorizado: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw crearExcepcion(ex, ex.getStatusCode().value());
        } catch (ResourceAccessException ex) {
            throw new ApiComunicacionException("Error de red consultando último autorizado: " + ex.getMessage(), ex);
        }
    }

    public ContadoresResponse consultarContadores(String apiKey) {
        String url = apiConfig.getUrl() + "/contadores";
        log.info("Consultando contadores en la API externa: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("X-Api-Key", apiKey);

        try {
            ResponseEntity<ContadoresResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    ContadoresResponse.class
            );
            log.info("JSON recibido de la API externa (contadores):\n{}", toJson(response.getBody()));
            actualizarFactuMem(response.getBody());
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            log.error("Error HTTP {} consultando contadores: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw crearExcepcion(ex, ex.getStatusCode().value());
        } catch (ResourceAccessException ex) {
            throw new ApiComunicacionException("Error de red consultando contadores: " + ex.getMessage(), ex);
        }
    }

    private void actualizarFactuMem(ContadoresResponse contadores) {
        if (contadores == null || contadores.getContadores() == null) {
            return;
        }
        for (ContadoresResponse.Contador c : contadores.getContadores()) {
            if (!Boolean.FALSE.equals(c.getEsCaea()) || c.getUltimoNroArca() == null || c.getCbteTipo() == null) {
                continue;
            }
            String descripcion;
            switch (c.getCbteTipo().intValue()) {
                case 1: descripcion = "NRO_FACTURA_A"; break;
                case 6: descripcion = "NRO_FACTURA_B"; break;
                case 3: descripcion = "NRO_NCREDITO_A"; break;
                case 8: descripcion = "NRO_NCREDITO_B"; break;
                default: continue;
            }
            factuMemRepository.findByDescripcion(descripcion).ifPresent(mem -> {
                mem.setValor(String.valueOf(c.getUltimoNroArca()));
                factuMemRepository.save(mem);
            });
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            return "No se pudo serializar a JSON: " + e.getMessage();
        }
    }

    private void esperar(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private HttpHeaders buildHeaders(String idempotencyKey, String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("X-Api-Key", apiKey);
        headers.set("Idempotency-Key", idempotencyKey);
        return headers;
    }

    private void esperarRetryAfter(HttpStatusCodeException ex, int intento) {
        String retryAfter = ex.getResponseHeaders() != null ? ex.getResponseHeaders().getFirst("Retry-After") : null;
        try {
            long ms = Long.parseLong(retryAfter.trim()) * 1000;
            log.debug("Esperando Retry-After de {}ms antes del siguiente intento...", ms);
            esperar(ms);
        } catch (Exception e) {
            esperarEntreReintentos(intento);
        }
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
