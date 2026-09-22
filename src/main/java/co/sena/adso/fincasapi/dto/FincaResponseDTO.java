package co.sena.adso.fincasapi.dto;

import co.sena.adso.fincasapi.entity.Finca;

public record FincaResponseDTO(
        Long id,
        String nombre,
        String propietario,
        String vereda,
        String municipio,
        Double hectareas
) {
    public static FincaResponseDTO fromEntity(Finca finca) {
        return new FincaResponseDTO(finca.getId(), finca.getNombre(), finca.getPropietario(),
                finca.getVereda(), finca.getMunicipio(), finca.getHectareas());
    }
}
