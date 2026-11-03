package com.pos.factura.repository.dbtpviv;

import com.pos.factura.entity.dbtpviv.FactuMemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la tabla `factu_mem` de DBTPVIV.
 */
@Repository
public interface FactuMemRepository extends JpaRepository<FactuMemEntity, Integer> {

    Optional<FactuMemEntity> findByDescripcion(String descripcion);
}
