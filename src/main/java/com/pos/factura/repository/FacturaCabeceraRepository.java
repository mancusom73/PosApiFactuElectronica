package com.pos.factura.repository;

import com.pos.factura.entity.factura.FacturaCabeceraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaCabeceraRepository extends JpaRepository<FacturaCabeceraEntity, Long> {

    /** Buscar por CAE — útil para verificar duplicados */
    Optional<FacturaCabeceraEntity> findByCae(String cae);

    /** Buscar por número de ticket del POS */
    Optional<FacturaCabeceraEntity> findByNumeroTicketPos(String numeroTicketPos);

    /** Facturas de un cliente (por documento) en un rango de fechas */
    @Query("SELECT f FROM FacturaCabeceraEntity f " +
           "WHERE f.clienteDocumentoNro = :docNro " +
           "AND f.fecha BETWEEN :desde AND :hasta " +
           "ORDER BY f.fecha DESC")
    List<FacturaCabeceraEntity> findByClienteYFecha(
            @Param("docNro") String docNro,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    /** Facturas por estado (PENDIENTE, APROBADO, RECHAZADO, ERROR) */
    List<FacturaCabeceraEntity> findByEstadoOrderByFechaAltaDesc(String estado);

    /** Buscar con detalle y medios de pago cargados en una sola query */
    @Query("SELECT f FROM FacturaCabeceraEntity f " +
           "LEFT JOIN FETCH f.detalle " +
           "LEFT JOIN FETCH f.mediosPago " +
           "WHERE f.id = :id")
    Optional<FacturaCabeceraEntity> findByIdConDetalle(@Param("id") Long id);
}
