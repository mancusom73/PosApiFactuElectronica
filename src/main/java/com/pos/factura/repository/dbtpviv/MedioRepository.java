package com.pos.factura.repository.dbtpviv;

import com.pos.factura.entity.dbtpviv.MedioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la tabla `medios` de DBTPVIV.
 */
@Repository
public interface MedioRepository extends JpaRepository<MedioEntity, Byte> {
}
