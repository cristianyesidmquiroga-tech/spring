package co.sena.adso.porteria.exception;

public class CredencialesInvalidasException extends RuntimeException {

    // Un solo mensaje para "no existe" y "contraseña incorrecta", así no se revela quién está registrado
    public CredencialesInvalidasException() {
        super("Correo/documento o contraseña incorrectos");
    }
}
