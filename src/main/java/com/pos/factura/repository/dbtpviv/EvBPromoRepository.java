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

    @Query("SELECT SUM(p.precioFijo) FROM EvBPromo p " +
           "WHERE p.cajaZ = :cajaZ AND p.idEvento = :idEvento AND p.bpromo = 101 " +
           "AND p.idGrupo IN (SELECT a.idGrupo FROM EvBPromo a " +
           "WHERE a.cajaZ = :cajaZ AND a.idEvento = :idEvento AND a.codArt = :codArt AND a.bpromo = 1)")
    Double sumPrecioFijoDescuentoByCajaZAndIdEventoAndCodArt(
            @Param("cajaZ") Long cajaZ,
            @Param("idEvento") Long idEvento,
            @Param("codArt") Integer codArt
    );

    @Query("SELECT e.idGrupo FROM EvBPromo e " +
           "WHERE e.cajaZ = :cajaZ AND e.idEvento = :idEvento " +
           "AND e.bpromo = :bpromo")
    Optional<Short> findIdGrupoByCajaZAndIdEventoAndBpromo(
            @Param("cajaZ") Long cajaZ,
            @Param("idEvento") Long idEvento,
            @Param("bpromo") Short bpromo
    );

    @Query("SELECT e.idGrupo FROM EvBPromo e " +
           "WHERE e.cajaZ = :cajaZ AND e.idEvento = :idEvento " +
           "AND e.bpromo = :bpromo AND e.precioFijo = 0")
    Optional<Short> findIdGrupoByCajaZAndIdEventoAndBpromoAndPrecioFijoCero(
            @Param("cajaZ") Long cajaZ,
            @Param("idEvento") Long idEvento,
            @Param("bpromo") Short bpromo
    );

    @Query("SELECT COUNT(e) FROM EvBPromo e " +
           "WHERE e.cajaZ = :cajaZ AND e.idEvento = :idEvento " +
           "AND e.idGrupo = :idGrupo AND e.bpromo = :bpromo AND e.precioFijo = 0")
    long countByCajaZAndIdEventoAndIdGrupoAndBpromoAndPrecioFijoCero(
            @Param("cajaZ") Long cajaZ,
            @Param("idEvento") Long idEvento,
            @Param("idGrupo") Short idGrupo,
            @Param("bpromo") Short bpromo
    );

    @Query("SELECT e.precioFijo FROM EvBPromo e " +
           "WHERE e.cajaZ = :cajaZ AND e.idEvento = :idEvento " +
           "AND e.idGrupo = :idGrupo AND e.precioFijo > 0")
    Optional<Double> findPrecioFijoByCajaZAndIdEventoAndIdGrupoAndPrecioFijoMayorCero(
            @Param("cajaZ") Long cajaZ,
            @Param("idEvento") Long idEvento,
            @Param("idGrupo") Short idGrupo
    );

    @Query("SELECT COUNT(e) > 0 FROM EvBPromo e " +
           "WHERE e.cajaZ = :cajaZ AND e.idEvento = :idEvento " +
           "AND e.codArt = :codArt AND e.bpromo = :bpromo AND e.precioFijo = 0")
    boolean existsByCajaZAndIdEventoAndCodArtAndBpromoAndPrecioFijoCero(
            @Param("cajaZ") Long cajaZ,
            @Param("idEvento") Long idEvento,
            @Param("codArt") Integer codArt,
            @Param("bpromo") Short bpromo
    );

    @Query("SELECT DISTINCT e.codArt FROM EvBPromo e " +
           "WHERE e.cajaZ = :cajaZ AND e.idEvento = :idEvento AND e.codArt > 0")
    List<Integer> findCodArtByCajaZAndIdEvento(
            @Param("cajaZ") Long cajaZ,
            @Param("idEvento") Long idEvento
    );

    @Query("SELECT COALESCE(SUM(e.precioFijo), 0) FROM EvBPromo e " +
           "WHERE e.cajaZ = :cajaZ AND e.idEvento = :idEvento")
    Double sumPrecioFijoByCajaZAndIdEvento(
            @Param("cajaZ") Long cajaZ,
            @Param("idEvento") Long idEvento
    );
}
