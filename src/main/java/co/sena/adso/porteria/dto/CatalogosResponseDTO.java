package co.sena.adso.porteria.dto;

import java.util.List;

public record CatalogosResponseDTO(
        List<Opcion> roles,
        List<String> cargos,
        List<TipoDocumento> tiposDocumento,
        List<String> tiposSangre,
        List<FichaOpcion> fichas
) {
    public record Opcion(Long id, String nombre) {
    }

    public record TipoDocumento(String codigo, String etiqueta, String formato) {
    }

    public record FichaOpcion(Long id, String numero, String programa) {
    }
}
