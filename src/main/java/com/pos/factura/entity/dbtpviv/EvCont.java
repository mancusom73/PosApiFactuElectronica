package com.pos.factura.entity.dbtpviv;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "ev_cont")
@IdClass(EvContId.class)
public class EvCont {

   /* @EmbeddedId
    private EvContId id;*/

    @Id
    @Column(name = "CAJA_Z")
    private Long cajaZ;

    @Id
    @Column(name = "ID_EVENTO")
    private Long idEvento;

    @Column(name = "POSICION")
    private Short posicion;

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

}