package com.pos.factura.entity.dbtpviv;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cajas")
public class CajaEntity {

    @Id
    @Column(name = "CAJA")
    private Short caja;

    @Column(name = "XAPIKEY")
    private String xapikey;
}
