package com.pos.factura.repository;

import com.pos.factura.entity.posfe.TicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Queries con múltiples @OneToMany (items, pagos) se separan para evitar
 * MultipleBagFetchException de Hibernate. Llamar en secuencia desde el servicio:
 *   1. findConItemsById / findConItemsByNumeroTicket  → carga items + producto
 *   2. findConPagosById / findConPagosByNumeroTicket  → carga pagos
 * Hibernate unifica ambos resultados en la misma instancia cacheada.
 */
@Repository
public interface TicketRepository extends JpaRepository<TicketEntity, Long> {

    /**
     * Busca un ticket por número correlativo del POS.
     * Carga cliente + items + pagos en una sola query (evita N+1).
     * Nota: text blocks (""") son Java 15+, se usan String normales para Java 11.
     */
    @Query("SELECT t FROM TicketEntity t " +
           "LEFT JOIN FETCH t.items i " +
           "LEFT JOIN FETCH i.producto " +
           "WHERE t.numeroTicket = :numeroTicket")
    Optional<TicketEntity> findConItemsByNumeroTicket(
            @Param("numeroTicket") String numeroTicket);

    @Query("SELECT t FROM TicketEntity t " +
           "LEFT JOIN FETCH t.pagos " +
           "WHERE t.numeroTicket = :numeroTicket")
    Optional<TicketEntity> findConPagosByNumeroTicket(
            @Param("numeroTicket") String numeroTicket);

    // ── Por ID ────────────────────────────────────────────────────────────────

    /**
     * Busca por ID con todas las relaciones cargadas.
     */
    @Query("SELECT t FROM TicketEntity t " +
           "LEFT JOIN FETCH t.items i " +
           "LEFT JOIN FETCH i.producto " +
           "WHERE t.id = :id")
    Optional<TicketEntity> findConDetalleById(@Param("id") Long id);

    @Query("SELECT t FROM TicketEntity t " +
           "LEFT JOIN FETCH t.pagos " +
           "WHERE t.id = :id")
    Optional<TicketEntity> findConPagosById(@Param("id") Long id);

    // ── Pendientes ────────────────────────────────────────────────────────────

    @Query("SELECT t FROM TicketEntity t " +
           "LEFT JOIN FETCH t.items i " +
           "LEFT JOIN FETCH i.producto " +
           "WHERE t.estado = 'PENDIENTE' " +
           "ORDER BY t.fecha ASC, t.fechaAlta ASC")
    List<TicketEntity> findPendientesConItems();

    @Query("SELECT t FROM TicketEntity t " +
           "LEFT JOIN FETCH t.pagos " +
           "WHERE t.estado = 'PENDIENTE' " +
           "ORDER BY t.fecha ASC, t.fechaAlta ASC")
    List<TicketEntity> findPendientesConPagos();

    // ── Otros ─────────────────────────────────────────────────────────────────

    // Filtra por codCliente (Long) — sin JOIN JPA cross-database
    List<TicketEntity> findByCodClienteAndFechaBetweenOrderByFechaDesc(
            Long codCliente, LocalDate desde, LocalDate hasta);

    /**
     * Tickets con error para auditoría.
     */
    List<TicketEntity> findByEstadoOrderByFechaAltaDesc(TicketEntity.EstadoTicket estado);

    /**
     * Verifica si el número de ticket ya existe (para evitar duplicados).
     */
    boolean existsByNumeroTicket(String numeroTicket);
}
