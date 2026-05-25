package com.pos.factura.dto;

import com.pos.factura.model.Cliente;
import com.pos.factura.model.Comprobante;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO que recibe el endpoint REST desde el punto de venta.
 * No incluye credenciales ni punto de venta (se inyectan desde config).
 */
@Data
public class SolicitudFacturaRequest {

    @Valid
    @NotNull(message = "Los datos del cliente son obligatorios")
    private Cliente cliente;

    @Valid
    @NotNull(message = "Los datos del comprobante son obligatorios")
    private Comprobante comprobante;
}
