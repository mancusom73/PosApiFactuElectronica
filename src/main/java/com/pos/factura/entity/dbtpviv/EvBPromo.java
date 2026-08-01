package com.pos.factura.entity.dbtpviv;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "ev_bpromo")
@IdClass(EvBPromoId.class)
public class EvBPromo {

    @Id
    @Column(name = "CAJA_Z")
    private Long cajaZ;

    @Id
    @Column(name = "ID_EVENTO")
    private Long idEvento;

    @Column(name = "STATUS")
    private Byte status;

    @Column(name = "ID_PROMO")
    private Short idPromo;

    @Column(name = "COD_PROMO")
    private Short codPromo;

    @Column(name = "PORC_DESC")
    private Float porcDesc;

    @Column(name = "COD_ART_AGR")
    private Integer codArtAgr;

    @Column(name = "COD_BARRA_AGR")
    private String codBarraAgr;

    @Column(name = "CANTIDAD")
    private Float cantidad;

    @Column(name = "IMP_SIN_IVA")
    private Double impSinIva;

    @Column(name = "EXENTO")
    private Double exento;

    @Column(name = "IVA1")
    private Double iva1;

    @Column(name = "IVA2")
    private Double iva2;

    @Column(name = "IHA")
    private Double iha;

    @Column(name = "ILA")
    private Double ila;

    @Column(name = "IMP_INT")
    private Double impInt;

    @Column(name = "COD_ART")
    private Integer codArt;

    @Column(name = "COD_BARRA")
    private String codBarra;

    @Column(name = "CUPONES")
    private Short cupones;

    @Column(name = "BONOS")
    private Short bonos;

    @Column(name = "TIPO")
    private Short tipo;

    @Column(name = "TOT_PUNTOS")
    private Float totPuntos;

    @Column(name = "COD_ART_AGR_EXT")
    private String codArtAgrExt;

    @Column(name = "COD_ART_EXT")
    private String codArtExt;

    @Column(name = "T_PROMO")
    private Short tPromo;

    @Column(name = "B_PROMO")
    private Short bpromo;

    @Column(name = "ID_GRUPO")
    private Short idGrupo;

    @Column(name = "SUCURSAL")
    private Short sucursal;

    @Column(name = "FECHA_TICKET")
    private Short fechaTicket;

    @Column(name = "NRO_Z")
    private Integer nroZ;

    @Column(name = "PRECIO_FIJO")
    private Double precioFijo;

    @Column(name = "POSICION")
    private Short posicion;

    @Column(name = "CANT_MINIMA")
    private Short cantMinima;

    @Column(name = "NRO_CAJA_DGI")
    private Short nroCajaDgi;

    @Column(name = "PUEDE_REVERTIR")
    private String puedeRevertir;

    @Column(name = "COD_PREMIO")
    private Integer codPremio;

    @Column(name = "MONTO_IMPUESTO1")
    private Double montoImpuesto1;

    @Column(name = "MONTO_IMPUESTO2")
    private Double montoImpuesto2;

    @Column(name = "MONTO_IMPUESTO3")
    private Double montoImpuesto3;

    @Column(name = "MONTO_IMPUESTO4")
    private Double montoImpuesto4;

    @Column(name = "COD_MEDIO")
    private Byte codMedio;

    @Column(name = "COD_SUBMEDIO")
    private Byte codSubmedio;

    @Column(name = "MODO_PLAN_PAGO")
    private Short modoPlanPago;

    @Column(name = "NRO_TARJETA_STRING")
    private String nroTarjetaString;

    @Column(name = "APLICADO")
    private String aplicado;

    @Column(name = "COD_EXT")
    private String codExt;

    @Column(name = "FIN")
    private Byte fin;
}
