package com.pos.factura.repository.dbtpviv;

import com.pos.factura.entity.dbtpviv.TCompraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la tabla `t_compra` de DBTPVIV.
 */
@Repository
public interface TCompraRepository extends JpaRepository<TCompraEntity, Byte> {
}
