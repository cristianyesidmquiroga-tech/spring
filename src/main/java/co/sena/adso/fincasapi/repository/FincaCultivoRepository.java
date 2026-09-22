package co.sena.adso.fincasapi.repository;

import co.sena.adso.fincasapi.entity.FincaCultivo;
import co.sena.adso.fincasapi.enums.EstadoSiembra;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FincaCultivoRepository extends JpaRepository<FincaCultivo, Long> {

    // JOIN FETCH trae finca y cultivo en la misma consulta y evita el N+1 al armar el DTO
    @Query("SELECT fc FROM FincaCultivo fc JOIN FETCH fc.finca JOIN FETCH fc.cultivo ORDER BY fc.id")
    List<FincaCultivo> listarConDetalle();

    @Query("SELECT fc FROM FincaCultivo fc JOIN FETCH fc.finca JOIN FETCH fc.cultivo WHERE fc.finca.id = :fincaId ORDER BY fc.id")
    List<FincaCultivo> listarPorFinca(@Param("fincaId") Long fincaId);

    @Query("SELECT COALESCE(SUM(fc.areaSembradaHa), 0) FROM FincaCultivo fc "
            + "WHERE fc.finca.id = :fincaId AND fc.estado = :estado AND fc.id <> :excluirId")
    Double sumarArea(@Param("fincaId") Long fincaId,
                     @Param("estado") EstadoSiembra estado,
                     @Param("excluirId") Long excluirId);

    boolean existsByCultivoId(Long cultivoId);
}
