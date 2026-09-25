package co.sena.adso.porteria.repository;

import co.sena.adso.porteria.entity.Mensaje;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    List<Mensaje> findByUsuarioIdOrderByFechaAscIdAsc(Long usuarioId);

    // Lo que le escribieron a la persona (también lo del autor ya borrado)
    @Modifying
    @Query("""
            UPDATE Mensaje m SET m.leido = true
            WHERE m.usuarioId = :usuarioId AND m.leido = false AND (m.autorId IS NULL OR m.autorId <> :usuarioId)
            """)
    int marcarLeidosPorLaPersona(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("UPDATE Mensaje m SET m.leido = true WHERE m.usuarioId = :usuarioId AND m.autorId = :usuarioId AND m.leido = false")
    int marcarLeidosPorElAsesor(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT COUNT(m) FROM Mensaje m
            WHERE m.usuarioId = :usuarioId AND m.leido = false AND (m.autorId IS NULL OR m.autorId <> :usuarioId)
            """)
    long sinLeerParaLaPersona(@Param("usuarioId") Long usuarioId);

    @Query("SELECT COUNT(DISTINCT m.usuarioId) FROM Mensaje m WHERE m.autorId = m.usuarioId AND m.leido = false")
    long hilosConRespuestaPendiente();

    // [usuario_id, última fecha, mensajes de la persona sin leer] por hilo
    @Query("""
            SELECT m.usuarioId, MAX(m.fecha),
                   SUM(CASE WHEN m.autorId = m.usuarioId AND m.leido = false THEN 1 ELSE 0 END)
            FROM Mensaje m GROUP BY m.usuarioId
            """)
    List<Object[]> resumenDeHilos();

    @Query("""
            SELECT m FROM Mensaje m
            WHERE m.id IN (SELECT MAX(u.id) FROM Mensaje u WHERE u.usuarioId IN :ids GROUP BY u.usuarioId)
            """)
    List<Mensaje> ultimosDe(@Param("ids") List<Long> ids);
}
