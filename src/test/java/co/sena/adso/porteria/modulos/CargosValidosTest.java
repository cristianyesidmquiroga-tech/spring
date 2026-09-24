package co.sena.adso.porteria.modulos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.service.CarnetService;
import co.sena.adso.porteria.service.CodigoBarrasService;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// Portería 2: tests/modulos/test_cargos_validos.py
class CargosValidosTest extends PruebaIntegracion {

    private static final Map<String, String> NUEVOS_CARGOS = new LinkedHashMap<>();

    static {
        NUEVOS_CARGOS.put("Coordinacion", CarnetService.FUNCIONARIO);
        NUEVOS_CARGOS.put("Subdirector", CarnetService.SUBDIRECTOR);
        NUEVOS_CARGOS.put("Contratista", CarnetService.CONTRATISTA);
        NUEVOS_CARGOS.put("Funcionario", CarnetService.FUNCIONARIO);
    }

    private Sesion iniciarSesionAdmin() throws Exception {
        Usuario admin = crearUsuario("admin@sena.edu.co", "Administrador", "Admin", "123456", u -> { });
        return new Sesion(admin, iniciarSesion(admin.getCorreo(), CLAVE));
    }

    private Long rolUsuario() {
        return jdbc.queryForObject("SELECT id FROM roles WHERE nombre = 'Usuario'", Long.class);
    }

    private Usuario primeroConCargo(String cargo) {
        return usuarioRepository.findByCargoInOrderByNombre(List.of(cargo)).get(0);
    }

    @Test
    void losCuatroCargosEstanEnLaListaBlancaYEnElCarnet() {
        CarnetService carnet = new CarnetService("", "Regional Santander", "Centro de Gestión Agroempresarial del Oriente",
            "Aseguradora Aurora", "601-7443718 Op. 1", "100603", new CodigoBarrasService());
        NUEVOS_CARGOS.forEach((cargo, perfil) -> {
            assertThat(Usuario.CARGOS_VALIDOS).contains(cargo);
            assertThat(carnet.perfilDeCargo(cargo)).isEqualTo(perfil);
        });
    }

    @Test
    void elPanelExponeLaListaBlanca() throws Exception {
        Sesion admin = iniciarSesionAdmin();
        JsonNode catalogos = leer(mvc.perform(con(admin, get("/api/catalogos"))).andExpect(status().isOk()).andReturn());
        List<String> cargos = new ArrayList<>();
        catalogos.get("cargos").forEach(c -> cargos.add(c.asText()));
        assertThat(cargos).containsAll(NUEVOS_CARGOS.keySet());
    }

    @Test
    void crearYEditarRechazanCargosFueraDeLaLista() throws Exception {
        Sesion admin = iniciarSesionAdmin();
        mvc.perform(conJson(admin, post("/api/admin/usuarios"), Map.of("nombre", "Cargo Invalido",
                        "correo", "invalido@sena.edu.co", "contrasena", CLAVE, "rolId", rolUsuario(),
                        "cargo", "Cargo Inventado")))
                .andExpect(status().isBadRequest());
        assertThat(usuarioRepository.existsByCorreoIgnoreCase("invalido@sena.edu.co")).isFalse();

        Usuario persona = crearUsuario("persona@sena.edu.co", "123457");
        mvc.perform(conJson(admin, put("/api/admin/usuarios/{id}", persona.getId()), Map.of("nombre", persona.getNombre(),
                        "correo", persona.getCorreo(), "rolId", rolUsuario(), "cargo", "Cargo Inventado")))
                .andExpect(status().isBadRequest());
        assertThat(usuarioRepository.findById(persona.getId()).orElseThrow().getCargo()).isEqualTo("Aprendiz");
    }

    // La aserción de correos enviados de Portería 2 queda para la fase 5 (la API aún no envía correos)
    @Test
    void losCargosNuevosSeCreanConSusPermisos() throws Exception {
        Sesion admin = iniciarSesionAdmin();
        int indice = 0;
        for (String cargo : NUEVOS_CARGOS.keySet()) {
            mvc.perform(conJson(admin, post("/api/admin/usuarios"), Map.of("nombre", cargo,
                            "correo", cargo.toLowerCase() + "@sena.edu.co", "contrasena", CLAVE, "rolId", rolUsuario(),
                            "cargo", cargo, "documento", String.valueOf(1_000_000_000L + indice++))))
                    .andExpect(status().isCreated());
        }

        assertThat(primeroConCargo("Coordinacion").puedeVerAmbientes()).isTrue();
        assertThat(primeroConCargo("Subdirector").puedeVerAmbientes()).isTrue();
        assertThat(primeroConCargo("Contratista").puedeVerAmbientes()).isFalse();
        assertThat(primeroConCargo("Funcionario").puedeVerAmbientes()).isFalse();
    }
}
