package co.sena.adso.porteria.repository;

import co.sena.adso.porteria.entity.Visitante;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface VisitanteRepository extends JpaRepository<Visitante, Long> {

    Optional<Visitante> findByDocumento(String documento);

    List<Visitante> findTop200ByOrderByFechaCreacionDesc();

    @Modifying
    @Query("UPDATE Visitante v SET v.activo = false WHERE v.activo = true")
    int desactivarTodos();
}
