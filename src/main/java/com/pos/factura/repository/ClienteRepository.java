package com.pos.factura.repository;

import com.pos.factura.entity.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<ClienteEntity, Long> {

    Optional<ClienteEntity> findByDocumentoNro(String documentoNro);

    boolean existsByDocumentoNro(String documentoNro);
}
