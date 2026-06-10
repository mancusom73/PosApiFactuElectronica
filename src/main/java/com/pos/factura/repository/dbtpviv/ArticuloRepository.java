package com.pos.factura.repository.dbtpviv;

import com.pos.factura.entity.dbtpviv.ArticuloEntity;
import com.pos.factura.entity.dbtpviv.ArticuloId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la tabla articulo de DBTPVIV.
 * Usa automáticamente el DataSource y TransactionManager de DBTPVIV.
 */
@Repository
public interface ArticuloRepository extends JpaRepository<ArticuloEntity, ArticuloId> {

    // ── Por clave primaria compuesta ──────────────────────────────────────────

    /**
     * Busca por los dos campos de la PK juntos.
     * Equivale a: WHERE COD_INTERNO = ? AND CODIGO_BARRA = ?
     */
    Optional<ArticuloEntity> findByCodInternoAndCodigoBarra(
            Integer codInterno, String codigoBarra);

    /**
     * Busca todos los artículos de un mismo COD_INTERNO.
     * Un producto puede tener múltiples códigos de barra.
     */
    List<ArticuloEntity> findByCodInterno(Integer codInterno);

    /**
     * Busca por CODIGO_BARRA.
     * Puede devolver más de un resultado si el mismo barcode
     * está asociado a distintos COD_INTERNO.
     */
    List<ArticuloEntity> findByCodigoBarra(String codigoBarra);

    /**
     * Busca el primer artículo activo por código de barra.
     * Útil para lectura en el punto de venta (scanner).
     */
    @Query("SELECT a FROM ArticuloEntity a " +
           "WHERE a.codigoBarra = :codigoBarra " +
           "AND a.habilitado = 'S'")
    Optional<ArticuloEntity> findActivoByCodigoBarra(@Param("codigoBarra") String codigoBarra);

    /**
     * Busca el primer artículo activo por COD_INTERNO.
     */
    @Query("SELECT a FROM ArticuloEntity a " +
           "WHERE a.codInterno = :codInterno " +
           "AND a.habilitado = 'S'")
    Optional<ArticuloEntity> findActivoByCodInterno(@Param("codInterno") Integer codInterno);

    // ── Búsquedas adicionales ─────────────────────────────────────────────────

    /**
     * Busca por código interno alfanumérico.
     */
    Optional<ArticuloEntity> findByCodInternoAlfa(String codInternoAlfa);

    /**
     * Busca artículos por nombre (búsqueda parcial, case-insensitive).
     * Útil para buscador del POS.
     */
    @Query("SELECT a FROM ArticuloEntity a " +
           "WHERE UPPER(a.nombre) LIKE UPPER(CONCAT('%', :nombre, '%')) " +
           "AND a.habilitado = 'S' " +
           "ORDER BY a.nombre ASC")
    List<ArticuloEntity> findByNombreContaining(@Param("nombre") String nombre);

    /**
     * Todos los artículos habilitados ordenados por nombre.
     */
    List<ArticuloEntity> findByHabilitadoOrderByNombreAsc(String habilitado);

    /**
     * Artículos por clasificación.
     */
    List<ArticuloEntity> findByCodClasificacionAndHabilitado(
            String codClasificacion, String habilitado);

    /**
     * Artículos por marca.
     */
    List<ArticuloEntity> findByMarcaAndHabilitadoOrderByNombreAsc(
            String marca, String habilitado);
}
