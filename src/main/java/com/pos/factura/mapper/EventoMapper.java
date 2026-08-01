package com.pos.factura.mapper;

import com.pos.factura.dto.SolicitudFacturaRequest;
import com.pos.factura.entity.dbtpviv.ArticuloEntity;
import com.pos.factura.entity.dbtpviv.ClienteEntity;
import com.pos.factura.entity.dbtpviv.EvCont;
import com.pos.factura.entity.dbtpviv.Evento;
import com.pos.factura.entity.dbtpviv.TipoEventoPOS;
import com.pos.factura.entity.posfe.TicketEntity;
import com.pos.factura.entity.posfe.TicketItemEntity;
import com.pos.factura.entity.posfe.TicketPagoEntity;
import com.pos.factura.model.*;
import com.pos.factura.repository.dbtpviv.ArticuloRepository;
import com.pos.factura.repository.dbtpviv.EvBPromoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
/**
 * Convierte Evento (DBTPVIV) + ClienteEntity (DBTPVIV) en SolicitudFacturaRequest.
 *
 * Mapeo Evento → Comprobante:
 *   cajaZ / idEvento       → trazabilidad (no van al comprobante)
 *   nroTicket              → numero del comprobante
 *   importeSinIva          → neto gravado
 *   iva1 + iva2            → IVA total
 *   exento                 → monto exento
 *   tipoEvento             → tipo de comprobante (ajustar según tabla)
 *
 * Mapeo EvCont → DetalleItem:
 *   codArticulo            → busca en ArticuloEntity por COD_INTERNO
 *   cantidad               → cantidad
 *   precioUnitario         → precio_unitario_sin_iva
 *   descuento              → bonificacion_porcentaje
 */
@Component
public class EventoMapper {

    private static final Logger log = LoggerFactory.getLogger(EventoMapper.class);

    private final ArticuloRepository articuloRepository;
    private final EvBPromoRepository evBPromoRepository;

    public EventoMapper(ArticuloRepository articuloRepository, EvBPromoRepository evBPromoRepository) {
        this.articuloRepository = articuloRepository;
        this.evBPromoRepository = evBPromoRepository;
    }

    /**
     * Busca en ev_bpromo el registro (COD_ART, B_PROMO=1) que marca el artículo con descuento,
     * toma su ID_GRUPO y busca el registro hermano (mismo ID_GRUPO, B_PROMO=101) que contiene
     * el descuento en PRECIO_FIJO.
     */
    public Double obtenerPrecioFijoDescuento(Long cajaZ, Long idEvento, Integer codArt) {
        return evBPromoRepository
                .findIdGrupoByCajaZAndIdEventoAndCodArtAndBpromo(cajaZ, idEvento, codArt, (short) 1)
                .flatMap(idGrupo -> evBPromoRepository
                        .findPrecioFijoByCajaZAndIdEventoAndIdGrupoAndBpromo(cajaZ, idEvento, idGrupo, (short) 101))
                .orElse(null);
    }

    public SolicitudFacturaRequest toSolicitud(Evento evento, ClienteEntity cliente, List<EvCont> evCont) {
        SolicitudFacturaRequest solicitud = new SolicitudFacturaRequest();
        solicitud.setCliente(mapCliente(cliente));
        solicitud.setComprobante(mapComprobante(evento, evCont));
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

    private Comprobante mapComprobante(Evento evento, List<EvCont> evCont) {
        Comprobante comprobante = new Comprobante();

        // Tipo de comprobante según TIPO_EVENTO
        // Ajustar según la tabla de tipos de tu sistema
        comprobante.setTipo(mapTipoEvento(evento.getTipoEvento()));

        // Fecha actual como fecha del comprobante
        comprobante.setFecha(java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        comprobante.setVencimiento(comprobante.getFecha());

        comprobante.setMoneda("PES");
        comprobante.setCotizacion("1");
        comprobante.setOperacion("V");
        comprobante.setRubro("Venta de mercaderia");
        comprobante.setNumero("0");
        comprobante.setBonificacion(BigDecimal.ZERO);

        // Items desde ev_cont
        comprobante.setDetalle(mapItems(evCont));

        return comprobante;
    }

    /**
     * Convierte TIPO_EVENTO al tipo de comprobante AFIP usando TipoEventoPOS.
     * Códigos válidos: 3=Factura A, 4=Factura B, 5=NC A, 6=NC B.
     */
    private String mapTipoEvento(Byte tipoEvento) {
        TipoEventoPOS tipo = TipoEventoPOS.fromCodigoODefault(tipoEvento, TipoEventoPOS.FACTURA_B);
        if (!TipoEventoPOS.esCodigoValido(tipoEvento)) {
            log.warn("Tipo de evento desconocido: {}. Se usará {} por defecto",
                    tipoEvento, TipoEventoPOS.FACTURA_B.getDescripcionLegible());
        }
        return tipo.getDescripcionAfip();
    }

    // ── Items (EvCont → DetalleItem) ─────────────────────────────────────────

    private List<DetalleItem> mapItems(List<EvCont> evConts) {
        if (evConts == null) return new ArrayList<>();
        return evConts.stream().map(this::mapItem).collect(Collectors.toList());
    }

    private DetalleItem mapItem(EvCont ec) {
        DetalleItem item = new DetalleItem();

        // Cantidad: Float en EvCont → BigDecimal
        item.setCantidad(ec.getCantidad() != null
                ? BigDecimal.valueOf(ec.getCantidad())
                : BigDecimal.ONE);

        // Descuento: Double → BigDecimal porcentaje
        item.setBonificacionPorcentaje(ec.getDescuento() != null
                ? BigDecimal.valueOf(ec.getDescuento())
                : BigDecimal.ZERO);

        item.setAfectaStock("S");
        item.setLeyenda("");
        item.setProducto(mapProducto(ec));
        return item;
    }

    private Producto mapProducto(EvCont ec) {
        Producto producto = new Producto();

        // Buscar artículo en DBTPVIV por COD_ARTICULO
        Optional<ArticuloEntity> artOpt = ec.getCodArticulo() != null
                ? articuloRepository.findActivoByCodInterno(ec.getCodArticulo())
                : Optional.empty();

        if (artOpt.isPresent()) {
            ArticuloEntity art = artOpt.get();
            producto.setCodigo(String.valueOf(art.getCodInterno()));
            producto.setDescripcion(art.getNombre() != null
                    ? art.getNombre() : "SIN DESCRIPCION");
            producto.setUnidadMedida(TicketMapper.mapUnidadMedida(art.getUnidad()));
            // Alícuota desde el artículo
            producto.setAlicuota(mapCodIvaAAlicuota(art.getCodIva()));
        } else {
            log.warn("Artículo COD_INTERNO={} no encontrado en DBTPVIV", ec.getCodArticulo());
            producto.setCodigo(ec.getCodArticulo() != null
                    ? String.valueOf(ec.getCodArticulo()) : "0");
            producto.setDescripcion("ARTICULO " + ec.getCodArticulo());
            producto.setUnidadMedida("7");
            producto.setAlicuota(21.0);
        }

        // Precio unitario sin IVA — desde EvCont (precio histórico de la venta)
        producto.setPrecioUnitarioSinIva(ec.getPrecioUnitario() != null
                ? BigDecimal.valueOf(ec.getPrecioUnitario())
                : BigDecimal.ZERO);

        producto.setListaPrecios("LISTA GENERAL");
        producto.setImpuestosInternosAlicuota(0.0);
        return producto;
    }

    /** Ajustar según la tabla de IVA de tu sistema */
    private double mapCodIvaAAlicuota(Integer codIva) {
        if (codIva == null) return 21.0;
        switch (codIva) {
            case 0: return 21.0;
            case 1: return 10.5;
            case 2: return 0.0;
            case 4: return 0.0;
            default: return 21.0;
        }
    }

    // ── Mapeo Evento → TicketEntity ───────────────────────────────────────────

    /**
     * Convierte un Evento de DBTPVIV en un TicketEntity de posFE.
     *
     * Mapeo de campos:
     *   eventos.cajaZ / idEvento → numeroTicket ("EV-{cajaZ}-{idEvento}")
     *   eventos.tipoEvento       → tipoComprobante  (via TipoEventoPOS)
     *   eventos.codCliente       → codCliente
     *   eventos.importeSinIva    → netoGravado
     *   eventos.iva1 + iva2      → ivaTotal
     *   eventos.exento           → exentos
     *   total calculado          → total
     *   ev_cont (detalles)       → items  (TicketItemEntity)
     *
     * Nota: los pagos (EvMedios) no se mapean aquí — cargarlos por separado
     * con EvMediosRepository si los necesitás en el TicketEntity.
     */
    public TicketEntity toTicketEntity(Evento evento, List<EvCont> evCont) {
        TicketEntity ticket = new TicketEntity();

        // ── Identificación ────────────────────────────────────────────────────
        // deberia venir en la eventos scado de factumem
        ticket.setNumeroTicket("EV-" + evento.getCajaZ() + "-" + evento.getIdEvento());

        // ── Tipo de comprobante ───────────────────────────────────────────────
        TipoEventoPOS tipoPos = TipoEventoPOS.fromCodigoODefault(
                evento.getTipoEvento(), TipoEventoPOS.FACTURA_B);
        ticket.setTipoComprobante(mapTipoComprobanteEnum(tipoPos));

        // ── Cliente ───────────────────────────────────────────────────────────
        if (evento.getCodCliente() != null) {
            ticket.setCodCliente(evento.getCodCliente().longValue());
        }

        // ── Fecha ─────────────────────────────────────────────────────────────
        // FECHA en eventos es Short — convertir al formato de tu BD
        // Si almacenás la fecha como días desde una época, ajustar la conversión
        ticket.setFecha(java.time.LocalDate.now());
        ticket.setFechaVencimiento(java.time.LocalDate.now());

        // ── Moneda y cotización ───────────────────────────────────────────────
        ticket.setMoneda("PES");
        ticket.setCotizacion(BigDecimal.ONE);
        ticket.setBonificacion(BigDecimal.ZERO);

        // ── Totales desde Evento ──────────────────────────────────────────────
        ticket.setNetoGravado(evento.getImporteSinIva() != null
                ? BigDecimal.valueOf(evento.getImporteSinIva()).setScale(2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        BigDecimal iva1 = evento.getIva1() != null
                ? BigDecimal.valueOf(evento.getIva1()) : BigDecimal.ZERO;
        BigDecimal iva2 = evento.getIva2() != null
                ? BigDecimal.valueOf(evento.getIva2()) : BigDecimal.ZERO;
        ticket.setIvaTotal(iva1.add(iva2).setScale(2, java.math.RoundingMode.HALF_UP));

        ticket.setExentos(evento.getExento() != null
                ? BigDecimal.valueOf(evento.getExento()).setScale(2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // Total = neto + IVA + exento
        ticket.setTotal(ticket.getNetoGravado()
                .add(ticket.getIvaTotal())
                .add(ticket.getExentos())
                .setScale(2, java.math.RoundingMode.HALF_UP));

        // ── Estado ────────────────────────────────────────────────────────────
        ticket.setEstado(TicketEntity.EstadoTicket.PENDIENTE);
        ticket.setFechaAlta(java.time.LocalDateTime.now());

        // ── Items desde ev_cont ───────────────────────────────────────────────
        if (evCont != null && !evCont.isEmpty()) {
            List<TicketItemEntity> items = mapItemsToTicket(evCont, evento.getCajaZ(), evento.getIdEvento(), ticket);
            ticket.setItems(items);
        }

        return ticket;
    }

    /**
     * Convierte los EvCont del evento en TicketItemEntity para el ticket.
     */
        private List<TicketItemEntity> mapItemsToTicket(List<EvCont> evCont, Long cajaZ, Long idEvento, TicketEntity ticket)  {
        List<TicketItemEntity> items = new ArrayList<>();
        int orden = 1;

        for (EvCont ec : evCont) {
            if (ec.getOrigen() != null && ec.getOrigen() == (byte) 4) {
                continue;
            }

            TicketItemEntity item = new TicketItemEntity();
            item.setTicket(ticket);
            item.setOrden(orden++);

            // COD_ARTICULO → codInterno
            item.setCodInterno(ec.getCodArticulo());

            // CANTIDAD: Float → BigDecimal
            item.setCantidad(ec.getCantidad() != null
                    ? BigDecimal.valueOf(ec.getCantidad()).setScale(2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ONE);

            // PRECIO_UNITARIO → precioUnitarioSinIva (precio histórico de la venta)
            item.setPrecioUnitarioSinIva(ec.getImporteSinIva() != null
                    ? BigDecimal.valueOf(ec.getImporteSinIva()).setScale(2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);

            // Alícuota IVA desde el artículo en DBTPVIV
            double alicuota = 21.0; // default
            if (ec.getCodArticulo() != null) {
                Optional<ArticuloEntity> artOpt =
                        articuloRepository.findActivoByCodInterno(ec.getCodArticulo());
                if (artOpt.isPresent()) {
                    alicuota = mapCodIvaAAlicuota(artOpt.get().getCodIva());
                } else {
                    log.warn("Artículo COD_INTERNO={} no encontrado al mapear TicketItem", ec.getCodArticulo());
                }
            }
            item.setAlicuotaIva(BigDecimal.valueOf(alicuota));

            // DESCUENTO → bonificacionPorcentaje (resuelto vía ev_bpromo, no viene en ev_cont)
            Double precioFijoDescuento = ec.getCodArticulo() != null
                    ? obtenerPrecioFijoDescuento(cajaZ, idEvento, ec.getCodArticulo())
                    : null;
            item.setBonificacionPorcentaje(precioFijoDescuento != null
                    ? BigDecimal.valueOf(precioFijoDescuento).setScale(2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);

            item.setAfectaStock("S");
            item.setLeyenda("");

            items.add(item);
        }

        Double descuentoTicket = obtenerDescuentoTicket(cajaZ, idEvento); //3383.375
        if (descuentoTicket != null && descuentoTicket != 0 && !items.isEmpty()) {
            BigDecimal descuentoProrrateado = BigDecimal.valueOf(descuentoTicket)
                    .divide(BigDecimal.valueOf(items.size()), 2, java.math.RoundingMode.HALF_UP);
            for (TicketItemEntity item : items) {
                item.setBonificacionPorcentaje(item.getBonificacionPorcentaje().add(descuentoProrrateado));
            }
        }

        return items;
    }

    /**
     * Busca en ev_bpromo el registro (B_PROMO=4) que marca el descuento del ticket,
     * toma su ID_GRUPO y busca el registro hermano (mismo ID_GRUPO, B_PROMO=101) que contiene
     * el descuento en PRECIO_FIJO.
     */
    public Double obtenerDescuentoTicket(Long cajaZ, Long idEvento) {
        return evBPromoRepository
                .findIdGrupoByCajaZAndIdEventoAndBpromo(cajaZ, idEvento, (short) 4)
                .flatMap(idGrupo -> evBPromoRepository
                        .findPrecioFijoByCajaZAndIdEventoAndIdGrupoAndBpromo(cajaZ, idEvento, idGrupo, (short) 101))
                .orElse(null);
    }

    /**
     * Convierte TipoEventoPOS al enum TipoComprobante de TicketEntity.
     */
    private TicketEntity.TipoComprobante mapTipoComprobanteEnum(TipoEventoPOS tipoPos) {
        switch (tipoPos) {
            case FACTURA_A:      return TicketEntity.TipoComprobante.FACTURA_A;
            case FACTURA_B:      return TicketEntity.TipoComprobante.FACTURA_B;
            case NOTA_CREDITO_A: return TicketEntity.TipoComprobante.NOTA_CREDITO_A;
            case NOTA_CREDITO_B: return TicketEntity.TipoComprobante.NOTA_CREDITO_B;
            default:             return TicketEntity.TipoComprobante.FACTURA_B;
        }
    }
}
