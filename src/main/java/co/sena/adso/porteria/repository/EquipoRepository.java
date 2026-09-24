package co.sena.adso.porteria.repository;

import co.sena.adso.porteria.entity.Equipo;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    List<Equipo> findByUsuarioIdOrderById(Long usuarioId);

    List<Equipo> findByUsuarioIdAndIdIn(Long usuarioId, Collection<Long> ids);

    long countByUsuarioId(Long usuarioId);

    boolean existsBySerial(String serial);

    @Modifying
    @Query("UPDATE Equipo e SET e.estado = 'Afuera' WHERE e.estado = 'Adentro'")
    int sacarTodos();
}
