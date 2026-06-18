package com.pos.factura.mapper;

import com.pos.factura.dto.SolicitudFacturaRequest;
import com.pos.factura.entity.dbtpviv.ArticuloEntity;
import com.pos.factura.entity.dbtpviv.ClienteEntity;
import com.pos.factura.entity.posfe.TicketEntity;
import com.pos.factura.entity.posfe.TicketItemEntity;
import com.pos.factura.entity.posfe.TicketPagoEntity;
import com.pos.factura.model.*;
import com.pos.factura.repository.dbtpviv.ArticuloRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Convierte TicketEntity (posFE) + ClienteEntity (DBTPVIV) en SolicitudFacturaRequest.
 *
 * Los artículos se leen de ArticuloEntity (DBTPVIV) usando el codInterno
 * guardado en cada TicketItemEntity.
 *
 * Mapeo articulo → Producto AFIP:
 *   COD_INTERNO      → codigo
 *   NOMBRE           → descripcion
 *   PRECIO_SIN_IVA   → precioUnitarioSinIva  (desde ticket_items, precio histórico)
 *   COD_IVA          → alicuota              (convertido: 1→21%, 2→10.5%, 3→0%)
 *   UNIDAD           → unidadMedida
 *   lista_precios    → "LISTA GENERAL" (fijo)
 */
@Component
public class TicketMapper {

    private static final Logger log = LoggerFactory.getLogger(TicketMapper.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ArticuloRepository articuloRepository;

    public TicketMapper(ArticuloRepository articuloRepository) {
        this.articuloRepository = articuloRepository;
    }

    public SolicitudFacturaRequest toSolicitud(TicketEntity ticket, ClienteEntity cliente) {
        SolicitudFacturaRequest solicitud = new SolicitudFacturaRequest();
        solicitud.setCliente(mapCliente(cliente));
        solicitud.setComprobante(mapComprobante(ticket));
        return solicitud;
    }

    // ── Cliente ───────────────────────────────────────────────────────────────

    private Cliente mapCliente(ClienteEntity ce) {
        Cliente cliente = new Cliente();
        cliente.setDocumentoTipo(mapTipoDocumento(ce.getCodDocumento()));
        cliente.setDocumentoNro(ce.getNroDocumento());
        cliente.setRazonSocial(ce.getNombre());
        cliente.setDomicilio(ce.getDomicilio());
        cliente.setProvincia(mapProvincia(ce.getProvincia()));
        cliente.setCondicionIva(mapCondicionIva(ce.getCondIva()));
        cliente.setEmail(null);
        cliente.setEnviaPorMail("N");

        return cliente;
    }

    /** Ajustar según los valores reales de COD_DOCUMENTO en tu BD */
    private String mapTipoDocumento(String cod) {
        if (cod == null) return "DNI";
        switch (cod.trim().toUpperCase()) {
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
            case "BUENOS AIRES": return "1";
            case "CABA":         return "2";
            case "CORDOBA":      return "13";
            case "SANTA FE":     return "21";
            case "MENDOZA":      return "10";
            default:             return "13";
        }
    }

    // ── Comprobante ───────────────────────────────────────────────────────────

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

    // ── Items ─────────────────────────────────────────────────────────────────

    private List<DetalleItem> mapItems(List<TicketItemEntity> items) {
        if (items == null) return new ArrayList<>();
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

    /**
     * Mapea un TicketItemEntity al modelo Producto usando ArticuloEntity de DBTPVIV.
     *
     * Precio y alícuota se toman del ticket_item (precio histórico al momento de la venta).
     * Descripción, código y unidad se toman del artículo actual en DBTPVIV.
     */
    private Producto mapProducto(TicketItemEntity ie) {
        Producto producto = new Producto();

        // Buscar artículo en DBTPVIV por COD_INTERNO
        Optional<ArticuloEntity> articuloOpt =
                articuloRepository.findActivoByCodInterno(ie.getCodInterno());

        if (articuloOpt.isPresent()) {
            ArticuloEntity art = articuloOpt.get();
            // COD_INTERNO → codigo
            producto.setCodigo(String.valueOf(art.getCodInterno()));
            // NOMBRE → descripcion
            producto.setDescripcion(art.getNombre() != null ? art.getNombre() : "SIN DESCRIPCION");
            // UNIDAD → unidadMedida (CHAR(2) de DBTPVIV → código numérico AFIP)
            producto.setUnidadMedida(mapUnidadMedida(art.getUnidad()));
        } else {
            // Artículo no encontrado — usar datos mínimos del ítem para no bloquear la factura
            log.warn("Artículo con COD_INTERNO {} no encontrado en DBTPVIV", ie.getCodInterno());
            producto.setCodigo(String.valueOf(ie.getCodInterno()));
            producto.setDescripcion("ARTICULO " + ie.getCodInterno());
            producto.setUnidadMedida("7");
        }

        // PRECIO_SIN_IVA → precio histórico guardado en el ticket_item
        producto.setPrecioUnitarioSinIva(ie.getPrecioUnitarioSinIva());

        // COD_IVA → alícuota AFIP (0, 10.5 o 21) — también histórico del ticket_item
        producto.setAlicuota(ie.getAlicuotaIva().doubleValue());

        // Siempre fija
        producto.setListaPrecios("LISTA GENERAL");
        producto.setImpuestosInternosAlicuota(0.0);
        return producto;
    }

    /**
     * Convierte COD_IVA (TINYINT de DBTPVIV) a alícuota porcentual AFIP.
     * Ajustar los valores según la tabla de IVA de tu sistema.
     */
    public static double mapCodIvaAAlicuota(Integer codIva) {
        if (codIva == null) return 21.0;
        switch (codIva) {
            case 1: return 21.0;    // IVA 21%
            case 2: return 10.5;    // IVA 10.5%
            case 3: return 0.0;     // Exento
            case 4: return 0.0;     // No gravado
            case 5: return 27.0;    // IVA 27%
            default: return 21.0;
        }
    }

    /**
     * Convierte la unidad de medida de DBTPVIV al código numérico de AFIP.
     * Código 7 = Unidades (el más común).
     */
    private String mapUnidadMedida(String unidad) {
        if (unidad == null || unidad.trim().isEmpty()) return "7";
        switch (unidad.trim().toUpperCase()) {
            case "UN":
            case "U":  return "7";   // Unidades
            case "KG":
            case "KI": return "1";   // Kilogramos
            case "GR": return "2";   // Gramos
            case "LT":
            case "L":  return "5";   // Litros
            case "MT":
            case "M":  return "6";   // Metros
            default:   return "7";   // Unidades por defecto
        }
    }

    // ── Pagos ─────────────────────────────────────────────────────────────────

    private Pagos mapPagos(List<TicketPagoEntity> pagosEntidad) {
        List<Pagos.FormaPago> formas = pagosEntidad.stream()
                .map(pe -> new Pagos.FormaPago(pe.getDescripcion(), pe.getImporte()))
                .collect(Collectors.toList());
        Pagos pagos = new Pagos();
        pagos.setFormasPago(formas);
        return pagos;
    }
}
