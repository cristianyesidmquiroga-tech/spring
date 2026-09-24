package co.sena.adso.porteria.service;

import co.sena.adso.porteria.dto.CatalogosResponseDTO.TipoDocumento;
import co.sena.adso.porteria.exception.DatoInvalidoException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Tipos de documento y validación del número según el tipo.
 * Longitudes: Registraduría Nacional y Migración Colombia.
 */
@Service
public class DocumentoService {

    public static final String TIPO_POR_DEFECTO = "CC";

    private record Regla(String etiqueta, int minimo, int maximo, boolean soloDigitos) {
    }

    private static final Map<String, Regla> TIPOS = new LinkedHashMap<>();

    static {
        TIPOS.put("CC", new Regla("Cédula de ciudadanía", 6, 10, true));
        TIPOS.put("TI", new Regla("Tarjeta de identidad", 10, 11, true));
        TIPOS.put("CE", new Regla("Cédula de extranjería", 6, 7, true));
        TIPOS.put("PPT", new Regla("Permiso por Protección Temporal", 7, 10, true));
        TIPOS.put("PA", new Regla("Pasaporte", 5, 15, false));
    }

    // CC y TI salen del NUIP, que se asigna desde 1.000.000.000: nunca empiezan por cero
    private static final List<String> SIN_CERO_INICIAL = List.of("CC", "TI");

    public List<TipoDocumento> catalogo() {
        return TIPOS.entrySet().stream()
                .map(e -> new TipoDocumento(e.getKey(), e.getValue().etiqueta(), formato(e.getValue())))
                .toList();
    }

    public boolean esTipoValido(String tipo) {
        return tipo != null && TIPOS.containsKey(tipo.toUpperCase());
    }

    public String normalizar(String numero) {
        return numero == null ? "" : numero.replaceAll("[\\s.\\-]", "").trim();
    }

    /** Devuelve el número limpio o lanza DatoInvalidoException con un mensaje para la persona. */
    public String validar(String tipo, String numero) {
        String clave = tipo == null || tipo.isBlank() ? TIPO_POR_DEFECTO : tipo.toUpperCase();
        Regla regla = TIPOS.get(clave);
        if (regla == null) {
            throw new DatoInvalidoException("Selecciona un tipo de documento válido");
        }
        String limpio = normalizar(numero);
        if (limpio.isEmpty()) {
            throw new DatoInvalidoException("Escribe el número de documento");
        }
        String nombre = regla.etiqueta().toLowerCase();
        if (regla.soloDigitos()) {
            if (!limpio.chars().allMatch(Character::isDigit)) {
                throw new DatoInvalidoException("El número de " + nombre + " debe tener solo números");
            }
            if (SIN_CERO_INICIAL.contains(clave) && limpio.startsWith("0")) {
                throw new DatoInvalidoException("Un número de " + nombre
                        + " no empieza por cero. Revísalo o cambia el tipo de documento");
            }
        } else if (!limpio.chars().allMatch(Character::isLetterOrDigit)) {
            throw new DatoInvalidoException("El " + nombre + " solo admite letras y números");
        }
        if (limpio.length() < regla.minimo() || limpio.length() > regla.maximo()) {
            throw new DatoInvalidoException("Un número de " + nombre + " debe tener " + formato(regla)
                    + " y escribiste " + limpio.length() + ". Revísalo o cambia el tipo de documento");
        }
        return limpio;
    }

    /** Sugerencia de tipo cuando no se indica: con letras solo puede ser pasaporte. */
    public String tipoProbable(String numero) {
        String limpio = normalizar(numero);
        if (!limpio.chars().allMatch(Character::isDigit)) {
            return limpio.isEmpty() ? TIPO_POR_DEFECTO : "PA";
        }
        if (limpio.startsWith("0")) {
            return limpio.length() <= 7 ? "CE" : "PPT";
        }
        return limpio.length() == 11 ? "TI" : TIPO_POR_DEFECTO;
    }

    private static String formato(Regla r) {
        String unidad = r.soloDigitos() ? "dígitos" : "caracteres";
        return r.minimo() == r.maximo() ? r.minimo() + " " + unidad
                : "entre " + r.minimo() + " y " + r.maximo() + " " + unidad;
    }
}
