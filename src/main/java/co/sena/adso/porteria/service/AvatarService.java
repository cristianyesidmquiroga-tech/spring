package co.sena.adso.porteria.service;

import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/** Silueta que se muestra mientras la persona no tiene foto, según su cargo o el tipo de pase. */
@Service
public class AvatarService {

    public static final String GENERICO = "generico.svg";

    private static final Map<String, String> POR_CARGO = Map.ofEntries(
            Map.entry("aprendiz", "aprendiz.svg"),
            Map.entry("instructor", "instructor.svg"),
            Map.entry("celador", "celador.svg"),
            Map.entry("porteria", "celador.svg"),
            Map.entry("portería", "celador.svg"),
            Map.entry("administrador", "administrador.svg"),
            Map.entry("administrativo", "administrativo.svg"),
            Map.entry("visitante", "visitante.svg"),
            Map.entry("vehiculo", "vehiculo.svg"),
            Map.entry("objetoexterno", "objeto.svg"));

    // El cargo es texto libre (formularios, Excel): se compara sin mayúsculas ni espacios
    public String archivoDe(String cargo) {
        return cargo == null ? GENERICO : POR_CARGO.getOrDefault(cargo.trim().toLowerCase(), GENERICO);
    }

    public Resource avatar(String cargo) {
        return new ClassPathResource("avatares/" + archivoDe(cargo));
    }
}
