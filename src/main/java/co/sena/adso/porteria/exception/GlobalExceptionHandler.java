package co.sena.adso.porteria.exception;

import co.sena.adso.porteria.dto.ErrorResponseDTO;
import co.sena.adso.porteria.dto.ErrorResponseDTO.CampoInvalido;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> datosInvalidos(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<CampoInvalido> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new CampoInvalido(e.getField(), e.getDefaultMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "Hay datos inválidos en la petición", req, campos, null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> parametroInvalido(ConstraintViolationException ex, HttpServletRequest req) {
        List<CampoInvalido> campos = ex.getConstraintViolations().stream()
                .map(v -> new CampoInvalido(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "Hay parámetros inválidos en la petición", req, campos, null);
    }

    @ExceptionHandler(DatoInvalidoException.class)
    public ResponseEntity<ErrorResponseDTO> datoInvalido(DatoInvalidoException ex, HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null, null);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponseDTO> peticionIlegible(Exception ex, HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST,
                "La petición trae un valor que no corresponde al tipo esperado", req, null, null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDTO> archivoGrande(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST, "La imagen supera el tamaño permitido (8 MB)", req, null, null);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponseDTO> credenciales(CredencialesInvalidasException ex, HttpServletRequest req) {
        return responder(HttpStatus.UNAUTHORIZED, ex.getMessage(), req, null, null);
    }

    @ExceptionHandler(CuentaBloqueadaException.class)
    public ResponseEntity<ErrorResponseDTO> bloqueada(CuentaBloqueadaException ex, HttpServletRequest req) {
        return responder(HttpStatus.FORBIDDEN, ex.getMessage(), req, null, ex.getSegundos());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> sinPermiso(AccessDeniedException ex, HttpServletRequest req) {
        return responder(HttpStatus.FORBIDDEN, "No tienes permiso para esta acción", req, null, null);
    }

    @ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResponseDTO> noEncontrado(Exception ex, HttpServletRequest req) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), req, null, null);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDTO> reglaNegocio(BusinessException ex, HttpServletRequest req) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req, null, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> conflicto(DataIntegrityViolationException ex, HttpServletRequest req) {
        return responder(HttpStatus.CONFLICT,
                "La operación choca con datos que ya existen o que dependen de este registro", req, null, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> inesperado(Exception ex, HttpServletRequest req) {
        log.error("Error no controlado en {}", req.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado, revisa el log del servidor", req, null, null);
    }

    private ResponseEntity<ErrorResponseDTO> responder(HttpStatus status, String mensaje, HttpServletRequest req,
                                                       List<CampoInvalido> campos, Long segundos) {
        ErrorResponseDTO cuerpo = new ErrorResponseDTO(status.value(), status.getReasonPhrase(), mensaje,
                req.getRequestURI(), OffsetDateTime.now(), campos, segundos);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
