package co.sena.adso.porteria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "usuarios")
public class Usuario {

    // El cargo gobierna permisos, por eso solo se aceptan estos valores
    public static final List<String> CARGOS_VALIDOS = List.of(
            "Aprendiz", "Instructor", "Administrativo", "Celador", "Portería",
            "Administrador", "Coordinacion", "Subdirector", "Contratista", "Funcionario");

    public static final List<String> CARGOS_ASESORES = List.of("Administrador", "Administrativo");

    public static final List<String> TIPOS_SANGRE = List.of("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-");

    public static final String FOTO_SIN_FOTO = "sin_foto";
    public static final String FOTO_PENDIENTE = "pendiente";
    public static final String FOTO_APROBADA = "aprobada";
    public static final String FOTO_RECHAZADA = "rechazada";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 100)
    private String nombres;

    @Column(length = 100)
    private String apellidos;

    @Column(nullable = false, unique = true, length = 100)
    private String correo;

    @Column(nullable = false, length = 100)
    private String contrasena;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rol_id")
    private Rol rol;

    @Column(length = 50)
    private String cargo;

    @Column(name = "tipo_documento", nullable = false, length = 5)
    private String tipoDocumento = "CC";

    @Column(unique = true, length = 20)
    private String documento;

    @Column(length = 150)
    private String programa;

    // Número de ficha en texto, para quien no tiene la ficha enlazada
    @Column(length = 20)
    private String ficha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ficha_id")
    private Ficha fichaRef;

    @Column(length = 20)
    private String horario;

    @Column(name = "tipo_sangre", length = 5)
    private String tipoSangre;

    @Column(name = "perfil_completo", nullable = false)
    private boolean perfilCompleto;

    @Column(name = "correo_verificado", nullable = false)
    private boolean correoVerificado;

    @Column(name = "debe_cambiar_contrasena", nullable = false)
    private boolean debeCambiarContrasena;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "session_token", length = 64)
    private String sessionToken;

    private String foto;

    @Column(name = "foto_estado", nullable = false, length = 20)
    private String fotoEstado = FOTO_SIN_FOTO;

    @Column(name = "foto_motivo", columnDefinition = "TEXT")
    private String fotoMotivo;

    @Column(name = "foto_revisada_por")
    private Long fotoRevisadaPor;

    @Column(name = "foto_fecha_revision")
    private LocalDateTime fotoFechaRevision;

    @Column(name = "foto_fecha_subida")
    private LocalDateTime fotoFechaSubida;

    @Column(name = "tutorial_visto", nullable = false)
    private boolean tutorialVisto;

    @Column(name = "codigo_verificacion", length = 6)
    private String codigoVerificacion;

    @Column(name = "codigo_expiracion")
    private LocalDateTime codigoExpiracion;

    @Column(name = "codigo_recuperacion", length = 6)
    private String codigoRecuperacion;

    @Column(name = "recuperacion_expiracion")
    private LocalDateTime recuperacionExpiracion;

    // Separado de intentos_fallidos: fallar códigos no debe bloquear ni desbloquear el login
    @Column(name = "intentos_codigo", nullable = false)
    private int intentosCodigo;

    @Column(name = "recuperacion_permiso", length = 64)
    private String recuperacionPermiso;

    protected Usuario() {
    }

    public Usuario(String nombre, String correo, String contrasenaHash, Rol rol, String cargo) {
        this.nombre = nombre;
        this.correo = correo;
        this.contrasena = contrasenaHash;
        this.rol = rol;
        this.cargo = cargo;
    }

    // Permisos: salen de combinar rol y cargo, igual que en el sistema original

    public boolean esAdmin() {
        return rolEs(Rol.ADMIN);
    }

    public boolean esAprendiz() {
        return "Aprendiz".equals(cargo);
    }

    public boolean esInstructor() {
        return "Instructor".equals(cargo);
    }

    public boolean esCelador() {
        return "Celador".equals(cargo) || "Portería".equals(cargo);
    }

    public boolean puedeOperarPorteria() {
        return esAdmin() || (rolEs(Rol.USUARIO) && (esCelador() || "Administrador".equals(cargo)));
    }

    public boolean puedeAsesorar() {
        return esAdmin() || (rolEs(Rol.USUARIO) && CARGOS_ASESORES.contains(cargo));
    }

    public boolean puedeGestionarAsistencia() {
        return esAdmin() || esInstructor();
    }

    public boolean puedeRegistrarEquipos() {
        return esAdmin() || (rolEs(Rol.USUARIO) && !esCelador());
    }

    public boolean puedeVerAmbientes() {
        return esAdmin() || "Coordinacion".equals(cargo) || "Subdirector".equals(cargo);
    }

    public boolean estaBloqueado(LocalDateTime ahora) {
        return bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta);
    }

    public boolean tieneFotoPropia() {
        return foto != null && !foto.isBlank();
    }

    /** El carnet solo se activa con los datos completos y la foto aprobada por un administrador. */
    public boolean calcularPerfilCompleto() {
        boolean datos = documento != null && tipoSangre != null && tieneFotoPropia();
        if (esAprendiz()) {
            datos = datos && (fichaRef != null || (ficha != null && !ficha.isBlank()));
        }
        return datos && FOTO_APROBADA.equals(fotoEstado);
    }

    public String numeroFicha() {
        return fichaRef != null ? fichaRef.getNumero() : ficha;
    }

    public String programaCarnet() {
        return fichaRef != null ? fichaRef.getPrograma() : programa;
    }

    // Solo la ficha tiene fecha: el texto histórico nunca la guardó
    public String fechaFinalizacionCarnet() {
        return fichaRef != null ? fichaRef.fechaFinalizacionTexto() : "";
    }

    private boolean rolEs(String nombreRol) {
        return rol != null && nombreRol.equals(rol.getNombre());
    }

    public void registrarIntentoFallido(int maximo, LocalDateTime bloqueoHasta) {
        intentosFallidos++;
        if (intentosFallidos >= maximo) {
            bloqueadoHasta = bloqueoHasta;
        }
    }

    public void limpiarBloqueo() {
        intentosFallidos = 0;
        bloqueadoHasta = null;
    }

    public void asignarFicha(Ficha nueva) {
        this.fichaRef = nueva;
        if (nueva != null) {
            this.ficha = nueva.getNumero();
            this.programa = nueva.getPrograma();
        }
    }

    public void registrarFotoNueva(String archivo, LocalDateTime fecha) {
        this.foto = archivo;
        this.fotoEstado = FOTO_PENDIENTE;
        this.fotoMotivo = null;
        this.fotoRevisadaPor = null;
        this.fotoFechaRevision = null;
        this.fotoFechaSubida = fecha;
    }

    public void revisarFoto(boolean aprobada, String motivo, Long revisor, LocalDateTime fecha) {
        this.fotoEstado = aprobada ? FOTO_APROBADA : FOTO_RECHAZADA;
        this.fotoMotivo = aprobada ? null : motivo;
        if (!aprobada) {
            this.foto = null;
        }
        this.fotoRevisadaPor = revisor;
        this.fotoFechaRevision = fecha;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }
    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }
    public String getPrograma() { return programa; }
    public void setPrograma(String programa) { this.programa = programa; }
    public String getFicha() { return ficha; }
    public void setFicha(String ficha) { this.ficha = ficha; }
    public Ficha getFichaRef() { return fichaRef; }
    public String getHorario() { return horario; }
    public void setHorario(String horario) { this.horario = horario; }
    public String getTipoSangre() { return tipoSangre; }
    public void setTipoSangre(String tipoSangre) { this.tipoSangre = tipoSangre; }
    public boolean isPerfilCompleto() { return perfilCompleto; }
    public void setPerfilCompleto(boolean perfilCompleto) { this.perfilCompleto = perfilCompleto; }
    public boolean isCorreoVerificado() { return correoVerificado; }
    public void setCorreoVerificado(boolean correoVerificado) { this.correoVerificado = correoVerificado; }
    public boolean isDebeCambiarContrasena() { return debeCambiarContrasena; }
    public void setDebeCambiarContrasena(boolean debeCambiarContrasena) { this.debeCambiarContrasena = debeCambiarContrasena; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public LocalDateTime getBloqueadoHasta() { return bloqueadoHasta; }
    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
    public String getFoto() { return foto; }
    public String getFotoEstado() { return fotoEstado; }
    public String getFotoMotivo() { return fotoMotivo; }
    public LocalDateTime getFotoFechaSubida() { return fotoFechaSubida; }
    public boolean isTutorialVisto() { return tutorialVisto; }
    public void marcarTutorialVisto() { this.tutorialVisto = true; }
    public String getCodigoVerificacion() { return codigoVerificacion; }
    public LocalDateTime getCodigoExpiracion() { return codigoExpiracion; }
    public String getCodigoRecuperacion() { return codigoRecuperacion; }
    public LocalDateTime getRecuperacionExpiracion() { return recuperacionExpiracion; }
    public int getIntentosCodigo() { return intentosCodigo; }
    public String getRecuperacionPermiso() { return recuperacionPermiso; }

    public void nuevoCodigoVerificacion(String codigo, LocalDateTime vence) {
        codigoVerificacion = codigo;
        codigoExpiracion = vence;
        intentosCodigo = 0;
    }

    public void verificarCorreo() {
        correoVerificado = true;
        codigoVerificacion = null;
        codigoExpiracion = null;
        intentosCodigo = 0;
    }

    public int sumarIntentoCodigo() {
        return ++intentosCodigo;
    }

    public void anularCodigoVerificacion() {
        codigoVerificacion = null;
    }

    public void nuevoCodigoRecuperacion(String codigo, LocalDateTime vence) {
        codigoRecuperacion = codigo;
        recuperacionExpiracion = vence;
        recuperacionPermiso = null;
        intentosCodigo = 0;
    }

    public void anularRecuperacion() {
        codigoRecuperacion = null;
        recuperacionExpiracion = null;
        recuperacionPermiso = null;
    }

    // El código ya no sirve; solo queda el permiso para el paso de cambio
    public void concederPermisoRecuperacion(String permiso) {
        codigoRecuperacion = null;
        recuperacionPermiso = permiso;
        intentosCodigo = 0;
    }

    // Cierra toda sesión abierta con la contraseña anterior
    public void restablecerContrasena(String hash) {
        contrasena = hash;
        anularRecuperacion();
        intentosCodigo = 0;
        debeCambiarContrasena = false;
        sessionToken = null;
        limpiarBloqueo();
    }
}
