package co.sena.adso.porteria.dto;

import jakarta.validation.constraints.Size;

// Los obligatorios se revisan en el servicio, en el mismo orden que Portería 2
public record RegistroRequestDTO(
        @Size(max = 100, message = "El nombre admite máximo 100 caracteres")
        String nombre,

        @Size(max = 100, message = "El correo admite máximo 100 caracteres")
        String correo,

        @Size(max = 5, message = "Tipo de documento inválido")
        String tipoDocumento,

        @Size(max = 30, message = "El documento es demasiado largo")
        String documento,

        @Size(max = 72, message = "La contraseña es demasiado larga")
        String password,

        @Size(max = 72, message = "La contraseña es demasiado larga")
        String confirmacion,

        @Size(max = 50, message = "Cargo inválido")
        String cargo,

        @Size(max = 20, message = "El número de ficha es demasiado largo")
        String ficha,

        @Size(max = 20, message = "Horario inválido")
        String horario,

        boolean aceptaDatos,

        @Size(max = 4000, message = "Verificación inválida")
        String captcha
) {
}
