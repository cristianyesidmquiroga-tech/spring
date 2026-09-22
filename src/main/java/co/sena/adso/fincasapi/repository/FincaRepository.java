package co.sena.adso.fincasapi.repository;

import co.sena.adso.fincasapi.entity.Finca;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FincaRepository extends JpaRepository<Finca, Long>, JpaSpecificationExecutor<Finca> {

    List<Finca> findByMunicipioIgnoreCase(String municipio);

    @Query(value = "SELECT * FROM fincas WHERE hectareas >= :minimo ORDER BY hectareas DESC", nativeQuery = true)
    List<Finca> buscarConAreaMinima(@Param("minimo") Double minimo);
}
