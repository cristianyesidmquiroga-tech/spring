package co.sena.adso.porteria.repository;

import co.sena.adso.porteria.entity.AsistenciaClase;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AsistenciaClaseRepository extends JpaRepository<AsistenciaClase, Long> {

    @Query("""
            SELECT a FROM AsistenciaClase a JOIN FETCH a.aprendiz LEFT JOIN FETCH a.instructor
            WHERE a.ficha = :ficha ORDER BY a.fecha DESC, a.id DESC
            """)
    List<AsistenciaClase> historialDeFicha(@Param("ficha") String ficha, Pageable pagina);

    @Query("""
            SELECT a FROM AsistenciaClase a LEFT JOIN FETCH a.instructor
            WHERE a.ficha IN :fichas AND a.fecha >= :desde AND a.fecha < :hasta ORDER BY a.id
            """)
    List<AsistenciaClase> deFichasEnRango(@Param("fichas") Collection<String> fichas,
                                          @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Modifying
    @Query("DELETE FROM AsistenciaClase a WHERE a.ficha = :ficha AND a.fecha >= :desde AND a.fecha < :hasta")
    void borrarDeFichaEnRango(@Param("ficha") String ficha, @Param("desde") LocalDateTime desde,
                              @Param("hasta") LocalDateTime hasta);

    // [aprendiz_id, faltas] desde una fecha
    @Query("""
            SELECT a.aprendiz.id, COUNT(a) FROM AsistenciaClase a
            WHERE a.presente = false AND a.fecha >= :desde GROUP BY a.aprendiz.id
            """)
    List<Object[]> faltasDesde(@Param("desde") LocalDateTime desde);
}
