package com.pos.factura.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Respuesta del GET /puntos-venta/{ptoVta}/ultimo-autorizado?cbteTipo= de la API externa.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UltimoAutorizadoResponse {

    private Long ptoVta;
    private Long cbteTipo;
    private Long ultimoNro;
    /** "arca" o "local" */
    private String fuente;
    private String timestamp;
}
