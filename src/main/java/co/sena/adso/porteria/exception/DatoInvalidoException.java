package co.sena.adso.porteria.exception;

/** Dato con formato incorrecto que no alcanza a validar una anotación (se responde 400). */
public class DatoInvalidoException extends RuntimeException {

    public DatoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
