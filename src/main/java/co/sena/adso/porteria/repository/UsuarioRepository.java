package co.sena.adso.porteria.repository;

import co.sena.adso.porteria.entity.Usuario;
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

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("SELECT u FROM Usuario u WHERE lower(u.correo) = lower(:identificador) OR u.documento = :identificador")
    Optional<Usuario> buscarPorCorreoODocumento(@Param("identificador") String identificador);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);

    boolean existsByDocumento(String documento);

    boolean existsByDocumentoAndIdNot(String documento, Long id);

    @Query("""
            SELECT u FROM Usuario u
            WHERE (:texto = '' OR lower(u.nombre) LIKE concat('%', :texto, '%')
                   OR lower(u.correo) LIKE concat('%', :texto, '%')
                   OR u.documento LIKE concat('%', :texto, '%'))
              AND (:rolId = 0 OR u.rol.id = :rolId)
              AND (:cargo = '' OR u.cargo = :cargo)
            """)
    Page<Usuario> buscar(@Param("texto") String texto, @Param("rolId") Long rolId,
                         @Param("cargo") String cargo, Pageable pageable);

    Page<Usuario> findByFotoEstado(String fotoEstado, Pageable pageable);

    Page<Usuario> findByFotoEstadoNot(String fotoEstado, Pageable pageable);

    long countByFotoEstado(String fotoEstado);

    Optional<Usuario> findByDocumento(String documento);

    long countByCargo(String cargo);

    long countByRolNombre(String rolNombre);

    List<Usuario> findByCargoInOrderByNombre(Collection<String> cargos);

    @Query("SELECT DISTINCT u.ficha FROM Usuario u WHERE u.ficha IS NOT NULL AND u.ficha <> '' ORDER BY u.ficha")
    List<String> fichasEnUso();

    // [usuario, primera entrada] de los aprendices de una ficha que cruzaron portería en el rango
    @Query("""
            SELECT u, MIN(a.fecha) FROM Usuario u, Acceso a
            WHERE a.referenciaId = u.id AND a.tipoReferencia = 'Usuario' AND a.tipo = 'Entrada'
              AND a.fecha >= :desde AND a.fecha < :hasta AND u.ficha = :ficha AND u.cargo = 'Aprendiz'
            GROUP BY u ORDER BY MIN(a.fecha), u.nombre
            """)
    List<Object[]> llegadasDeFicha(@Param("ficha") String ficha, @Param("desde") LocalDateTime desde,
                                   @Param("hasta") LocalDateTime hasta);

    // [ficha, aprendices, programa] de las fichas con aprendices que entraron en el rango
    @Query("""
            SELECT u.ficha, COUNT(DISTINCT u.id), MAX(u.programa) FROM Usuario u, Acceso a
            WHERE a.referenciaId = u.id AND a.tipoReferencia = 'Usuario' AND a.tipo = 'Entrada'
              AND a.fecha >= :desde AND a.fecha < :hasta AND u.cargo = 'Aprendiz'
              AND u.ficha IS NOT NULL AND u.ficha <> ''
            GROUP BY u.ficha ORDER BY u.ficha
            """)
    List<Object[]> fichasConAprendicesAdentro(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    List<Usuario> findByCorreoVerificadoTrueOrderByCargoAscNombreAsc();

    @Query("SELECT u.fichaRef.id, COUNT(u) FROM Usuario u WHERE u.fichaRef IS NOT NULL GROUP BY u.fichaRef.id")
    List<Object[]> contarPorFicha();

    // La columna de texto sigue a la ficha: reportes y asistencia filtran por ella
    @Modifying
    @Query("UPDATE Usuario u SET u.ficha = :numero, u.programa = :programa WHERE u.fichaRef.id = :fichaId")
    void sincronizarFicha(@Param("fichaId") Long fichaId, @Param("numero") String numero,
                          @Param("programa") String programa);

    // El patrón llega con % y _ ya escapados: buscar "%" no debe traer a todo el centro
    @Query("""
            SELECT u FROM Usuario u
            WHERE (:filtrarIds = false OR u.id IN :ids)
              AND (:ficha = '' OR u.ficha = :ficha)
              AND (:cargo = '' OR u.cargo = :cargo)
              AND (:patron = '' OR lower(u.nombre) LIKE :patron ESCAPE '\\' OR u.documento LIKE :patron ESCAPE '\\')
            ORDER BY u.nombre
            """)
    List<Usuario> buscarParaHistorial(@Param("filtrarIds") boolean filtrarIds, @Param("ids") List<Long> ids,
                                      @Param("ficha") String ficha, @Param("cargo") String cargo,
                                      @Param("patron") String patron, Pageable pageable);
}
