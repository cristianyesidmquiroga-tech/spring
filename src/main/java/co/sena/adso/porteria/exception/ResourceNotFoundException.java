package co.sena.adso.porteria.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String recurso, Long id) {
        super("No existe " + recurso + " con id " + id);
    }
}
