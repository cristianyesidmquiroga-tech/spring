package co.sena.adso.porteria.modulos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Equipo;
import co.sena.adso.porteria.entity.Rol;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.repository.EquipoRepository;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

// Portería 2: tests/modulos/test_porteria.py
class PorteriaTest extends PruebaIntegracion {

    @Autowired
    private EquipoRepository equipoRepository;

    private Usuario aprendiz(String correo, String documento) {
        return crearUsuario(correo, "Aprendiz", Rol.USUARIO, documento, u -> { });
    }

    private ResultActions movimiento(Sesion sesion, String tipoEntidad, Object id, String tipo, List<Long> equipos)
            throws Exception {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("tipoEntidad", tipoEntidad);
        cuerpo.put("entidadId", id);
        cuerpo.put("tipo", tipo);
        cuerpo.put("equiposIds", equipos);
        return mvc.perform(conJson(sesion, post("/api/porteria/movimientos"), cuerpo));
    }

    private long accesos(String condicion, Object... valores) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM accesos WHERE " + condicion, Long.class, valores);
    }

    private String estadoDe(Equipo equipo) {
        return equipoRepository.findById(equipo.getId()).orElseThrow().getEstado();
    }

    @Test
    void aprendizNoEntraAlEscaner() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        mvc.perform(con(aprendiz, get("/api/porteria/verificar").param("codigo", "123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void celadorEntraAlEscaner() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        mvc.perform(con(celador, get("/api/porteria/verificar").param("codigo", "123")))
                .andExpect(status().isOk());
    }

    @Test
    void aprendizNoRegistraMovimientos() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        movimiento(aprendiz, "Usuario", aprendiz.usuario().getId(), "Entrada", null).andExpect(status().isForbidden());
    }

    @Test
    void celadorNoEntraAGestionDeUsuarios() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        mvc.perform(con(celador, get("/api/admin/usuarios"))).andExpect(status().isForbidden());
    }

    @Test
    void entradaValidaSeRegistra() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        Usuario aprendiz = aprendiz("aprendiz@sena.edu.co", "123123");

        movimiento(celador, "Usuario", aprendiz.getId(), "Entrada", null).andExpect(status().isCreated());
        assertThat(accesos("referencia_id = ? AND tipo = 'Entrada'", aprendiz.getId())).isEqualTo(1);
    }

    @Test
    void dobleEntradaSeRechazaYSeAudita() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        Usuario aprendiz = aprendiz("aprendiz@sena.edu.co", "123123");

        movimiento(celador, "Usuario", aprendiz.getId(), "Entrada", null);
        movimiento(celador, "Usuario", aprendiz.getId(), "Entrada", null).andExpect(status().isConflict());

        assertThat(accesos("referencia_id = ? AND tipo = 'Entrada'", aprendiz.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auditoria WHERE accion = 'Inconsistencia de acceso detectada'",
                Long.class)).isEqualTo(1);
    }

    @Test
    void salidaSinEntradaSeRechaza() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        Usuario aprendiz = aprendiz("aprendiz@sena.edu.co", "123123");

        movimiento(celador, "Usuario", aprendiz.getId(), "Salida", null).andExpect(status().isConflict());
        assertThat(accesos("referencia_id = ?", aprendiz.getId())).isZero();
    }

    @Test
    void movimientoInvalidoSeRechaza() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        Usuario aprendiz = aprendiz("aprendiz@sena.edu.co", "123123");

        movimiento(celador, "Usuario", aprendiz.getId(), "Teletransporte", null).andExpect(status().isBadRequest());
    }

    @Test
    void entidadInventadaNoCreaRegistro() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        aprendiz("aprendiz@sena.edu.co", "123123");

        movimiento(celador, "Fantasma", 99999, "LoQueSea", null).andExpect(status().isBadRequest());
        assertThat(accesos("tipo_referencia = 'Fantasma'")).isZero();
    }

    @Test
    void elAccesoRegistraQuienLoHizo() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        Usuario aprendiz = aprendiz("aprendiz@sena.edu.co", "123123");

        movimiento(celador, "Usuario", aprendiz.getId(), "Entrada", null);
        assertThat(jdbc.queryForObject("SELECT operador_id FROM accesos WHERE referencia_id = ?", Long.class,
                aprendiz.getId())).isEqualTo(celador.usuario().getId());
    }

    @Test
    void noSePuedenMarcarEquiposDeOtro() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        Usuario ana = aprendiz("ana@sena.edu.co", "111111");
        Usuario beto = aprendiz("beto@sena.edu.co", "222222");
        Equipo deBeto = equipoRepository.save(new Equipo("Portatil de Beto", null, "Portátil", beto.getId()));

        movimiento(celador, "Usuario", ana.getId(), "Entrada", List.of(deBeto.getId()));

        assertThat(estadoDe(deBeto)).as("no debe cambiar el equipo ajeno").isEqualTo("Afuera");
    }

    @Test
    void equipoPropioCambiaDeEstado() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        Usuario ana = aprendiz("ana@sena.edu.co", "111111");
        Equipo equipo = equipoRepository.save(new Equipo("Portatil de Ana", null, "Portátil", ana.getId()));

        movimiento(celador, "Usuario", ana.getId(), "Entrada", List.of(equipo.getId()));

        assertThat(estadoDe(equipo)).isEqualTo("Adentro");
    }

    @Test
    void noSePuedeBorrarElEquipoDeOtro() throws Exception {
        Sesion ana = entrarComo(Perfil.APRENDIZ);
        Usuario beto = aprendiz("beto@sena.edu.co", "222222");
        Equipo equipo = equipoRepository.save(new Equipo("Portatil de Beto", null, "Portátil", beto.getId()));

        mvc.perform(con(ana, delete("/api/equipos/" + equipo.getId()))).andExpect(status().isForbidden());
        assertThat(equipoRepository.existsById(equipo.getId())).isTrue();
    }

    @Test
    void borrarPorGetYaNoFunciona() throws Exception {
        Sesion ana = entrarComo(Perfil.APRENDIZ);
        Equipo equipo = equipoRepository.save(new Equipo("Portatil de Ana", null, "Portátil", ana.usuario().getId()));

        mvc.perform(con(ana, get("/api/equipos/" + equipo.getId()))).andExpect(status().isMethodNotAllowed());
        assertThat(equipoRepository.existsById(equipo.getId())).isTrue();
    }
}
