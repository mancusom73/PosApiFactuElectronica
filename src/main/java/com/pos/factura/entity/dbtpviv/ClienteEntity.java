package com.pos.factura.entity.dbtpviv;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entidad JPA que representa la tabla `clientes` de la base de datos del POS.
 *
 * Mapeo:
 *   clientes ←→ ClienteEntity
 */
@Getter
@Setter
@Entity
@Table(name = "clientes")
public class ClienteEntity {

    @Id
   // @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "COD_CLIENTE", nullable = false ,columnDefinition = "INT(11)")
    private Long codCliente;

    @Column(name = "COD_DOCUMENTO", nullable = false,columnDefinition = "CHAR(1)" ,length = 1)
    private String codDocumento;         // DNI, CUIT, CUIL, PASAPORTE

    @Column(name = "NRO_DOCUMENTO", nullable = false, columnDefinition = "INT(11)")
    private String nroDocumento;

    @Column(name = "NOMBRE", nullable = false, length = 200)
    private String nombre;

   /* @Column(name = "email", length = 150)
    private String email;*/

    @Column(name = "DOMICILIO", length = 40)
    private String domicilio;

    @Column(name = "PROVINCIA", length = 15)
    private String provincia;             // Código AFIP de provincia

    @Column(name = "COND_IVA", nullable = false, columnDefinition = "TINYINT(4)")
    private String condIva;          // CF, RI, MO

    /*@Column(name = "envia_por_mail", length = 1)
    private String enviaPorMail = "N";*/

 /*   @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;*/

    // Relación: un cliente puede tener muchos tickets
  /*  @OneToMany(mappedBy = "cliente", fetch = FetchType.LAZY)
    private List<TicketEntity> tickets;*/


    /*@PrePersist
    protected void onCreate() {
        fechaAlta = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaModificacion = LocalDateTime.now();
    }*/
}
