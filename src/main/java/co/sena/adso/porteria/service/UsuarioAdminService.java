package co.sena.adso.porteria.service;

import co.sena.adso.porteria.dto.AutorizacionRequestDTO;
import co.sena.adso.porteria.dto.FotoRevisionResponseDTO;
import co.sena.adso.porteria.dto.FotosRevisionResponseDTO;
import co.sena.adso.porteria.dto.RevisionFotoRequestDTO;
import co.sena.adso.porteria.dto.RevisionFotoResponseDTO;
import co.sena.adso.porteria.dto.UsuarioAdminRequestDTO;
import co.sena.adso.porteria.dto.UsuarioAdminResponseDTO;
import co.sena.adso.porteria.entity.Rol;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.exception.BusinessException;
import co.sena.adso.porteria.exception.DatoInvalidoException;
import co.sena.adso.porteria.exception.ResourceNotFoundException;
import co.sena.adso.porteria.repository.AccesoRepository;
import co.sena.adso.porteria.repository.FichaRepository;
import co.sena.adso.porteria.repository.RolRepository;
import co.sena.adso.porteria.repository.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioAdminService {

    private static final String TABLA = "usuarios";
    private static final int FOTOS_POR_PAGINA = 24;
    private static final List<String> ESTADOS_REVISION =
            List.of(Usuario.FOTO_PENDIENTE, Usuario.FOTO_APROBADA, Usuario.FOTO_RECHAZADA, "todos");

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final FichaRepository fichaRepository;
    private final AccesoRepository accesoRepository;
    private final DocumentoService documentoService;
    private final AuthService authService;
    private final AuditoriaService auditoriaService;
    private final FotoService fotoService;
    private final PasswordEncoder passwordEncoder;
    private final MensajeService mensajeService;
    private final Clock reloj;

    public UsuarioAdminService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                               FichaRepository fichaRepository, AccesoRepository accesoRepository,
                               DocumentoService documentoService,
                               AuthService authService, AuditoriaService auditoriaService, FotoService fotoService,
                               PasswordEncoder passwordEncoder, MensajeService mensajeService, Clock reloj) {
        this.mensajeService = mensajeService;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.fichaRepository = fichaRepository;
        this.accesoRepository = accesoRepository;
        this.documentoService = documentoService;
        this.authService = authService;
        this.auditoriaService = auditoriaService;
        this.fotoService = fotoService;
        this.passwordEncoder = passwordEncoder;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Page<UsuarioAdminResponseDTO> listar(String texto, Long rolId, String cargo, Pageable pageable) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        String filtroTexto = texto == null ? "" : texto.trim().toLowerCase();
        return usuarioRepository.buscar(filtroTexto, rolId == null ? 0L : rolId, cargo == null ? "" : cargo, pageable)
                .map(u -> UsuarioAdminResponseDTO.fromEntity(u, ahora));
    }

    @Transactional(readOnly = true)
    public UsuarioAdminResponseDTO obtener(Long id) {
        return UsuarioAdminResponseDTO.fromEntity(buscar(id), LocalDateTime.now(reloj));
    }

    @Transactional
    public UsuarioAdminResponseDTO crear(UsuarioAdminRequestDTO datos) {
        if (datos.contrasena() == null || datos.contrasena().isBlank()) {
            throw new DatoInvalidoException("La contraseña temporal es obligatoria");
        }
        authService.validarContrasena(datos.contrasena(), null);
        String correo = datos.correo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new BusinessException("El correo ya está registrado");
        }
        Rol rol = buscarRol(datos.rolId());
        Usuario usuario = new Usuario(datos.nombre().trim(), correo, passwordEncoder.encode(datos.contrasena()),
                rol, validarCargo(datos.cargo()));
        aplicarDocumento(usuario, datos.tipoDocumento(), datos.documento());
        aplicarDatosFormacion(usuario, datos);

        // Las cuentas de rol Usuario completan su perfil y cambian la contraseña temporal al entrar
        boolean esUsuarioNormal = Rol.USUARIO.equals(rol.getNombre());
        usuario.setPerfilCompleto(!esUsuarioNormal);
        usuario.setDebeCambiarContrasena(esUsuarioNormal);
        usuario.setCorreoVerificado(true);
        usuarioRepository.save(usuario);

        auditoriaService.registrar(authService.usuarioActual(), TABLA, usuario.getId(), "Creación de usuario",
                datos.autorizadoPor(), datos.motivo(),
                "Alta de " + usuario.getCorreo() + " con rol " + rol.getNombre() + " y cargo " + usuario.getCargo());
        return UsuarioAdminResponseDTO.fromEntity(usuario, LocalDateTime.now(reloj));
    }

    @Transactional
    public UsuarioAdminResponseDTO editar(Long id, UsuarioAdminRequestDTO datos) {
        Usuario usuario = buscar(id);
        String cargoAnterior = usuario.getCargo();
        Long rolAnterior = usuario.getRol().getId();

        String correo = datos.correo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreoIgnoreCaseAndIdNot(correo, id)) {
            throw new BusinessException("El correo ya está registrado");
        }
        usuario.setNombre(datos.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setRol(buscarRol(datos.rolId()));
        usuario.setCargo(validarCargo(datos.cargo()));
        aplicarDocumento(usuario, datos.tipoDocumento(), datos.documento());
        aplicarDatosFormacion(usuario, datos);

        boolean cambiaContrasena = datos.contrasena() != null && !datos.contrasena().isBlank();
        if (cambiaContrasena) {
            authService.validarContrasena(datos.contrasena(), null);
            usuario.setContrasena(passwordEncoder.encode(datos.contrasena()));
        }
        // Cambiar rol o contraseña expulsa las sesiones abiertas de esa persona
        if (cambiaContrasena || !rolAnterior.equals(usuario.getRol().getId())) {
            usuario.setSessionToken(null);
        }
        // El cargo decide si se exige ficha: al cambiarlo se recalcula el perfil de quien debe completarlo
        boolean cambioCargo = cargoAnterior == null ? usuario.getCargo() != null : !cargoAnterior.equals(usuario.getCargo());
        if (cambioCargo && Rol.USUARIO.equals(usuario.getRol().getNombre())) {
            usuario.setPerfilCompleto(usuario.calcularPerfilCompleto());
        }

        auditoriaService.registrar(authService.usuarioActual(), TABLA, id, "Edición de usuario",
                datos.autorizadoPor(), datos.motivo(),
                "Edición de " + usuario.getNombre() + " (" + usuario.getCorreo() + ")"
                        + (cambiaContrasena ? ", con cambio de contraseña" : ""));
        return UsuarioAdminResponseDTO.fromEntity(usuario, LocalDateTime.now(reloj));
    }

    @Transactional
    public void eliminar(Long id, AutorizacionRequestDTO autorizacion) {
        Usuario admin = authService.usuarioActual();
        if (admin.getId().equals(id)) {
            throw new BusinessException("No puedes eliminar tu propia cuenta");
        }
        Usuario usuario = buscar(id);
        if (usuario.esAdmin()) {
            throw new AccessDeniedException("Las cuentas con rol Admin no se eliminan, solo se editan");
        }
        String descripcion = usuario.getNombre() + " (" + usuario.getCorreo() + ")";
        String foto = usuario.getFoto();
        auditoriaService.registrar(admin, TABLA, id, "Eliminación permanente",
                autorizacion.autorizadoPor(), autorizacion.motivo(), "Eliminación del perfil de " + descripcion);
        // accesos no tiene clave foránea hacia usuarios (la referencia es polimórfica): se borran aparte
        accesoRepository.borrarDeUsuario(id);
        usuarioRepository.delete(usuario);
        fotoService.borrar(foto);
    }

    @Transactional
    public UsuarioAdminResponseDTO desbloquear(Long id) {
        Usuario usuario = buscar(id);
        usuario.limpiarBloqueo();
        auditoriaService.registrar(authService.usuarioActual(), TABLA, id, "Desbloqueo de cuenta", null, null,
                "Se quitó el bloqueo por intentos fallidos a " + usuario.getCorreo());
        return UsuarioAdminResponseDTO.fromEntity(usuario, LocalDateTime.now(reloj));
    }

    // Paginado: aun con estado "todos" solo se expone el trozo que el admin está mirando (Ley 1581)
    @Transactional(readOnly = true)
    public FotosRevisionResponseDTO fotos(String estado, int pagina) {
        String filtro = ESTADOS_REVISION.contains(estado) ? estado : Usuario.FOTO_PENDIENTE;
        Pageable orden = PageRequest.of(pagina, FOTOS_POR_PAGINA,
                Sort.by(Sort.Order.asc("fotoFechaSubida").nullsFirst(), Sort.Order.asc("id")));
        Page<Usuario> usuarios = "todos".equals(filtro)
                ? usuarioRepository.findByFotoEstadoNot(Usuario.FOTO_SIN_FOTO, orden)
                : usuarioRepository.findByFotoEstado(filtro, orden);
        return FotosRevisionResponseDTO.de(usuarios.map(FotoRevisionResponseDTO::fromEntity), filtro,
                usuarioRepository.countByFotoEstado(Usuario.FOTO_PENDIENTE));
    }

    @Transactional
    public RevisionFotoResponseDTO revisarFoto(Long id, RevisionFotoRequestDTO revision) {
        Usuario usuario = buscar(id);
        if (usuario.getFoto() == null) {
            throw new DatoInvalidoException("Ese usuario no tiene foto que revisar");
        }
        String motivo = Texto.opcional(revision.motivo());
        if (!revision.aprobada() && motivo == null) {
            throw new DatoInvalidoException("Escribe el motivo del rechazo para que la persona sepa qué corregir");
        }
        // Idempotente: un doble clic no debe registrar dos veces la misma aprobación
        if (revision.aprobada() && Usuario.FOTO_APROBADA.equals(usuario.getFotoEstado())) {
            return new RevisionFotoResponseDTO("Esa foto ya estaba aprobada.",
                    usuarioRepository.countByFotoEstado(Usuario.FOTO_PENDIENTE));
        }
        Usuario admin = authService.usuarioActual();
        if (!revision.aprobada()) {
            // La foto rechazada no corresponde a la persona: no se conserva (minimización, Ley 1581)
            fotoService.borrar(usuario.getFoto());
        }
        usuario.revisarFoto(revision.aprobada(), motivo, admin.getId(), LocalDateTime.now(reloj));
        usuario.setPerfilCompleto(usuario.calcularPerfilCompleto());
        auditoriaService.registrar(admin, TABLA, id, revision.aprobada() ? "Foto aprobada" : "Foto rechazada",
                null, motivo, "Revisión de la foto de " + usuario.getNombre());
        // El correo se puede perder; el hilo vive en el sistema y ahí mismo la persona puede responder
        mensajeService.registrar(id, admin, revision.aprobada()
                ? "Tu foto de perfil fue aprobada. Tu carnet digital ya está activo."
                : "Tu foto de perfil no fue aprobada. Motivo: " + motivo + "\n\nSube una foto nueva que cumpla los "
                        + "requisitos. Si tienes algún problema para hacerlo, respóndeme por aquí.", true);
        usuarioRepository.flush();
        return new RevisionFotoResponseDTO(revision.aprobada() ? "Foto aprobada" : "Foto rechazada",
                usuarioRepository.countByFotoEstado(Usuario.FOTO_PENDIENTE));
    }

    private void aplicarDocumento(Usuario usuario, String tipo, String numero) {
        if (numero == null || numero.isBlank()) {
            usuario.setDocumento(null);
            return;
        }
        String tipoFinal = tipo;
        if (tipoFinal == null || tipoFinal.isBlank()) {
            // Sin tipo solo se deduce si es todo dígitos; con letras sería pasaporte y hay que declararlo
            if (!documentoService.normalizar(numero).chars().allMatch(Character::isDigit)) {
                throw new DatoInvalidoException("Indica el tipo de documento: un número con letras solo es válido como pasaporte");
            }
            tipoFinal = documentoService.tipoProbable(numero);
        }
        String limpio = documentoService.validar(tipoFinal, numero);
        boolean duplicado = usuario.getId() == null ? usuarioRepository.existsByDocumento(limpio)
                : usuarioRepository.existsByDocumentoAndIdNot(limpio, usuario.getId());
        if (duplicado) {
            throw new BusinessException("El documento ya está registrado");
        }
        usuario.setTipoDocumento(tipoFinal.toUpperCase());
        usuario.setDocumento(limpio);
    }

    // Si la ficha ya existe, el aprendiz hereda programa y fecha; nunca se crean fichas desde aquí
    private void aplicarDatosFormacion(Usuario usuario, UsuarioAdminRequestDTO datos) {
        usuario.setHorario(textoOpcional(datos.horario()));
        usuario.setPrograma(textoOpcional(datos.programa()));
        String numero = textoOpcional(datos.ficha());
        usuario.setFicha(numero);
        usuario.asignarFicha(numero == null ? null : fichaRepository.findByNumero(numero).orElse(null));
    }

    private String validarCargo(String cargo) {
        String valor = textoOpcional(cargo);
        if (valor != null && !Usuario.CARGOS_VALIDOS.contains(valor)) {
            throw new DatoInvalidoException("El cargo seleccionado no es válido");
        }
        return valor;
    }

    private Rol buscarRol(Long id) {
        return rolRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("un rol", id));
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("un usuario", id));
    }

    private static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
