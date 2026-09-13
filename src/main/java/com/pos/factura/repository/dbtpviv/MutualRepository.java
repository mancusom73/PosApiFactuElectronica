package com.pos.factura.repository.dbtpviv;

import com.pos.factura.entity.dbtpviv.MutualEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la tabla `mutuales` de DBTPVIV.
 */
@Repository
public interface MutualRepository extends JpaRepository<MutualEntity, Byte> {
}
