package co.sena.adso.porteria.exception;

public class CuentaBloqueadaException extends RuntimeException {

    private final long segundos;

    public CuentaBloqueadaException(String mensaje, long segundos) {
        super(mensaje);
        this.segundos = segundos;
    }

    public long getSegundos() {
        return segundos;
    }
}
