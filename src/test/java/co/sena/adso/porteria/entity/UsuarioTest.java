package co.sena.adso.porteria.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

/** Los permisos salen de combinar rol y cargo, igual que en el sistema original. */
class UsuarioTest {

    private static Usuario usuario(String rol, String cargo) {
        Rol r = BeanUtils.instantiateClass(Rol.class);
        ReflectionTestUtils.setField(r, "nombre", rol);
        return new Usuario("Prueba", "p@porteria.local", "hash", r, cargo);
    }

    @Test
    void celadorOperaPorteriaPeroNoRegistraEquipos() {
        Usuario celador = usuario(Rol.USUARIO, "Celador");
        assertThat(celador.puedeOperarPorteria()).isTrue();
        assertThat(celador.puedeRegistrarEquipos()).isFalse();
        assertThat(celador.puedeAsesorar()).isFalse();
    }

    @Test
    void trabajadorNoOperaPorteriaNiRegistraEquipos() {
        Usuario trabajador = usuario(Rol.TRABAJADOR, "Celador");
        assertThat(trabajador.puedeOperarPorteria()).isFalse();
        assertThat(trabajador.puedeRegistrarEquipos()).isFalse();
    }

    @Test
    void trabajadorInstructorSiPasaAsistenciaPorqueMiraElCargo() {
        assertThat(usuario(Rol.TRABAJADOR, "Instructor").puedeGestionarAsistencia()).isTrue();
    }

    @Test
    void cargoAdministradorConRolUsuarioOperaYAsesoraPeroNoEsAdmin() {
        Usuario u = usuario(Rol.USUARIO, "Administrador");
        assertThat(u.puedeOperarPorteria()).isTrue();
        assertThat(u.puedeAsesorar()).isTrue();
        assertThat(u.esAdmin()).isFalse();
    }

    @Test
    void aprendizNecesitaFichaYFotoAprobadaParaElCarnet() {
        Usuario u = usuario(Rol.USUARIO, "Aprendiz");
        u.setDocumento("1098000003");
        u.setTipoSangre("O+");
        u.registrarFotoNueva("foto.jpg", LocalDateTime.now());
        u.revisarFoto(true, null, 1L, LocalDateTime.now());
        assertThat(u.calcularPerfilCompleto()).isFalse();

        u.setFicha("2977385");
        assertThat(u.calcularPerfilCompleto()).isTrue();
    }

    @Test
    void fotoPendienteNoActivaElCarnet() {
        Usuario u = usuario(Rol.USUARIO, "Instructor");
        u.setDocumento("1098000002");
        u.setTipoSangre("A+");
        u.registrarFotoNueva("foto.jpg", LocalDateTime.now());
        assertThat(u.calcularPerfilCompleto()).isFalse();
    }
}
