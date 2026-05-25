package com.pos.factura.mapper;

import com.pos.factura.dto.SolicitudFacturaRequest;
import com.pos.factura.entity.ClienteEntity;
import com.pos.factura.entity.ProductoEntity;
import com.pos.factura.entity.TicketEntity;
import com.pos.factura.entity.TicketItemEntity;
import com.pos.factura.entity.TicketPagoEntity;
import com.pos.factura.model.*;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Convierte un TicketEntity (leído de MySQL) en una SolicitudFacturaRequest
 * lista para ser procesada por el FacturaService.
 *
 * Flujo:
 *   MySQL → TicketEntity → TicketMapper → SolicitudFacturaRequest → FacturaService → API AFIP
 */
@Component
public class TicketMapper {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Convierte el ticket completo (con cliente, ítems y pagos) en el DTO
     * que recibe el servicio de facturación.
     */
    public SolicitudFacturaRequest toSolicitud(TicketEntity ticket) {
        SolicitudFacturaRequest solicitud = new SolicitudFacturaRequest();
        solicitud.setCliente(mapCliente(ticket));
        solicitud.setComprobante(mapComprobante(ticket));
        return solicitud;
    }

    // =========================================================================
    // Cliente
    // =========================================================================

    private Cliente mapCliente(TicketEntity ticket) {
        ClienteEntity ce = ticket.getCliente();

        Cliente cliente = new Cliente();
        cliente.setDocumentoTipo(ce.getDocumentoTipo());
        cliente.setDocumentoNro(ce.getDocumentoNro());
        cliente.setRazonSocial(ce.getRazonSocial());
        cliente.setEmail(ce.getEmail());
        cliente.setDomicilio(ce.getDomicilio());
        cliente.setProvincia(ce.getProvincia() != null ? ce.getProvincia() : "13");
        cliente.setCondicionIva(ce.getCondicionIva());
        cliente.setEnviaPorMail(ce.getEnviaPorMail() != null ? ce.getEnviaPorMail() : "N");
        return cliente;
    }

    // =========================================================================
    // Comprobante
    // =========================================================================

    private Comprobante mapComprobante(TicketEntity ticket) {
        Comprobante comprobante = new Comprobante();

        // Tipo de comprobante: usar descripción AFIP del enum
        comprobante.setTipo(ticket.getTipoComprobante().getDescripcionAfip());

        // Fecha en formato dd/MM/yyyy
        comprobante.setFecha(ticket.getFecha().format(FMT));

        // Fecha de vencimiento del pago (si existe)
        if (ticket.getFechaVencimiento() != null) {
            comprobante.setVencimiento(ticket.getFechaVencimiento().format(FMT));
        } else {
            comprobante.setVencimiento(ticket.getFecha().format(FMT));
        }

        comprobante.setMoneda(ticket.getMoneda() != null ? ticket.getMoneda() : "PES");
        comprobante.setCotizacion(
                ticket.getCotizacion() != null ? ticket.getCotizacion().toPlainString() : "1");
        comprobante.setOperacion("V");
        comprobante.setRubro("Venta de mercadería");
        comprobante.setNumero("0"); // AFIP asigna el próximo número
        comprobante.setBonificacion(ticket.getBonificacion());

        // Ítems
        comprobante.setDetalle(mapItems(ticket.getItems()));

        // Formas de pago
        if (ticket.getPagos() != null && !ticket.getPagos().isEmpty()) {
            comprobante.setPagos(mapPagos(ticket.getPagos()));
        }

        return comprobante;
    }

    // =========================================================================
    // Ítems del detalle
    // =========================================================================

    private List<DetalleItem> mapItems(List<TicketItemEntity> items) {
        return items.stream()
                .map(this::mapItem)
                .collect(Collectors.toList());
    }

    private DetalleItem mapItem(TicketItemEntity ie) {
        DetalleItem item = new DetalleItem();
        item.setCantidad(ie.getCantidad());
        item.setBonificacionPorcentaje(ie.getBonificacionPorcentaje());
        item.setAfectaStock(ie.getAfectaStock() != null ? ie.getAfectaStock() : "S");
        item.setLeyenda(ie.getLeyenda() != null ? ie.getLeyenda() : "");
        item.setProducto(mapProducto(ie));
        return item;
    }

    private Producto mapProducto(TicketItemEntity ie) {
        ProductoEntity pe = ie.getProducto();

        Producto producto = new Producto();
        producto.setCodigo(pe.getCodigo());
        producto.setDescripcion(pe.getDescripcion());

        // Usar precio histórico del ítem (precio al momento de la venta)
        // — puede diferir del precio actual en el catálogo
        producto.setPrecioUnitarioSinIva(ie.getPrecioUnitarioSinIva());
        producto.setAlicuota(ie.getAlicuotaIva().doubleValue());
        producto.setUnidadMedida(pe.getUnidadMedida() != null ? pe.getUnidadMedida() : "7");
        producto.setListaPrecios(pe.getListaPrecios() != null ? pe.getListaPrecios() : "LISTA GENERAL");
        producto.setImpuestosInternosAlicuota(0.0);
        return producto;
    }

    // =========================================================================
    // Formas de pago
    // =========================================================================

    private Pagos mapPagos(List<TicketPagoEntity> pagosEntidad) {
        List<Pagos.FormaPago> formas = pagosEntidad.stream()
                .map(pe -> new Pagos.FormaPago(pe.getDescripcion(), pe.getImporte()))
                .collect(Collectors.toList());

        Pagos pagos = new Pagos();
        pagos.setFormasPago(formas);
        // El total de pagos se calcula en CalculadorComprobante.calcularTotalPagos()
        return pagos;
    }
}
