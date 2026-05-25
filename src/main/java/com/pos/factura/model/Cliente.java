package com.pos.factura.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.*;
import lombok.Data;

@Data
public class Cliente {

    /**
     * Tipo de documento: DNI, CUIT, CUIL, PASAPORTE, SIN_IDENTIFICAR
     */
    @NotBlank(message = "El tipo de documento es obligatorio")
    @JsonProperty("documento_tipo")
    private String documentoTipo;

    @NotBlank(message = "El número de documento es obligatorio")
    @JsonProperty("documento_nro")
    private String documentoNro;

    @NotBlank(message = "La razón social es obligatoria")
    @JsonProperty("razon_social")
    private String razonSocial;

    @Email(message = "El email no tiene formato válido")
    @JsonProperty("email")
    private String email;

    @JsonProperty("domicilio")
    private String domicilio;

    /**
     * Código de provincia AFIP:
     * 1=CABA, 2=Buenos Aires, 13=Córdoba, etc.
     */
    @JsonProperty("provincia")
    private String provincia = "13"; // Córdoba por defecto

    /**
     * CF=Consumidor Final, RI=Resp.Inscripto, MO=Monotributista
     */
    @NotBlank(message = "La condición IVA es obligatoria")
    @JsonProperty("condicion_iva")
    private String condicionIva;

    /**
     * "S" para enviar PDF por email, "N" para no enviar
     */
    @JsonProperty("envia_por_mail")
    private String enviaPorMail = "N";
}
