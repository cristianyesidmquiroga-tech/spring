package co.sena.adso.porteria.repository;

import co.sena.adso.porteria.entity.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    List<Usuario> findByFotoEstadoOrderByFotoFechaSubidaAsc(String fotoEstado);

    Optional<Usuario> findByDocumento(String documento);

    long countByCargo(String cargo);

    long countByRolNombre(String rolNombre);

    List<Usuario> findByCargoOrderByNombre(String cargo);

    @Query("SELECT DISTINCT u.ficha FROM Usuario u WHERE u.ficha IS NOT NULL AND u.ficha <> '' ORDER BY u.ficha")
    List<String> fichasEnUso();

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
