package com.pos.factura.repository.dbtpviv;

import com.pos.factura.entity.dbtpviv.TarjetaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la tabla `tarjetas` de DBTPVIV.
 */
@Repository
public interface TarjetaRepository extends JpaRepository<TarjetaEntity, Short> {
}
