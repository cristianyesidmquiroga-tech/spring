package co.sena.adso.porteria.dto;

import java.util.List;

public record ComunicadosResponseDTO(String mesActual, List<Destinatario> usuarios, List<Inasistente> inasistentes) {

    public record Destinatario(Long id, String nombre, String documento, String correo, String cargo, String ficha) {
    }

    public record Inasistente(Long id, String nombre, String documento, String correo, String ficha, String programa,
                              long faltas) {
    }
}
