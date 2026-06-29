package com.pos.factura.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pos.factura.entity.dbtpviv.EventoId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventoResponse {

    @Column(name = "CAJA_Z")
    private Long cajaZ;

    @Column(name = "ID_EVENTO")
    private Long idEvento;

    @JsonProperty("NRO_TICKET")
    private Long nroTicket;

    @JsonProperty("FECHA")
    private Short fecha;

    @JsonProperty("HORA")
    private Short hora;

    @JsonProperty("CAJERO")
    private Short cajero;

    @JsonProperty("CANT_BULTOS")
    private Short cantBultos;

    @JsonProperty("IMPORTE_SIN_IVA")
    private Double importeSinIva;

    @JsonProperty("IMP_INT")
    private Double impInt;

    @JsonProperty ("EXENTO")
    private Double exento;

    @JsonProperty ("IVA1")
    private Double iva1;

    @JsonProperty ("COD_CLIENTE")
    private Integer codCliente;

    @JsonProperty ("IVA2")
    private Double iva2;

    @JsonProperty ("TIPO_EVENTO")
    private Byte tipoEvento;

    @JsonProperty ("SUCURSAL")
    private Short sucursal;

    @JsonProperty ("CAJA")
    private Short caja;

  /*  private boolean exitoso;
    private String mensaje;*/

    public boolean isExitoso() {
        return true;
        // return error != null && error.equalsIgnoreCase("N");
    }

}
