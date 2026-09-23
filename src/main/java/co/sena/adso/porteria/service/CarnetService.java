package co.sena.adso.porteria.service;

import co.sena.adso.porteria.dto.CarnetResponseDTO;
import co.sena.adso.porteria.entity.Usuario;
import java.util.Arrays;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Traduce el cargo del sistema al perfil del carnet impreso del SENA y arma sus datos. */
@Service
public class CarnetService {

    public static final String APRENDIZ = "APRENDIZ";
    public static final String INSTRUCTOR = "INSTRUCTOR";
    public static final String CONTRATISTA = "CONTRATISTA";
    public static final String FUNCIONARIO = "FUNCIONARIO";
    public static final String SUBDIRECTOR = "SUBDIRECTOR";

    // La vigilancia se contrata a un tercero, por eso celador sale como contratista
    private static final Map<String, String> PERFIL_POR_CARGO = Map.ofEntries(
            Map.entry("aprendiz", APRENDIZ),
            Map.entry("instructor", INSTRUCTOR),
            Map.entry("celador", CONTRATISTA),
            Map.entry("portería", CONTRATISTA),
            Map.entry("contratista", CONTRATISTA),
            Map.entry("administrativo", FUNCIONARIO),
            Map.entry("administrador", FUNCIONARIO),
            Map.entry("funcionario", FUNCIONARIO),
            Map.entry("coordinacion", FUNCIONARIO),
            Map.entry("subdirector", SUBDIRECTOR));

    private static final Map<String, String> ABREVIATURAS = Map.of(
            "CC", "C.C.", "TI", "T.I.", "CE", "C.E.", "PPT", "PPT", "PA", "Pasaporte");

    public String perfilDeCargo(String cargo) {
        if (cargo == null) {
            return FUNCIONARIO;
        }
        return PERFIL_POR_CARGO.getOrDefault(cargo.trim().toLowerCase(), FUNCIONARIO);
    }

    /**
     * Nombres y apellidos para las dos líneas del carnet. Si la persona no los declaró por
     * separado se reparte el nombre completo solo para pintarlo, nunca se guarda.
     */
    public String[] partirNombre(String completo, String nombres, String apellidos) {
        if (tieneTexto(nombres) && tieneTexto(apellidos)) {
            return new String[]{nombres.trim(), apellidos.trim()};
        }
        String[] palabras = completo == null ? new String[0] : completo.trim().split("\\s+");
        if (palabras.length == 0 || palabras[0].isEmpty()) {
            return new String[]{"", ""};
        }
        if (palabras.length == 1) {
            return new String[]{palabras[0], ""};
        }
        int corte = palabras.length == 3 ? 1 : palabras.length / 2;
        return new String[]{
                String.join(" ", Arrays.copyOfRange(palabras, 0, corte)),
                String.join(" ", Arrays.copyOfRange(palabras, corte, palabras.length))};
    }

    public CarnetResponseDTO construir(Usuario u) {
        String[] nombre = partirNombre(u.getNombre(), u.getNombres(), u.getApellidos());
        String perfil = perfilDeCargo(u.getCargo());
        String documento = u.getDocumento() == null ? ""
                : ABREVIATURAS.getOrDefault(u.getTipoDocumento(), "C.C.") + " " + u.getDocumento();
        String fechaFin = u.getFichaRef() != null ? u.getFichaRef().fechaFinalizacionTexto() : "";
        boolean activo = u.isPerfilCompleto() && u.getDocumento() != null;
        return new CarnetResponseDTO(activo, perfil, nombre[0], nombre[1], documento, u.getTipoSangre(),
                u.numeroFicha(), u.programaCarnet(), fechaFin, activo ? u.getDocumento() : null);
    }

    private static boolean tieneTexto(String s) {
        return s != null && !s.isBlank();
    }
}
