package com.pos.factura.mapper;

import com.pos.factura.dto.SolicitudFacturaRequest;
import com.pos.factura.entity.posfe.ProductoEntity;
import com.pos.factura.entity.posfe.TicketEntity;
import com.pos.factura.entity.posfe.TicketItemEntity;
import com.pos.factura.entity.posfe.TicketPagoEntity;
import com.pos.factura.entity.dbtpviv.ClienteEntity;
import com.pos.factura.model.*;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Convierte TicketEntity + ClienteEntity en SolicitudFacturaRequest.
 *
 * ClienteEntity usa los campos reales de DBTPVIV:
 *   COD_CLIENTE, COD_DOCUMENTO, NRO_DOCUMENTO, NOMBRE, DOMICILIO, PROVINCIA, COND_IVA
 */
@Component
public class TicketMapper {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public SolicitudFacturaRequest toSolicitud(TicketEntity ticket, ClienteEntity cliente) {
        SolicitudFacturaRequest solicitud = new SolicitudFacturaRequest();
        solicitud.setCliente(mapCliente(cliente));
        solicitud.setComprobante(mapComprobante(ticket));
        return solicitud;
    }

    private Cliente mapCliente(ClienteEntity ce) {
        Cliente cliente = new Cliente();
        cliente.setDocumentoTipo(mapTipoDocumento(ce.getCodDocumento()));
        cliente.setDocumentoNro(ce.getNroDocumento());
        cliente.setRazonSocial(ce.getNombre());
        cliente.setDomicilio(ce.getDomicilio());

        // PROVINCIA: viene como descripción (ej "CORDOBA"), mapear a código AFIP
        cliente.setProvincia(mapProvincia(ce.getProvincia()));

        // COND_IVA: mapear código numérico → código AFIP (CF, RI, MO)
        cliente.setCondicionIva(mapCondicionIva(ce.getCondIva()));

        // Email y envia_por_mail no existen en DBTPVIV — valores por defecto
        cliente.setEmail(null);
        cliente.setEnviaPorMail("N");

        return cliente;
    }

    /**
     * Mapea el código de documento de DBTPVIV al tipo que espera AFIP.
     * Ajustar según los valores reales de la columna COD_DOCUMENTO en tu BD.
     */
    private String mapTipoDocumento(String codDocumento) {
        if (codDocumento == null) return "DNI";
        switch (codDocumento.trim().toUpperCase()) {
            case "D": return "DNI";
            case "C": return "CUIT";
            case "L": return "CUIL";
            case "P": return "PASAPORTE";
            default:  return "DNI";
        }
    }

    /** Ajustar según los valores reales de COND_IVA en tu BD */
    private String mapCondicionIva(String cond) {
        if (cond == null) return "CF";
        switch (cond.trim()) {
            case "1": return "RI";
            case "4": return "CF";
            case "6": return "MO";
            default:  return "CF";
        }
    }

    /** Ajustar según los valores reales de PROVINCIA en tu BD */
    private String mapProvincia(String provincia) {
        if (provincia == null) return "13";
        switch (provincia.trim().toUpperCase()) {
            case "BUENOS AIRES":  return "1";
            case "CABA":          return "2";
            case "CORDOBA":       return "13";
            case "SANTA FE":      return "21";
            case "MENDOZA":       return "10";
            default:              return "13";
        }
    }

    private Comprobante mapComprobante(TicketEntity ticket) {
        Comprobante comprobante = new Comprobante();
        comprobante.setTipo(ticket.getTipoComprobante().getDescripcionAfip());
        comprobante.setFecha(ticket.getFecha().format(FMT));
        comprobante.setVencimiento(ticket.getFechaVencimiento() != null
                ? ticket.getFechaVencimiento().format(FMT)
                : ticket.getFecha().format(FMT));
        comprobante.setMoneda(ticket.getMoneda() != null ? ticket.getMoneda() : "PES");
        comprobante.setCotizacion(ticket.getCotizacion() != null
                ? ticket.getCotizacion().toPlainString() : "1");
        comprobante.setOperacion("V");
        comprobante.setRubro("Venta de mercaderia");
        comprobante.setNumero("0");
        comprobante.setBonificacion(ticket.getBonificacion());
        comprobante.setDetalle(mapItems(ticket.getItems()));
        if (ticket.getPagos() != null && !ticket.getPagos().isEmpty()) {
            comprobante.setPagos(mapPagos(ticket.getPagos()));
        }
        return comprobante;
    }

    private List<DetalleItem> mapItems(List<TicketItemEntity> items) {
        return items.stream().map(this::mapItem).collect(Collectors.toList());
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
        producto.setPrecioUnitarioSinIva(ie.getPrecioUnitarioSinIva());
        producto.setAlicuota(ie.getAlicuotaIva().doubleValue());
        producto.setUnidadMedida(pe.getUnidadMedida() != null ? pe.getUnidadMedida() : "7");
        producto.setListaPrecios(pe.getListaPrecios() != null ? pe.getListaPrecios() : "LISTA GENERAL");
        producto.setImpuestosInternosAlicuota(0.0);
        return producto;
    }

    private Pagos mapPagos(List<TicketPagoEntity> pagosEntidad) {
        List<Pagos.FormaPago> formas = pagosEntidad.stream()
                .map(pe -> new Pagos.FormaPago(pe.getDescripcion(), pe.getImporte()))
                .collect(Collectors.toList());
        Pagos pagos = new Pagos();
        pagos.setFormasPago(formas);
        return pagos;
    }
}
