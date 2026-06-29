package com.pos.factura.repository.dbtpviv;

import com.pos.factura.entity.dbtpviv.EvCont;
import com.pos.factura.entity.dbtpviv.EvContId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvContRepository
        extends JpaRepository<EvCont, EvContId> {

    List<EvCont> findByCajaZAndIdEvento(
            Long cajaZ,
            Long idEvento
    );
}