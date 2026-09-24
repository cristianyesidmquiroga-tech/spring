package co.sena.adso.porteria.repository;

import co.sena.adso.porteria.entity.ObjetoExterno;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObjetoExternoRepository extends JpaRepository<ObjetoExterno, Long> {

    Optional<ObjetoExterno> findBySerial(String serial);

    boolean existsBySerialAndIdNot(String serial, Long id);

    List<ObjetoExterno> findTop200ByOrderByFechaCreacionDesc();
}
