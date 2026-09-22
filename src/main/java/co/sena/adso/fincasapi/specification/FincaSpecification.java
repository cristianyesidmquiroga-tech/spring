package co.sena.adso.fincasapi.specification;

import co.sena.adso.fincasapi.entity.Finca;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class FincaSpecification {

    private FincaSpecification() {
    }

    /** Arma el filtro solo con los parámetros que llegaron con valor. */
    public static Specification<Finca> filtrar(String municipio, String propietario, Double hectareasMin) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (tieneTexto(municipio)) {
                condiciones.add(cb.equal(cb.lower(root.get("municipio")), municipio.trim().toLowerCase()));
            }
            if (tieneTexto(propietario)) {
                condiciones.add(cb.like(cb.lower(root.get("propietario")), "%" + propietario.trim().toLowerCase() + "%"));
            }
            if (hectareasMin != null) {
                condiciones.add(cb.greaterThanOrEqualTo(root.get("hectareas"), hectareasMin));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
