package co.sena.adso.porteria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioAdminRequestDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite máximo 100 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 100, message = "El correo admite máximo 100 caracteres")
        String correo,

        // Obligatoria al crear; al editar, vacía significa que no se cambia
        @Size(max = 72, message = "La contraseña admite máximo 72 caracteres")
        String contrasena,

        @NotNull(message = "El rol es obligatorio")
        Long rolId,

        @Size(max = 50, message = "El cargo admite máximo 50 caracteres")
        String cargo,

        @Size(max = 5, message = "Tipo de documento inválido")
        String tipoDocumento,

        @Size(max = 30, message = "El documento es demasiado largo")
        String documento,

        @Size(max = 20, message = "La ficha admite máximo 20 caracteres")
        String ficha,

        @Size(max = 100, message = "El programa admite máximo 100 caracteres")
        String programa,

        @Size(max = 20, message = "El horario admite máximo 20 caracteres")
        String horario,

        @Size(max = 100, message = "Autorizado por admite máximo 100 caracteres")
        String autorizadoPor,

        @Size(max = 500, message = "El motivo admite máximo 500 caracteres")
        String motivo
) {
}
