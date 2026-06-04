package com.pos.factura.repository.dbtpviv;

import com.pos.factura.entity.dbtpviv.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la tabla clientes de DBTPVIV.
 * Usa automaticamente el DataSource y TransactionManager de DBTPVIV.
 */
@Repository
public interface ClienteRepository extends JpaRepository<ClienteEntity, Long> {

    Optional<ClienteEntity> findByDocumentoNro(String documentoNro);

    boolean existsByDocumentoNro(String documentoNro);
}
