package com.pos.factura.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Respuesta del GET /contadores de la API externa.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContadoresResponse {

    private Long ptoVta;
    private Long ptoVtaCaea;
    private List<Contador> contadores;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Contador {
        private Long ptoVta;
        private Boolean esCaea;
        private Long cbteTipo;
        /** null si nunca se verificó en ARCA: no fiarse */
        private Long ultimoNroArca;
        private String verificadoEnArca;
        private Long ultimoNroNodo;
    }
}
