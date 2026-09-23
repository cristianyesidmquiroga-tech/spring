package co.sena.adso.porteria.config;

import co.sena.adso.porteria.entity.Rol;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.repository.FichaRepository;
import co.sena.adso.porteria.repository.RolRepository;
import co.sena.adso.porteria.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea el administrador principal al arrancar si no existe. Nunca le cambia la contraseña
 * a un admin existente. En desarrollo, si hay DEMO_PASSWORD, crea un usuario por perfil.
 */
@Component
public class DatosIniciales implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final FichaRepository fichaRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminDocumento;
    private final String demoPassword;

    public DatosIniciales(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                          FichaRepository fichaRepository, PasswordEncoder passwordEncoder,
                          @Value("${app.admin.email}") String adminEmail,
                          @Value("${app.admin.password}") String adminPassword,
                          @Value("${app.admin.documento}") String adminDocumento,
                          @Value("${app.demo.password:}") String demoPassword) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.fichaRepository = fichaRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminDocumento = adminDocumento;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        crearAdmin();
        if (demoPassword != null && !demoPassword.isBlank()) {
            crearUsuariosDePrueba();
        }
    }

    private void crearAdmin() {
        if (adminEmail == null || adminEmail.isBlank()) {
            throw new IllegalStateException("Falta ADMIN_EMAIL: configúralo antes de arrancar el sistema");
        }
        if (usuarioRepository.existsByCorreoIgnoreCase(adminEmail)) {
            return;
        }
        if (adminPassword == null || adminPassword.length() < 12) {
            throw new IllegalStateException("No existe el admin y ADMIN_PASSWORD falta o tiene menos de 12 caracteres");
        }
        Usuario admin = new Usuario("Super Administrador", adminEmail.trim().toLowerCase(),
                passwordEncoder.encode(adminPassword), rol(Rol.ADMIN), "Administrador");
        admin.setDocumento(adminDocumento == null || adminDocumento.isBlank() ? null : adminDocumento.trim());
        admin.setTipoSangre("O+");
        admin.setPerfilCompleto(true);
        admin.setCorreoVerificado(true);
        admin.setDebeCambiarContrasena(true);
        usuarioRepository.save(admin);
        log.info("Administrador principal creado; debe cambiar la contraseña en su primer ingreso");
    }

    private void crearUsuariosDePrueba() {
        crearDemo("Laura Celador", "celador@porteria.local", "1098000001", Rol.USUARIO, "Celador", null);
        crearDemo("Andrés Instructor", "instructor@porteria.local", "1098000002", Rol.USUARIO, "Instructor", null);
        crearDemo("María Aprendiz", "aprendiz@porteria.local", "1098000003", Rol.USUARIO, "Aprendiz", "2977385");
        crearDemo("Jorge Administrativo", "administrativo@porteria.local", "1098000004", Rol.USUARIO, "Administrativo", null);
        crearDemo("Carlos Coordinador", "coordinacion@porteria.local", "1098000005", Rol.TRABAJADOR, "Coordinacion", null);
    }

    private void crearDemo(String nombre, String correo, String documento, String rol, String cargo, String ficha) {
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            return;
        }
        Usuario u = new Usuario(nombre, correo, passwordEncoder.encode(demoPassword), rol(rol), cargo);
        u.setDocumento(documento);
        u.setCorreoVerificado(true);
        if (ficha != null) {
            fichaRepository.findByNumero(ficha).ifPresent(u::asignarFicha);
        }
        usuarioRepository.save(u);
    }

    private Rol rol(String nombre) {
        return rolRepository.findByNombre(nombre)
                .orElseThrow(() -> new IllegalStateException("Falta el rol " + nombre + " en la base"));
    }
}
