package co.sena.adso.porteria.exception;

/** La operación choca con el estado actual (por ejemplo, entrada de alguien que ya está adentro). */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
