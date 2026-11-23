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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        return evBPromoRepository.sumPrecioFijoDescuentoByCajaZAndIdEventoAndCodArt(cajaZ, idEvento, codArt);
    }

    /**
     * Busca en ev_bpromo el registro (COD_ART = , B_PROMO=4 precio_fijo >0) que marca el artículo con descuento,
     * toma su ID_GRUPO y busca el registro hermano (mismo ID_GRUPO, B_PROMO=101) que contiene
     * el descuento en PRECIO_FIJO. Ese descuento debe
     */
    public Double obtenerDescuentoPerfil(Long cajaZ, Long idEvento,  Integer codArt, BigDecimal subtotalItemConDescuento) {
        if (evBPromoRepository.existsByCajaZAndIdEventoAndCodArtAndBpromoAndPrecioFijoCero(cajaZ, idEvento, codArt, (short) 1)) {
            return 0.0;
        }
        return evBPromoRepository
                .findIdGrupoByCajaZAndIdEventoAndBpromoAndPrecioFijoCero(cajaZ, idEvento, (short) 4)
                .flatMap(idGrupo -> {
                    long cantidad = evBPromoRepository
                            .countByCajaZAndIdEventoAndIdGrupoAndBpromoAndPrecioFijoCero(cajaZ, idEvento, idGrupo, (short) 4);
                    if (cantidad == 0 || subtotalItemConDescuento == null || subtotalItemConDescuento.signum() == 0) {
                        return Optional.<Double>empty();
                    }
                    return evBPromoRepository
                            .findPrecioFijoByCajaZAndIdEventoAndIdGrupoAndPrecioFijoMayorCero(cajaZ, idEvento, idGrupo)
                            .map(precioFijo -> precioFijo / subtotalItemConDescuento.doubleValue());
                })
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
        if (cod == null) return "CUIT";
        switch (cod.trim().toUpperCase()) {
            case "1": return "DNI";
            case "3": return "CUIT";
            case "4": return "CEDEULA";
            case "2": return "CUIL";
            case "5": return "PASAPORTE";
            default:  return "CUIT";
        }
    }

    /** Ajustar según los valores reales de COND_IVA en tu BD */
    private String mapCondicionIva(String cond) {
        if (cond == null) return "CF";
        switch (cond.trim()) {
            case "1": return "RI";
            case "4": return "EX";
            case "5": return "CF";
            case "6": return "MO";
            case "7": return "NC";
            case "8": return "LIB";
            case "9": return "EXT";
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
        comprobante.setPercepcion(evento.getPercepcionDgi() != null
                ? BigDecimal.valueOf(evento.getPercepcionDgi()).setScale(2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

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

        Producto producto = mapProducto(ec);
        item.setProducto(producto);

        // Descuento: EvCont.DESCUENTO es un monto en $, no un porcentaje: se convierte sobre el subtotal.
        BigDecimal subtotalItem = producto.getPrecioUnitarioSinIva().multiply(item.getCantidad());
        item.setBonificacionPorcentaje(ec.getDescuento() != null && subtotalItem.signum() > 0
                ? BigDecimal.valueOf(ec.getDescuento())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(subtotalItem, 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        item.setAfectaStock("S");
        item.setLeyenda("");
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
            producto.setAlicuota(mapCodIvaAAlicuota(art.getCodIva(), art.getGravado()));
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
    private double mapCodIvaAAlicuota(Integer codIva, String gravado) {
        if (codIva == null) return 21.0;
        if( gravado.compareToIgnoreCase("N") == 0 ) codIva = 2;
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
        ticket.setTipoComprobanteAnt(evento.getTipoComprobanteAnt());
        ticket.setNroBoletaAnterior(evento.getNroBoletaAnterior());
        ticket.setNroCajaAnt(evento.getNroCajaAnt());

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

        ticket.setPercepcion(evento.getPercepcionDgi() != null
                ? BigDecimal.valueOf(evento.getPercepcionDgi()).setScale(2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // Total = neto + IVA + exento + percepcion
        ticket.setTotal(ticket.getNetoGravado()
                .add(ticket.getIvaTotal())
                .add(ticket.getExentos())
                .add(ticket.getPercepcion())
                .setScale(2, java.math.RoundingMode.HALF_UP));

        // ── Estado ────────────────────────────────────────────────────────────
        ticket.setEstado(TicketEntity.EstadoTicket.PENDIENTE);
        ticket.setFechaAlta(java.time.LocalDateTime.now());

        // ── Items desde ev_cont ───────────────────────────────────────────────
        if (evCont != null && !evCont.isEmpty()) {
            List<TicketItemEntity> items = mapItemsToTicket(evCont, evento.getCajaZ(), evento.getIdEvento(), ticket);
            ticket.setItems(items);
        }

        Double bonificacion = evBPromoRepository.sumPrecioFijoByCajaZAndIdEvento(evento.getCajaZ(), evento.getIdEvento());
        ticket.setBonificacion(bonificacion != null
                ? BigDecimal.valueOf(bonificacion).setScale(2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        return ticket;
    }

    /**
     * Convierte los EvCont del evento en TicketItemEntity para el ticket.
     */
        private List<TicketItemEntity> mapItemsToTicket(List<EvCont> evCont, Long cajaZ, Long idEvento, TicketEntity ticket)  {
        List<TicketItemEntity> items = new ArrayList<>();
        int orden = 1;

        List<Integer> codArticulosEnPromo = evBPromoRepository.findCodArtByCajaZAndIdEvento(cajaZ, idEvento);
        BigDecimal subtotalItemConDescuento = BigDecimal.ZERO;
        for (EvCont ec : evCont) {
            if (Objects.equals(ec.getCajaZ(), cajaZ)
                    && Objects.equals(ec.getIdEvento(), idEvento)
                    && !codArticulosEnPromo.contains(ec.getCodArticulo())
                    && ec.getOrigen() != (byte) 4
                    && !esDescuentoGlobal(ec)) {
                BigDecimal precioSinIva = ec.getImporteSinIva() != null
                        ? BigDecimal.valueOf(ec.getImporteSinIva() + ec.getExento()) : BigDecimal.ZERO;
                BigDecimal iva = BigDecimal.ZERO;
                if (ec.getIva1() != null) {
                    iva = iva.add(BigDecimal.valueOf(ec.getIva1()));
                }
                if (ec.getIva2() != null) {
                    iva = iva.add(BigDecimal.valueOf(ec.getIva2()));
                }
                BigDecimal cantidadLinea = ec.getCantidad() != null ? BigDecimal.valueOf(ec.getCantidad()) : BigDecimal.ONE;
                subtotalItemConDescuento = subtotalItemConDescuento.add(precioSinIva.add(iva).abs().multiply(cantidadLinea));
            }
        }

        // Cada anulación (cantidad negativa) se cancela contra la línea original del mismo artículo, cantidad y precio:
        // ninguna de las dos se envía (los pesables no se agrupan y quedarían como ítem con cantidad negativa)
        java.util.Set<EvCont> anuladas = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (EvCont anulacion : evCont) {
            if (anulacion.getOrigen() == null || anulacion.getOrigen() != (byte) 1
                    || anulacion.getCantidad() == null || anulacion.getCantidad() >= 0) {
                continue;
            }
            evCont.stream()
                    .filter(ec -> !anuladas.contains(ec)
                            && Objects.equals(ec.getOrigen(), anulacion.getOrigen())
                            && ec.getCantidad() != null && ec.getCantidad() > 0
                            && Objects.equals(ec.getCodArticulo(), anulacion.getCodArticulo())
                            && Objects.equals(ec.getCodBarra(), anulacion.getCodBarra())
                            && Objects.equals(ec.getPrecioUnitario(), anulacion.getPrecioUnitario())
                            && Math.abs(ec.getCantidad() + anulacion.getCantidad()) < 0.0005)
                    .findFirst()
                    .ifPresent(original -> {
                        anuladas.add(original);
                        anuladas.add(anulacion);
                    });
        }

        Map<String, TicketItemEntity> itemsPorClaveOrigen1 = new HashMap<>();
        Map<String, BigDecimal> importePorClaveOrigen1 = new HashMap<>();
        Map<TicketItemEntity, Double> precioFijoPorItem = new java.util.IdentityHashMap<>();
        Map<TicketItemEntity, Double> perfilPorItem = new java.util.IdentityHashMap<>();
        java.util.Set<Integer> articulosConPrecioFijo = new java.util.HashSet<>();
        for (EvCont ec : evCont) {
            if (ec.getOrigen() != null && ec.getOrigen() == (byte) 4 || esDescuentoGlobal(ec) || anuladas.contains(ec)) {
                continue;
            }

            // CANTIDAD: Float → BigDecimal
            BigDecimal cantidadEc = ec.getCantidad() != null
                    ? BigDecimal.valueOf(ec.getCantidad()).setScale(3, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ONE;

            // PRECIO_UNITARIO → precioUnitarioSinIva (precio histórico de la venta, incluye exento)
            BigDecimal importeSinIvaItem = ec.getImporteSinIva() != null
                    ? BigDecimal.valueOf(ec.getImporteSinIva()) : BigDecimal.ZERO;
            BigDecimal exentoItem = ec.getExento() != null
                    ? BigDecimal.valueOf(ec.getExento()) : BigDecimal.ZERO;
            BigDecimal impIntItem = ec.getImpInt() != null
                    ? BigDecimal.valueOf(ec.getImpInt()) : BigDecimal.ZERO;

            Optional<ArticuloEntity> artOpt = ec.getCodArticulo() != null
                    ? articuloRepository.findActivoByCodInterno(ec.getCodArticulo())
                    : Optional.empty();
            boolean pesable = artOpt.map(a -> "S".equals(a.getPesable())).orElse(false);

            boolean esOrigen1 = ec.getOrigen() != null && ec.getOrigen() == (byte) 1 && !pesable;
            String claveItem = ec.getCodArticulo() + "|" + ec.getCodBarra();
            // Las anulaciones traen cantidad y precio negativos: el signo lo da la cantidad
            BigDecimal importeLinea = importeSinIvaItem.add(exentoItem).abs().multiply(cantidadEc);
            if (esOrigen1 && itemsPorClaveOrigen1.containsKey(claveItem)) {
                // Precio promedio ponderado sobre los importes reales de cada línea agrupada
                TicketItemEntity itemExistente = itemsPorClaveOrigen1.get(claveItem);
                BigDecimal nuevaCantidad = itemExistente.getCantidad().add(cantidadEc);
                BigDecimal importeAcumulado = importePorClaveOrigen1.merge(claveItem, importeLinea, BigDecimal::add);
                if (nuevaCantidad.signum() != 0) {
                    itemExistente.setPrecioUnitarioSinIva(
                            importeAcumulado.divide(nuevaCantidad, 4, java.math.RoundingMode.HALF_UP));
                }
                itemExistente.setCantidad(nuevaCantidad);
                continue;
            }

            TicketItemEntity item = new TicketItemEntity();
            item.setTicket(ticket);
            item.setOrden(orden++);

            // COD_ARTICULO → codInterno
            item.setCodInterno(ec.getCodArticulo());

            item.setCantidad(cantidadEc);

            item.setPrecioUnitarioSinIva(importeSinIvaItem.add(exentoItem).abs().setScale(4, java.math.RoundingMode.HALF_UP));
            item.setImpuestoInterno(impIntItem.abs());

            if (esOrigen1) {
                itemsPorClaveOrigen1.put(claveItem, item);
                importePorClaveOrigen1.put(claveItem, importeLinea);
            }

            // Alícuota IVA desde el artículo en DBTPVIV
            double alicuota = 21.0; // default
            double ivaEc = (ec.getIva1() != null ? ec.getIva1() : 0.0) + (ec.getIva2() != null ? ec.getIva2() : 0.0);
            if (ec.getOrigen() != null && ec.getOrigen() == (byte) 5 && importeSinIvaItem.signum() != 0) {
                // Recargos ORIGEN=5: la alícuota es la que usó el POS (IVA/IMPORTE_SIN_IVA), no la del artículo
                double ratio = ivaEc / importeSinIvaItem.doubleValue() * 100;
                alicuota = java.util.stream.DoubleStream.of(0.0, 10.5, 21.0)
                        .boxed().min(java.util.Comparator.comparingDouble(a -> Math.abs(a - ratio))).get();
            } else if (ec.getCodArticulo() != null) {
                if (artOpt.isPresent()) {
                    alicuota = mapCodIvaAAlicuota(artOpt.get().getCodIva(),artOpt.get().getGravado() );
                } else {
                    log.warn("Artículo COD_INTERNO={} no encontrado al mapear TicketItem", ec.getCodArticulo());
                }
            }
            item.setAlicuotaIva(BigDecimal.valueOf(alicuota));

            // DESCUENTO → bonificacionPorcentaje (resuelto vía ev_bpromo, no viene en ev_cont)
            // ev_bpromo.PRECIO_FIJO es un monto en $, no un porcentaje: se convierte sobre el subtotal del ítem.
            // El % sobre PRECIO_FIJO se calcula después del loop, con la cantidad y precio de la línea ya agrupada
            Double precioFijoDescuento = ec.getCodArticulo() != null && articulosConPrecioFijo.add(ec.getCodArticulo())
                    ? obtenerPrecioFijoDescuento(cajaZ, idEvento, ec.getCodArticulo())
                    : null;
            if (precioFijoDescuento != null) {
                precioFijoPorItem.put(item, precioFijoDescuento);
            }
            item.setBonificacionPorcentaje(BigDecimal.ZERO);
            Double precioDescuentoPerfil = ec.getCodArticulo() != null
                    ? obtenerDescuentoPerfil(cajaZ, idEvento, ec.getCodArticulo(), subtotalItemConDescuento)
                    : null;
            if (precioDescuentoPerfil != null) {
                perfilPorItem.put(item, precioDescuentoPerfil);
                item.setBonificacionPorcentaje(item.getBonificacionPorcentaje()
                        .add(BigDecimal.valueOf(precioDescuentoPerfil).multiply(BigDecimal.valueOf(100)).setScale(2, java.math.RoundingMode.HALF_UP)));
            }


            item.setAfectaStock("S");
            item.setLeyenda("");


            items.add(item);
        }
        items.removeIf(it -> it.getCantidad().abs().compareTo(BigDecimal.valueOf(0.02)) < 0);
        BigDecimal cien = BigDecimal.valueOf(100);
        // Descuentos ya prorrateados por el POS en las líneas ORIGEN=4 (promos PRECIO_FIJO + descuento del ticket),
        // agrupados por alícuota: el descuento de perfil del grupo es lo que queda después de las promos por artículo
        Map<Double, BigDecimal> descuentoOrigen4 = descuentoPorAlicuota(evCont, items, (byte) 4);
        boolean usarOrigen4 = !descuentoOrigen4.isEmpty();
        Map<Double, BigDecimal> subtotalPerfilPorAlicuota = new HashMap<>();
        Map<Double, BigDecimal> precioFijoPorAlicuota = new HashMap<>();
        if (usarOrigen4) {
            for (TicketItemEntity item : items) {
                BigDecimal subtotalLinea = subtotalConIva(item);
                if (subtotalLinea.signum() <= 0) {
                    continue;
                }
                Double alicuota = item.getAlicuotaIva().doubleValue();
                Double fraccionPerfil = perfilPorItem.get(item);
                if (fraccionPerfil != null && fraccionPerfil > 0) {
                    subtotalPerfilPorAlicuota.merge(alicuota, subtotalLinea, BigDecimal::add);
                }
                if (precioFijoPorItem.containsKey(item)) {
                    precioFijoPorAlicuota.merge(alicuota, BigDecimal.valueOf(precioFijoPorItem.get(item)), BigDecimal::add);
                }
            }
        }
        Map<Double, BigDecimal> descuentoObjetivo = new HashMap<>(descuentoOrigen4);
        Map<Double, BigDecimal> descuentoAplicado = new HashMap<>();
        Map<Double, TicketItemEntity> ultimoConDescuento = new HashMap<>();
        for (TicketItemEntity item : items) {
            Double precioFijo = precioFijoPorItem.get(item);
            Double fraccionPerfil = perfilPorItem.get(item);
            if (usarOrigen4 && fraccionPerfil != null && fraccionPerfil <= 0) {
                fraccionPerfil = null;
            }
            if (precioFijo == null && fraccionPerfil == null) {
                continue;
            }
            BigDecimal subtotalLinea = subtotalConIva(item);
            if (subtotalLinea.signum() <= 0) {
                continue;
            }
            Double alicuota = item.getAlicuotaIva().doubleValue();
            Double clave = usarOrigen4 ? alicuota : null;
            if (fraccionPerfil != null) {
                BigDecimal pctPerfil;
                if (usarOrigen4) {
                    BigDecimal objetivoGrupo = descuentoOrigen4.getOrDefault(alicuota, BigDecimal.ZERO)
                            .subtract(precioFijoPorAlicuota.getOrDefault(alicuota, BigDecimal.ZERO));
                    pctPerfil = objetivoGrupo.max(BigDecimal.ZERO).multiply(cien)
                            .divide(subtotalPerfilPorAlicuota.get(alicuota), 2, java.math.RoundingMode.HALF_UP);
                    item.setBonificacionPorcentaje(pctPerfil);
                } else {
                    pctPerfil = BigDecimal.valueOf(fraccionPerfil).multiply(cien).setScale(2, java.math.RoundingMode.HALF_UP);
                    descuentoObjetivo.merge(clave, subtotalLinea.multiply(BigDecimal.valueOf(fraccionPerfil)), BigDecimal::add);
                }
                descuentoAplicado.merge(clave, subtotalLinea.multiply(pctPerfil).divide(cien), BigDecimal::add);
            }
            if (precioFijo != null) {
                BigDecimal pct = BigDecimal.valueOf(precioFijo).multiply(cien).divide(subtotalLinea, 2, java.math.RoundingMode.HALF_UP);
                item.setBonificacionPorcentaje(item.getBonificacionPorcentaje().add(pct));
                if (!usarOrigen4) {
                    descuentoObjetivo.merge(clave, BigDecimal.valueOf(precioFijo), BigDecimal::add);
                }
                descuentoAplicado.merge(clave, subtotalLinea.multiply(pct).divide(cien), BigDecimal::add);
            }
            ultimoConDescuento.put(clave, item);
        }
        // Descuento global del ticket (ORIGEN=5 negativo, ej. variación por medio de pago):
        // se prorratea sobre todos los ítems de la alícuota con la que el POS registró el IVA
        for (Map.Entry<Double, BigDecimal> global : descuentoPorAlicuota(evCont, items, (byte) 5).entrySet()) {
            Double alicuota = global.getKey();
            BigDecimal subtotalGrupo = BigDecimal.ZERO;
            for (TicketItemEntity item : items) {
                BigDecimal subtotalLinea = subtotalConIva(item);
                if (alicuota.equals(item.getAlicuotaIva().doubleValue()) && subtotalLinea.signum() > 0) {
                    subtotalGrupo = subtotalGrupo.add(subtotalLinea);
                }
            }
            if (subtotalGrupo.signum() <= 0) {
                log.warn("Descuento global de {} sin ítems con alícuota {}", global.getValue(), alicuota);
                continue;
            }
            BigDecimal pct = global.getValue().multiply(cien).divide(subtotalGrupo, 2, java.math.RoundingMode.HALF_UP);
            for (TicketItemEntity item : items) {
                BigDecimal subtotalLinea = subtotalConIva(item);
                if (alicuota.equals(item.getAlicuotaIva().doubleValue()) && subtotalLinea.signum() > 0) {
                    item.setBonificacionPorcentaje(item.getBonificacionPorcentaje().add(pct));
                    descuentoAplicado.merge(alicuota, subtotalLinea.multiply(pct).divide(cien), BigDecimal::add);
                    ultimoConDescuento.put(alicuota, item);
                }
            }
            descuentoObjetivo.merge(alicuota, global.getValue(), BigDecimal::add);
        }
        // El % redondeado a 2 decimales no reproduce exacto el descuento: la diferencia se absorbe en el precio
        // del último ítem con descuento (por alícuota cuando se usa ORIGEN=4, para no mover importe entre grupos)
        ultimoConDescuento.forEach((clave, ultimo) -> {
            BigDecimal diferencia = descuentoObjetivo.getOrDefault(clave, BigDecimal.ZERO)
                    .subtract(descuentoAplicado.getOrDefault(clave, BigDecimal.ZERO));
            BigDecimal factorNeto = ultimo.getCantidad().abs()
                    .multiply(BigDecimal.ONE.subtract(ultimo.getBonificacionPorcentaje().divide(cien)))
                    .multiply(BigDecimal.ONE.add(ultimo.getAlicuotaIva().divide(cien, 4, java.math.RoundingMode.HALF_UP)));
            if (factorNeto.signum() > 0) {
                ultimo.setPrecioUnitarioSinIva(ultimo.getPrecioUnitarioSinIva()
                        .subtract(diferencia.divide(factorNeto, 4, java.math.RoundingMode.HALF_UP)));
            }
        });
        return items;
    }

    private BigDecimal subtotalConIva(TicketItemEntity item) {
        BigDecimal factorIva = BigDecimal.ONE.add(item.getAlicuotaIva().divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP));
        return item.getPrecioUnitarioSinIva().multiply(item.getCantidad().abs()).multiply(factorIva);
    }

    /**
     * Suma el descuento con IVA de las líneas ORIGEN=4 por alícuota. Las líneas con EXENTO van a alícuota 0;
     * el resto se asigna a la alícuota de los ítems más cercana a IVA/IMPORTE_SIN_IVA (el importe puede incluir
     * la parte de impuestos internos, por eso no da la alícuota exacta).
     */
    private static boolean esDescuentoGlobal(EvCont ec) {
        double importe = (ec.getImporteSinIva() != null ? ec.getImporteSinIva() : 0.0)
                + (ec.getExento() != null ? ec.getExento() : 0.0);
        return ec.getOrigen() != null && ec.getOrigen() == (byte) 5 && importe < 0;
    }

    private Map<Double, BigDecimal> descuentoPorAlicuota(List<EvCont> evCont, List<TicketItemEntity> items, byte origen) {
        java.util.Set<Double> alicuotasGravadas = new java.util.HashSet<>();
        for (TicketItemEntity item : items) {
            if (item.getAlicuotaIva().signum() > 0) {
                alicuotasGravadas.add(item.getAlicuotaIva().doubleValue());
            }
        }
        Map<Double, BigDecimal> descuento = new HashMap<>();
        for (EvCont ec : evCont) {
            if (ec.getOrigen() == null || ec.getOrigen() != origen || origen == (byte) 5 && !esDescuentoGlobal(ec)) {
                continue;
            }
            double importe = ec.getImporteSinIva() != null ? ec.getImporteSinIva() : 0.0;
            double exento = ec.getExento() != null ? ec.getExento() : 0.0;
            double iva = (ec.getIva1() != null ? ec.getIva1() : 0.0) + (ec.getIva2() != null ? ec.getIva2() : 0.0);
            // Las anulaciones traen cantidad -1 e importes con el signo invertido: el signo lo da el importe
            double cantidad = ec.getCantidad() != null ? Math.abs(ec.getCantidad()) : 1.0;
            double impInt = ec.getImpInt() != null ? ec.getImpInt() : 0.0;
            Double alicuota;
            if (exento != 0.0) {
                alicuota = 0.0;
            } else if (importe != 0.0 && !alicuotasGravadas.isEmpty()) {
                double ratio = iva / importe * 100;
                alicuota = alicuotasGravadas.stream()
                        .min(java.util.Comparator.comparingDouble(a -> Math.abs(a - ratio))).get();
            } else {
                continue;
            }
            descuento.merge(alicuota, BigDecimal.valueOf(importe + exento + iva + impInt).multiply(BigDecimal.valueOf(cantidad)).negate(),
                    BigDecimal::add);
        }
        return descuento;
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
            case TICKET_FACTURA_A: return TicketEntity.TipoComprobante.FACTURA_A;
            case TICKET_FACTURA_B: return TicketEntity.TipoComprobante.FACTURA_B;
            default:             return TicketEntity.TipoComprobante.FACTURA_B;
        }
    }
}


