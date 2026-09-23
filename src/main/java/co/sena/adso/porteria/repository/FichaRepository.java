package co.sena.adso.porteria.repository;

import co.sena.adso.porteria.entity.Ficha;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FichaRepository extends JpaRepository<Ficha, Long> {

    Optional<Ficha> findByNumero(String numero);

    // Las activas y, aunque esté archivada, la que ya tiene la persona
    @Query("SELECT f FROM Ficha f WHERE f.activa = true OR f.id = :actual ORDER BY f.numero")
    List<Ficha> listarParaSelector(@Param("actual") Long fichaActualId);
}
