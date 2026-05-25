package com.pos.factura.client;

import com.pos.factura.config.ApiConfig;
import com.pos.factura.dto.ApiFacturaRequest;
import com.pos.factura.dto.ApiFacturaResponse;
import com.pos.factura.exception.ApiComunicacionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
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

    private static final int MAX_REINTENTOS = 3;
    private static final long ESPERA_ENTRE_REINTENTOS_MS = 1000;

    private final RestTemplate restTemplate;
    private final ApiConfig apiConfig;

    public FacturaApiClient(RestTemplate restTemplate, ApiConfig apiConfig) {
        this.restTemplate = restTemplate;
        this.apiConfig = apiConfig;
    }

    /**
     * Envía el request de factura a la API externa y retorna la respuesta.
     *
     * @param request JSON completo con credenciales + comprobante
     * @return Respuesta del proveedor con CAE, PDF, QR
     * @throws ApiComunicacionException si falla la comunicación
     */
    public ApiFacturaResponse emitirFactura(ApiFacturaRequest request) {
        String url = apiConfig.getUrl() + "/facturacion/nuevo";

        log.info("Enviando comprobante tipo '{}' a la API externa: {}",
                request.getComprobante().getTipo(), url);

        HttpHeaders headers = buildHeaders();
        HttpEntity<ApiFacturaRequest> entity = new HttpEntity<>(request, headers);

        int intento = 0;
        Exception ultimoError = null;

        while (intento < MAX_REINTENTOS) {
            intento++;
            try {
                log.debug("Intento {} de {} hacia API externa", intento, MAX_REINTENTOS);

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

                log.info("Respuesta recibida de la API externa — exitoso: {}, CAE: {}",
                        body.isExitoso(), body.getCae());

                return body;

            } catch (HttpClientErrorException ex) {
                // Error 4xx: no reintentar, es un error del cliente
                log.error("Error del cliente HTTP {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
                throw new ApiComunicacionException(
                        "Error en los datos enviados a la API: " + ex.getResponseBodyAsString(),
                        ex.getStatusCode().value()
                );

            } catch (HttpServerErrorException ex) {
                // Error 5xx del servidor externo: reintentable
                log.warn("Error del servidor externo HTTP {} en intento {}: {}",
                        ex.getStatusCode(), intento, ex.getResponseBodyAsString());
                ultimoError = ex;

            } catch (ResourceAccessException ex) {
                // Timeout o error de red: reintentable
                log.warn("Error de red en intento {}: {}", intento, ex.getMessage());
                ultimoError = ex;
            }

            if (intento < MAX_REINTENTOS) {
                esperarEntreReintentos(intento);
            }
        }

        // Agotados los reintentos
        throw new ApiComunicacionException(
                "No se pudo conectar con la API externa luego de " + MAX_REINTENTOS + " intentos: "
                        + (ultimoError != null ? ultimoError.getMessage() : "error desconocido"),
                ultimoError
        );
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    private void esperarEntreReintentos(int intento) {
        try {
            long espera = ESPERA_ENTRE_REINTENTOS_MS * intento;
            log.debug("Esperando {}ms antes del siguiente intento...", espera);
            Thread.sleep(espera);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
