package co.sena.adso.porteria.dto;

// Contadores del menú lateral y aviso de limpieza próxima para el admin
public record AvisosResponseDTO(long mensajesSinLeer, long hilosPendientes, long fotosPendientes, String avisoRespaldo) {
}
