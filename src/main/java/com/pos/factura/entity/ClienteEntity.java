package com.pos.factura.entity;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "documento_tipo", nullable = false, length = 20)
    private String documentoTipo;         // DNI, CUIT, CUIL, PASAPORTE

    @Column(name = "documento_nro", nullable = false, length = 20)
    private String documentoNro;

    @Column(name = "razon_social", nullable = false, length = 200)
    private String razonSocial;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "domicilio", length = 250)
    private String domicilio;

    @Column(name = "provincia", length = 5)
    private String provincia;             // Código AFIP de provincia

    @Column(name = "condicion_iva", nullable = false, length = 5)
    private String condicionIva;          // CF, RI, MO

    @Column(name = "envia_por_mail", length = 1)
    private String enviaPorMail = "N";

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    // Relación: un cliente puede tener muchos tickets
    @OneToMany(mappedBy = "cliente", fetch = FetchType.LAZY)
    private List<TicketEntity> tickets;

    @PrePersist
    protected void onCreate() {
        fechaAlta = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaModificacion = LocalDateTime.now();
    }
}
