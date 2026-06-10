package com.pos.factura.repository.dbtpviv;

import com.pos.factura.entity.dbtpviv.TablaTemporalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * EJEMPLO de repositorio para una tabla de DBTPVIV.
 *
 * Spring Data JPA usará automáticamente el DataSource y EntityManager
 * de DBTPVIV para todos los repositorios en este paquete
 * (configurado en DbtpvivDataSourceConfig.java).
 *
 * Reemplazar con los repositorios reales de tus tablas de DBTPVIV.
 */
@Repository
public interface TablaTemporalRepository extends JpaRepository<TablaTemporalEntity, Long> {
    // Agregar aquí los métodos de consulta que necesites
}
