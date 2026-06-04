package com.pos.factura.entity.dbtpviv;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "eventos")
public class Evento {

    @EmbeddedId
    private EventoId id;

    @Column(name = "NRO_TICKET")
    private Integer nroTicket;

    @Column(name = "FECHA")
    private Short fecha;

    @Column(name = "HORA")
    private Short hora;

    @Column(name = "CAJERO")
    private Short cajero;

    @Column(name = "CANT_BULTOS")
    private Short cantBultos;

    @Column(name = "IMPORTE_SIN_IVA")
    private Double importeSinIva;

    @Column(name = "IMP_INT")
    private Double impInt;

    @Column(name = "EXENTO")
    private Double exento;

    @Column(name = "IVA1")
    private Double iva1;

    @Column(name = "COD_CLIENTE")
    private Integer codCliente;

    @Column(name = "IVA2")
    private Double iva2;

    @Column(name = "SUC_COMPROBANTE")
    private Short sucComprobante;

    @Column(name = "NRO_COMPROBANTE")
    private Integer nroComprobante;

    @Column(name = "TIPO_EVENTO")
    private Byte tipoEvento;

    @Column(name = "SUCURSAL")
    private Short sucursal;

    @Column(name = "CAJA")
    private Short caja;

    @Column(name = "TARJ_CLIE_AFINIDAD")
    private String tarjClieAfinidad;

    // =========================================
    // GETTERS & SETTERS
    // =========================================

    public EventoId getId() {
        return id;
    }

    public void setId(EventoId id) {
        this.id = id;
    }

    public Integer getNroTicket() {
        return nroTicket;
    }

    public void setNroTicket(Integer nroTicket) {
        this.nroTicket = nroTicket;
    }

    public Short getFecha() {
        return fecha;
    }

    public void setFecha(Short fecha) {
        this.fecha = fecha;
    }

    public Short getHora() {
        return hora;
    }

    public void setHora(Short hora) {
        this.hora = hora;
    }

    public Short getCajero() {
        return cajero;
    }

    public void setCajero(Short cajero) {
        this.cajero = cajero;
    }

    public Short getCantBultos() {
        return cantBultos;
    }

    public void setCantBultos(Short cantBultos) {
        this.cantBultos = cantBultos;
    }

    public Double getImporteSinIva() {
        return importeSinIva;
    }

    public void setImporteSinIva(Double importeSinIva) {
        this.importeSinIva = importeSinIva;
    }

    public Double getImpInt() {
        return impInt;
    }

    public void setImpInt(Double impInt) {
        this.impInt = impInt;
    }

    public Double getExento() {
        return exento;
    }

    public void setExento(Double exento) {
        this.exento = exento;
    }

    public Double getIva1() {
        return iva1;
    }

    public void setIva1(Double iva1) {
        this.iva1 = iva1;
    }

    public Integer getCodCliente() {
        return codCliente;
    }

    public void setCodCliente(Integer codCliente) {
        this.codCliente = codCliente;
    }

    public Double getIva2() {
        return iva2;
    }

    public void setIva2(Double iva2) {
        this.iva2 = iva2;
    }

    public Short getSucComprobante() {
        return sucComprobante;
    }

    public void setSucComprobante(Short sucComprobante) {
        this.sucComprobante = sucComprobante;
    }

    public Integer getNroComprobante() {
        return nroComprobante;
    }

    public void setNroComprobante(Integer nroComprobante) {
        this.nroComprobante = nroComprobante;
    }

    public Byte getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(Byte tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public Short getSucursal() {
        return sucursal;
    }

    public void setSucursal(Short sucursal) {
        this.sucursal = sucursal;
    }

    public Short getCaja() {
        return caja;
    }

    public void setCaja(Short caja) {
        this.caja = caja;
    }

    public String getTarjClieAfinidad() {
        return tarjClieAfinidad;
    }

    public void setTarjClieAfinidad(String tarjClieAfinidad) {
        this.tarjClieAfinidad = tarjClieAfinidad;
    }
    @OneToMany(
            mappedBy = "evento",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL
    )
    private List<EvCont> detalles;
    public List<EvCont> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<EvCont> detalles) {
        this.detalles = detalles;
    }
}