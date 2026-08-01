package com.pos.factura.repository.dbtpviv;

import com.pos.factura.entity.dbtpviv.EvBPromo;
import com.pos.factura.entity.dbtpviv.EvBPromoId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvBPromoRepository
        extends JpaRepository<EvBPromo, EvBPromoId> {

    List<EvBPromo> findByCajaZAndIdEvento(
            Long cajaZ,
            Long idEvento
    );

    @Query("SELECT e.idGrupo FROM EvBPromo e " +
           "WHERE e.cajaZ = :cajaZ AND e.idEvento = :idEvento " +
           "AND e.codArt = :codArt AND e.bpromo = :bpromo")
    Optional<Short> findIdGrupoByCajaZAndIdEventoAndCodArtAndBpromo(
            @Param("cajaZ") Long cajaZ,
            @Param("idEvento") Long idEvento,
            @Param("codArt") Integer codArt,
            @Param("bpromo") Short bpromo
    );

    @Query("SELECT e.precioFijo FROM EvBPromo e " +
           "WHERE e.cajaZ = :cajaZ AND e.idEvento = :idEvento " +
           "AND e.idGrupo = :idGrupo AND e.bpromo = :bpromo")
    Optional<Double> findPrecioFijoByCajaZAndIdEventoAndIdGrupoAndBpromo(
            @Param("cajaZ") Long cajaZ,
            @Param("idEvento") Long idEvento,
            @Param("idGrupo") Short idGrupo,
            @Param("bpromo") Short bpromo
    );

    @Query("SELECT e.idGrupo FROM EvBPromo e " +
           "WHERE e.cajaZ = :cajaZ AND e.idEvento = :idEvento " +
           "AND e.bpromo = :bpromo")
    Optional<Short> findIdGrupoByCajaZAndIdEventoAndBpromo(
            @Param("cajaZ") Long cajaZ,
            @Param("idEvento") Long idEvento,
            @Param("bpromo") Short bpromo
    );
}
