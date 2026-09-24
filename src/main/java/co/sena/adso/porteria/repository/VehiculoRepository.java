package co.sena.adso.porteria.repository;

import co.sena.adso.porteria.entity.Vehiculo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    Optional<Vehiculo> findByPlaca(String placa);

    List<Vehiculo> findTop200ByOrderByFechaCreacionDesc();

    @Modifying
    @Query("UPDATE Vehiculo v SET v.activo = false WHERE v.activo = true")
    int desactivarTodos();
}
