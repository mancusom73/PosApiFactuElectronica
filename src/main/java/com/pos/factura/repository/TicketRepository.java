package com.pos.factura.repository;

import com.pos.factura.entity.TicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<TicketEntity, Long> {

    /**
     * Busca un ticket por número correlativo del POS.
     * Carga cliente + items + pagos en una sola query (evita N+1).
     * Nota: text blocks (""") son Java 15+, se usan String normales para Java 11.
     */
    @Query("SELECT t FROM TicketEntity t " +
           "LEFT JOIN FETCH t.cliente " +
           "LEFT JOIN FETCH t.items i " +
           "LEFT JOIN FETCH i.producto " +
           "LEFT JOIN FETCH t.pagos " +
           "WHERE t.numeroTicket = :numeroTicket")
    Optional<TicketEntity> findByNumeroTicketConDetalle(@Param("numeroTicket") String numeroTicket);

    /**
     * Busca por ID con todas las relaciones cargadas.
     */
    @Query("SELECT t FROM TicketEntity t " +
           "LEFT JOIN FETCH t.cliente " +
           "LEFT JOIN FETCH t.items i " +
           "LEFT JOIN FETCH i.producto " +
           "LEFT JOIN FETCH t.pagos " +
           "WHERE t.id = :id")
    Optional<TicketEntity> findByIdConDetalle(@Param("id") Long id);

    /**
     * Tickets pendientes de facturación (para reintento o procesamiento batch).
     */
    @Query("SELECT t FROM TicketEntity t " +
           "LEFT JOIN FETCH t.cliente " +
           "LEFT JOIN FETCH t.items i " +
           "LEFT JOIN FETCH i.producto " +
           "LEFT JOIN FETCH t.pagos " +
           "WHERE t.estado = 'PENDIENTE' " +
           "ORDER BY t.fecha ASC, t.fechaAlta ASC")
    List<TicketEntity> findPendientes();

    /**
     * Tickets de un cliente en un rango de fechas.
     */
    @Query("SELECT t FROM TicketEntity t " +
           "WHERE t.cliente.id = :clienteId " +
           "AND t.fecha BETWEEN :desde AND :hasta " +
           "ORDER BY t.fecha DESC")
    List<TicketEntity> findByClienteYFecha(
            @Param("clienteId") Long clienteId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    /**
     * Tickets con error para auditoría.
     */
    List<TicketEntity> findByEstadoOrderByFechaAltaDesc(TicketEntity.EstadoTicket estado);

    /**
     * Verifica si el número de ticket ya existe (para evitar duplicados).
     */
    boolean existsByNumeroTicket(String numeroTicket);
}
