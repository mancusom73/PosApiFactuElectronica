package com.pos.factura.entity.dbtpviv;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@Table(name = "eventos")
@IdClass(EventoId.class)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Evento {

  /*  @EmbeddedId
    private EventoId id;*/

    @Id
    @Column(name = "CAJA_Z")
    private Long cajaZ;

    @Id
    @Column(name = "ID_EVENTO")
    private Long idEvento;

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

 /*   @Column(name = "SUC_COMPROBANTE")
    private Short sucComprobante;

    @Column(name = "NRO_COMPROBANTE")
    private Integer nroComprobante;
*/
    @Column(name = "TIPO_EVENTO")
    private Byte tipoEvento;

    @Column(name = "SUCURSAL")
    private Short sucursal;

    @Column(name = "CAJA")
    private Short caja;

    @Column(name = "TARJ_CLIE_AFINIDAD")
    private String tarjClieAfinidad;

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