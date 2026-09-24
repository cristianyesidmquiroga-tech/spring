package co.sena.adso.porteria.service;

import co.sena.adso.porteria.dto.CarnetResponseDTO;
import co.sena.adso.porteria.dto.PerfilRequestDTO;
import co.sena.adso.porteria.dto.PerfilResponseDTO;
import co.sena.adso.porteria.entity.Ficha;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.exception.BusinessException;
import co.sena.adso.porteria.exception.DatoInvalidoException;
import co.sena.adso.porteria.exception.ResourceNotFoundException;
import co.sena.adso.porteria.repository.FichaRepository;
import co.sena.adso.porteria.repository.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PerfilService {

    private final AuthService authService;
    private final UsuarioRepository usuarioRepository;
    private final FichaRepository fichaRepository;
    private final DocumentoService documentoService;
    private final CarnetService carnetService;
    private final FotoService fotoService;
    private final AvatarService avatarService;
    private final Clock reloj;

    public PerfilService(AuthService authService, UsuarioRepository usuarioRepository, FichaRepository fichaRepository,
                         DocumentoService documentoService, CarnetService carnetService, FotoService fotoService,
                         AvatarService avatarService, Clock reloj) {
        this.authService = authService;
        this.usuarioRepository = usuarioRepository;
        this.fichaRepository = fichaRepository;
        this.documentoService = documentoService;
        this.carnetService = carnetService;
        this.fotoService = fotoService;
        this.avatarService = avatarService;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public PerfilResponseDTO obtener() {
        return PerfilResponseDTO.fromEntity(authService.usuarioActual());
    }

    @Transactional(readOnly = true)
    public CarnetResponseDTO carnet() {
        return carnetService.construir(authService.usuarioActual());
    }

    @Transactional
    public PerfilResponseDTO actualizar(PerfilRequestDTO datos) {
        Usuario usuario = authService.usuarioActual();
        usuario.setNombres(textoOpcional(datos.nombres()));
        usuario.setApellidos(textoOpcional(datos.apellidos()));

        if (datos.documento() != null && !datos.documento().isBlank()) {
            String tipo = datos.tipoDocumento() != null ? datos.tipoDocumento() : usuario.getTipoDocumento();
            String limpio = documentoService.validar(tipo, datos.documento());
            if (usuarioRepository.existsByDocumentoAndIdNot(limpio, usuario.getId())) {
                throw new BusinessException("Ese número de documento ya está registrado por otra persona");
            }
            usuario.setTipoDocumento(tipo.toUpperCase());
            usuario.setDocumento(limpio);
        }

        if (datos.programa() != null && !datos.programa().isBlank() && !usuario.esAprendiz()) {
            usuario.setPrograma(capitalizar(Texto.limpiar(datos.programa())));
        }

        // El aprendiz elige su ficha y de ella hereda programa y fecha de finalización
        if (usuario.esAprendiz()) {
            Ficha ficha = null;
            if (datos.fichaId() != null) {
                ficha = fichaRepository.findById(datos.fichaId())
                        .orElseThrow(() -> new ResourceNotFoundException("una ficha", datos.fichaId()));
            }
            usuario.asignarFicha(ficha);
        }

        if (datos.tipoSangre() != null && !datos.tipoSangre().isBlank()) {
            String sangre = datos.tipoSangre().trim().toUpperCase();
            if (!Usuario.TIPOS_SANGRE.contains(sangre)) {
                throw new DatoInvalidoException("Tipo de sangre no válido, selecciónalo de la lista");
            }
            usuario.setTipoSangre(sangre);
        }

        usuario.setPerfilCompleto(usuario.calcularPerfilCompleto());
        return PerfilResponseDTO.fromEntity(usuario);
    }

    @Transactional
    public PerfilResponseDTO subirFoto(MultipartFile archivo) {
        Usuario usuario = authService.usuarioActual();
        String nombre = fotoService.guardar(usuario, archivo);
        // La foto queda pendiente: un administrador confirma que es de la persona antes de activar el carnet
        usuario.registrarFotoNueva(nombre, LocalDateTime.now(reloj));
        usuario.setPerfilCompleto(false);
        return PerfilResponseDTO.fromEntity(usuario);
    }

    public record Imagen(Resource archivo, boolean silueta) {
    }

    /** Foto de una persona (la propia, o la de otros si el permiso lo justifica); sin foto, la silueta de su cargo. */
    @Transactional(readOnly = true)
    public Imagen foto(Long usuarioId) {
        Usuario solicitante = authService.usuarioActual();
        boolean puedeVer = solicitante.getId().equals(usuarioId) || solicitante.esAdmin()
                || solicitante.puedeOperarPorteria() || solicitante.puedeAsesorar()
                || solicitante.puedeGestionarAsistencia();
        if (!puedeVer) {
            throw new AccessDeniedException("Sin permiso para ver la foto");
        }
        Usuario dueno = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("un usuario", usuarioId));
        return fotoService.buscar(dueno).map(f -> new Imagen(f, false))
                .orElseGet(() -> new Imagen(avatarService.avatar(dueno.getCargo()), true));
    }

    private static String textoOpcional(String valor) {
        String limpio = Texto.opcional(valor);
        return limpio == null ? null : limpio.replaceAll("\\s+", " ");
    }

    private static String capitalizar(String texto) {
        StringBuilder sb = new StringBuilder();
        for (String palabra : texto.toLowerCase().split("\\s+")) {
            if (!palabra.isEmpty()) {
                sb.append(Character.toUpperCase(palabra.charAt(0))).append(palabra.substring(1)).append(' ');
            }
        }
        return sb.toString().trim();
    }
}
