package co.sena.adso.porteria.repository;

import co.sena.adso.porteria.entity.Acceso;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccesoRepository extends JpaRepository<Acceso, Long> {

    Optional<Acceso> findFirstByReferenciaIdAndTipoReferenciaOrderByFechaDescIdDesc(Long referenciaId, String tipoReferencia);

    // Último movimiento de cada entidad de un tipo; se cuentan las que quedaron en Entrada
    @Query(value = """
            SELECT COUNT(*) FROM (
                SELECT DISTINCT ON (referencia_id) tipo FROM accesos
                WHERE tipo_referencia = :tipoReferencia
                ORDER BY referencia_id, fecha DESC, id DESC
            ) ultimos WHERE tipo = 'Entrada'
            """, nativeQuery = true)
    long contarAdentro(@Param("tipoReferencia") String tipoReferencia);

    @Query(value = """
            SELECT referencia_id FROM (
                SELECT DISTINCT ON (referencia_id) referencia_id, tipo FROM accesos
                WHERE tipo_referencia = :tipoReferencia AND referencia_id IN (:ids)
                ORDER BY referencia_id, fecha DESC, id DESC
            ) ultimos WHERE tipo = 'Entrada'
            """, nativeQuery = true)
    List<Long> quienesEstanAdentro(@Param("tipoReferencia") String tipoReferencia, @Param("ids") Collection<Long> ids);

    // Para el cierre de medianoche: [referencia_id, tipo_referencia, punto_id] de todo lo que sigue adentro
    @Query(value = """
            SELECT referencia_id, tipo_referencia, punto_id FROM (
                SELECT DISTINCT ON (referencia_id, tipo_referencia) referencia_id, tipo_referencia, punto_id, tipo
                FROM accesos
                ORDER BY referencia_id, tipo_referencia, fecha DESC, id DESC
            ) ultimos WHERE tipo = 'Entrada'
            """, nativeQuery = true)
    List<Object[]> pendientesDeSalida();

    @Query("""
            SELECT a FROM Acceso a
            WHERE a.fecha >= :desde AND a.fecha < :hasta
              AND (:tipoReferencia = '' OR a.tipoReferencia = :tipoReferencia)
              AND (:cargo = '' OR (a.tipoReferencia = 'Usuario'
                   AND a.referenciaId IN (SELECT u.id FROM Usuario u WHERE u.cargo = :cargo)))
              AND (:ficha = '' OR (a.tipoReferencia = 'Usuario'
                   AND a.referenciaId IN (SELECT u.id FROM Usuario u WHERE u.ficha = :ficha)))
            ORDER BY a.fecha DESC, a.id DESC
            """)
    Page<Acceso> buscarRecientes(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
                                 @Param("tipoReferencia") String tipoReferencia, @Param("cargo") String cargo,
                                 @Param("ficha") String ficha, Pageable pageable);

    // Entradas de personas por día y cargo, para la gráfica del panel
    @Query(value = """
            SELECT CAST(a.fecha AS DATE) AS dia, u.cargo, COUNT(*) AS total
            FROM accesos a JOIN usuarios u ON u.id = a.referencia_id
            WHERE a.tipo_referencia = 'Usuario' AND a.tipo = 'Entrada' AND a.fecha >= :desde
            GROUP BY CAST(a.fecha AS DATE), u.cargo
            """, nativeQuery = true)
    List<Object[]> entradasPorDiaYCargo(@Param("desde") LocalDateTime desde);

    @Query("""
            SELECT a, u FROM Acceso a, Usuario u
            WHERE a.tipoReferencia = 'Usuario' AND u.id = a.referenciaId
              AND a.fecha >= :desde AND a.fecha < :hasta
              AND (:cargo = '' OR u.cargo = :cargo)
              AND (:ficha = '' OR u.ficha = :ficha)
            ORDER BY a.fecha DESC
            """)
    List<Object[]> accesosDePersonas(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
                                     @Param("cargo") String cargo, @Param("ficha") String ficha);

    @Query("""
            SELECT a FROM Acceso a
            WHERE a.tipoReferencia = 'Usuario' AND a.referenciaId IN :ids
              AND a.fecha >= :desde AND a.fecha <= :hasta
            ORDER BY a.referenciaId, a.fecha, a.id
            """)
    List<Acceso> accesosDeUsuarios(@Param("ids") Collection<Long> ids, @Param("desde") LocalDateTime desde,
                                   @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT a.referenciaId, COUNT(a) FROM Acceso a
            WHERE a.tipoReferencia = 'Usuario' AND a.referenciaId IN :ids
              AND a.fecha >= :desde AND a.fecha <= :hasta
            GROUP BY a.referenciaId
            """)
    List<Object[]> contarPorUsuario(@Param("ids") Collection<Long> ids, @Param("desde") LocalDateTime desde,
                                    @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT a.referenciaId, MIN(a.fecha) FROM Acceso a
            WHERE a.tipoReferencia = 'Usuario' AND a.referenciaId IN :ids
            GROUP BY a.referenciaId
            """)
    List<Object[]> primerAccesoPorUsuario(@Param("ids") Collection<Long> ids);

    @Modifying
    @Query("DELETE FROM Acceso a WHERE a.tipoReferencia = 'Usuario' AND a.referenciaId = :usuarioId")
    void borrarDeUsuario(@Param("usuarioId") Long usuarioId);
}
