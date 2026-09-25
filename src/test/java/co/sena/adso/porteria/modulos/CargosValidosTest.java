package co.sena.adso.porteria.modulos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.service.CarnetService;
import co.sena.adso.porteria.service.CodigoBarrasService;
import co.sena.adso.porteria.service.CorreoService;
import co.sena.adso.porteria.soporte.Excel;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

// Portería 2: tests/modulos/test_cargos_validos.py
class CargosValidosTest extends PruebaIntegracion {

    // Como el monkeypatch de enviar_correo: cuenta los correos sin enviarlos
    @MockBean
    private CorreoService correoService;

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
        verify(correoService, times(4)).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void registroConSesionAdminTambienValidaElCargo() throws Exception {
        Sesion admin = iniciarSesionAdmin();
        Map<String, Object> datos = datosRegistro();
        datos.put("nombre", "Cargo Invalido");
        datos.put("correo", "registro-invalido@sena.edu.co");
        datos.put("documento", "123458");
        datos.put("cargo", "Cargo Inventado");
        mvc.perform(conJson(admin, post("/api/auth/registro"), datos)).andExpect(status().isBadRequest());
        assertThat(usuarioRepository.existsByCorreoIgnoreCase("registro-invalido@sena.edu.co")).isFalse();
    }

    @Test
    void importacionAceptaLosCargosNuevosYAvisaLosInvalidos() throws Exception {
        Sesion admin = iniciarSesionAdmin();
        List<Map<String, Object>> filas = new ArrayList<>();
        NUEVOS_CARGOS.keySet().forEach(cargo -> filas.add(Map.of("Nombre", cargo,
                "Correo", cargo.toLowerCase() + "@sena.edu.co", "Cargo", cargo)));
        filas.add(Map.of("Nombre", "No Valido", "Correo", "no-valido@sena.edu.co", "Cargo", "Cargo Inventado"));

        JsonNode detalles = leer(mvc.perform(con(admin, multipart("/api/admin/usuarios/importar")
                .file(Excel.deFilas(filas, "cargos.xlsx")))).andExpect(status().isOk()).andReturn()).get("detalles");

        assertThat(detalles.get("creados").asInt()).isEqualTo(5);
        List<String> avisos = new ArrayList<>();
        detalles.get("errores").forEach(a -> avisos.add(a.asText().toLowerCase()));
        assertThat(avisos).anyMatch(a -> a.contains("cargo") && a.contains("no válido"));
        NUEVOS_CARGOS.keySet().forEach(cargo -> assertThat(usuarioRepository.findByCargoInOrderByNombre(List.of(cargo))).isNotEmpty());
        assertThat(porCorreo("no-valido@sena.edu.co").getCargo()).isEqualTo("Aprendiz");
    }
}
