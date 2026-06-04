package com.pos.factura.entity.dbtpviv;

import javax.persistence.*;

@Entity
@Table(name = "ev_cont")
public class EvCont {

    @EmbeddedId
    private EvContId id;

    // =====================================================
    // RELACION MANY TO ONE
    // =====================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(
                    name = "CAJA_Z",
                    referencedColumnName = "CAJA_Z",
                    insertable = false,
                    updatable = false
            ),
            @JoinColumn(
                    name = "ID_EVENTO",
                    referencedColumnName = "ID_EVENTO",
                    insertable = false,
                    updatable = false
            )
    })
    private Evento evento;

    // =====================================================
    // CAMPOS
    // =====================================================

    @Column(name = "COD_ARTICULO")
    private Integer codArticulo;

    @Column(name = "CANTIDAD")
    private Float cantidad;

    @Column(name = "PRECIO_UNITARIO")
    private Double precioUnitario;

    @Column(name = "TOTAL")
    private Double total;

    @Column(name = "COD_BARRA")
    private String codBarra;

    @Column(name = "DESCUENTO")
    private Double descuento;

    @Column(name = "COD_ARTICULO_ALFA")
    private String codArticuloAlfa;

    @Column(name = "COD_CLASIFICACION")
    private String codClasificacion;

    // =====================================================
    // GETTERS & SETTERS
    // =====================================================

    public EvContId getId() {
        return id;
    }

    public void setId(EvContId id) {
        this.id = id;
    }

    public Evento getEvento() {
        return evento;
    }

    public void setEvento(Evento evento) {
        this.evento = evento;
    }

    public Integer getCodArticulo() {
        return codArticulo;
    }

    public void setCodArticulo(Integer codArticulo) {
        this.codArticulo = codArticulo;
    }

    public Float getCantidad() {
        return cantidad;
    }

    public void setCantidad(Float cantidad) {
        this.cantidad = cantidad;
    }

    public Double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(Double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public String getCodBarra() {
        return codBarra;
    }

    public void setCodBarra(String codBarra) {
        this.codBarra = codBarra;
    }

    public Double getDescuento() {
        return descuento;
    }

    public void setDescuento(Double descuento) {
        this.descuento = descuento;
    }

    public String getCodArticuloAlfa() {
        return codArticuloAlfa;
    }

    public void setCodArticuloAlfa(String codArticuloAlfa) {
        this.codArticuloAlfa = codArticuloAlfa;
    }

    public String getCodClasificacion() {
        return codClasificacion;
    }

    public void setCodClasificacion(String codClasificacion) {
        this.codClasificacion = codClasificacion;
    }
}