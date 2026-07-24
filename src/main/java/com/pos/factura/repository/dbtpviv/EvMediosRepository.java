package com.pos.factura.repository.dbtpviv;

import com.pos.factura.entity.dbtpviv.EvMedios;
import com.pos.factura.entity.dbtpviv.EvMediosId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la tabla ev_medios de DBTPVIV.
 * Usa automáticamente el DataSource y TransactionManager de DBTPVIV.
 */
@Repository
public interface EvMediosRepository extends JpaRepository<EvMedios, EvMediosId> {

    // ── Por clave primaria compuesta ──────────────────────────────────────────

    /**
     * Busca por los tres campos de la PK.
     * WHERE CAJA_Z = ? AND ID_EVENTO = ? AND POSICION = ?
     */
    Optional<EvMedios> findByIdCajaZAndIdIdEventoAndIdPosicion(
            Integer cajaZ, Integer idEvento, Byte posicion);

    // ── Por evento (cajaZ + idEvento) ─────────────────────────────────────────

    /**
     * Todos los medios de pago de un evento, ordenados por posición.
     * Es la consulta principal — un evento puede tener varios medios de pago.
     */
    @Query("SELECT m FROM EvMedios m " +
           "WHERE m.id.cajaZ = :cajaZ " +
           "AND m.id.idEvento = :idEvento " +
           "ORDER BY m.id.posicion ASC")
    List<EvMedios> findByEvento(
            @Param("cajaZ") Long cajaZ,
            @Param("idEvento") Long idEvento);

    /**
     * Todos los medios de pago de una caja Z completa.
     */
    @Query("SELECT m FROM EvMedios m " +
           "WHERE m.id.cajaZ = :cajaZ " +
           "ORDER BY m.id.idEvento ASC, m.id.posicion ASC")
    List<EvMedios> findByCajaZ(@Param("cajaZ") Integer cajaZ);

    // ── Por modo de pago ──────────────────────────────────────────────────────

    /**
     * Medios de pago de un evento filtrados por modo de pago.
     * Útil para sumar solo efectivo, solo tarjeta, etc.
     */
    @Query("SELECT m FROM EvMedios m " +
           "WHERE m.id.cajaZ = :cajaZ " +
           "AND m.id.idEvento = :idEvento " +
           "AND m.modoPago = :modoPago " +
           "ORDER BY m.id.posicion ASC")
    List<EvMedios> findByEventoAndModoPago(
            @Param("cajaZ") Integer cajaZ,
            @Param("idEvento") Integer idEvento,
            @Param("modoPago") Short modoPago);

    /**
     * Suma el importe total de los medios de pago de un evento.
     */
    @Query("SELECT SUM(m.importe) FROM EvMedios m " +
           "WHERE m.id.cajaZ = :cajaZ " +
           "AND m.id.idEvento = :idEvento")
    Double sumImporteByEvento(
            @Param("cajaZ") Integer cajaZ,
            @Param("idEvento") Integer idEvento);
}
