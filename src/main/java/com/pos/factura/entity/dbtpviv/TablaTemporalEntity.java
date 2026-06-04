package com.pos.factura.entity.dbtpviv;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * EJEMPLO de entidad para una tabla de la base de datos DBTPVIV.
 *
 * Reemplazar este archivo con las entidades que correspondan a tus
 * tablas reales de DBTPVIV.
 *
 * Reglas importantes:
 *  - Todas las entidades de DBTPVIV deben estar en este paquete:
 *      com.pos.factura.entity.dbtpviv
 *  - Sus repositorios deben estar en:
 *      com.pos.factura.repository.dbtpviv
 *  - Los métodos @Transactional que usen estas entidades deben
 *    especificar el transaction manager:
 *      @Transactional("dbtpvivTransactionManager")
 *
 * Ejemplo de uso en un servicio:
 * ─────────────────────────────
 *   @Service
 *   public class MiServicioDbtpviv {
 *
 *       private final TablaTemporalRepository repo;
 *
 *       public MiServicioDbtpviv(TablaTemporalRepository repo) {
 *           this.repo = repo;
 *       }
 *
 *       @Transactional("dbtpvivTransactionManager")
 *       public List<TablaTemporalEntity> listar() {
 *           return repo.findAll();
 *       }
 *   }
 */
@Getter
@Setter
@Entity
@Table(name = "tabla_temporal_ejemplo")
public class TablaTemporalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    // Agregar aquí los campos que correspondan a tus tablas reales de DBTPVIV
}
