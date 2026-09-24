package co.sena.adso.porteria.modulos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.ObjetoExterno;
import co.sena.adso.porteria.entity.Rol;
import co.sena.adso.porteria.entity.Vehiculo;
import co.sena.adso.porteria.entity.Visitante;
import co.sena.adso.porteria.repository.ObjetoExternoRepository;
import co.sena.adso.porteria.repository.VehiculoRepository;
import co.sena.adso.porteria.repository.VisitanteRepository;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

// Portería 2: tests/modulos/test_pases.py
class PasesTest extends PruebaIntegracion {

    @Autowired
    private VisitanteRepository visitanteRepository;

    @Autowired
    private VehiculoRepository vehiculoRepository;

    @Autowired
    private ObjetoExternoRepository objetoRepository;

    private ResultActions enviar(Sesion sesion, String ruta, Object cuerpo) throws Exception {
        return mvc.perform(conJson(sesion, post("/api/porteria/pases/" + ruta), cuerpo));
    }

    private ResultActions movimiento(Sesion sesion, String tipoEntidad, Long id, String tipo) throws Exception {
        return mvc.perform(conJson(sesion, post("/api/porteria/movimientos"),
                Map.of("tipoEntidad", tipoEntidad, "entidadId", id, "tipo", tipo)));
    }

    private ObjetoExterno crearObjeto(String descripcion) {
        return objetoRepository.save(new ObjetoExterno(descripcion, "SN-1", "X", null, LocalDateTime.now(), null));
    }

    private ObjetoExterno objeto(Long id) {
        return objetoRepository.findById(id).orElseThrow();
    }

    private long accesos(Long id, String tipoReferencia) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM accesos WHERE referencia_id = ? AND tipo_referencia = ?",
                Long.class, id, tipoReferencia);
    }

    private long entradas(Long id, String tipoReferencia) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM accesos WHERE referencia_id = ? AND tipo_referencia = ? "
                + "AND tipo = 'Entrada'", Long.class, id, tipoReferencia);
    }

    private long inconsistencias() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM auditoria WHERE accion = 'Inconsistencia de acceso detectada'",
                Long.class);
    }

    private Visitante visitante() {
        return visitanteRepository.save(new Visitante("Visita", "V1", null, LocalDateTime.now()));
    }

    private Vehiculo vehiculo() {
        return vehiculoRepository.save(new Vehiculo("XYZ999", "Externo", null, null, LocalDateTime.now()));
    }

    @Test
    void celadorCreaVisitante() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);

        enviar(celador, "visitantes", Map.of("nombre", "Maria Gomez", "documento", "9988776", "motivo", "Reunion"))
                .andExpect(status().isOk());

        Visitante visitante = visitanteRepository.findByDocumento("9988776").orElseThrow();
        assertThat(visitante.getNombre()).isEqualTo("Maria Gomez");
        assertThat(visitante.getCodigo()).isEqualTo("SENA-VISIT:9988776");
        assertThat(visitante.isActivo()).isTrue();
    }

    @Test
    void aprendizNoCreaVisitante() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);

        enviar(aprendiz, "visitantes", Map.of("nombre", "Maria Gomez", "documento", "9988776"))
                .andExpect(status().isForbidden());
        assertThat(visitanteRepository.findByDocumento("9988776")).isEmpty();
    }

    @Test
    void sinDocumentoNoCreaVisitante() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);

        enviar(celador, "visitantes", Map.of("nombre", "Maria Gomez"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
        assertThat(visitanteRepository.count()).isZero();
    }

    @Test
    void visitanteExistenteSeReactivaEnVezDeDuplicar() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);

        enviar(celador, "visitantes", Map.of("nombre", "Maria Gomez", "documento", "9988776", "motivo", "Reunion"));
        jdbc.update("UPDATE visitantes SET activo = false WHERE documento = '9988776'");
        enviar(celador, "visitantes", Map.of("nombre", "Maria Gomez R.", "documento", "9988776", "motivo", "Entrega"));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM visitantes WHERE documento = '9988776'", Long.class))
                .isEqualTo(1);
        Visitante actualizado = visitanteRepository.findByDocumento("9988776").orElseThrow();
        assertThat(actualizado.getNombre()).isEqualTo("Maria Gomez R.");
        assertThat(actualizado.isActivo()).isTrue();
    }

    @Test
    void celadorCreaVehiculo() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);

        enviar(celador, "vehiculos", Map.of("placa", "abc123", "tipo", "Externo", "propietario", "Juan"))
                .andExpect(status().isOk());

        assertThat(vehiculoRepository.findByPlaca("ABC123").orElseThrow().getCodigo()).isEqualTo("SENA-VEH-E:ABC123");
    }

    @Test
    void vehiculoSenaUsaPrefijoDistinto() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);

        enviar(celador, "vehiculos", Map.of("placa", "sen001", "tipo", "SENA"));

        assertThat(vehiculoRepository.findByPlaca("SEN001").orElseThrow().getCodigo()).isEqualTo("SENA-VEH-S:SEN001");
    }

    @Test
    void placaVaciaNoCreaVehiculo() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);

        enviar(celador, "vehiculos", Map.of("tipo", "Externo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
        assertThat(vehiculoRepository.count()).isZero();
    }

    // Se manda un cuerpo válido: sin tipo, la validación del JSON respondería 400 antes de revisar el permiso
    @Test
    void aprendizNoCreaVehiculo() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);

        enviar(aprendiz, "vehiculos", Map.of("placa", "ABC123", "tipo", "Externo")).andExpect(status().isForbidden());
        assertThat(vehiculoRepository.count()).isZero();
    }

    @Test
    void celadorCreaObjetoConSerial() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);

        enviar(celador, "objetos", Map.of("descripcion", "Camara fotografica", "serial", "SN-CAM-1",
                "propietario", "Canal Regional")).andExpect(status().isOk());

        assertThat(objetoRepository.findBySerial("SN-CAM-1").orElseThrow().getCodigo()).isEqualTo("SENA-OBJ:SN-CAM-1");
    }

    @Test
    void objetoSinSerialGeneraUno() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);

        enviar(celador, "objetos", Map.of("descripcion", "Caja sin marcar")).andExpect(status().isOk());

        ObjetoExterno objeto = objetoRepository.findAll().stream()
                .filter(o -> o.getDescripcion().equals("Caja sin marcar")).findFirst().orElseThrow();
        assertThat(objeto.getSerial()).startsWith("SN-");
    }

    @Test
    void descripcionVaciaNoCreaObjeto() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);

        enviar(celador, "objetos", Map.of("serial", "X1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
        assertThat(objetoRepository.count()).isZero();
    }

    @Test
    void aprendizNoCreaObjeto() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);

        enviar(aprendiz, "objetos", Map.of("descripcion", "Camara fotografica")).andExpect(status().isForbidden());
        assertThat(objetoRepository.count()).isZero();
    }

    // La API no tiene formulario de edición: el cliente lo llena con el objeto que trae el listado de pases
    @Test
    void celadorVeFormularioDeEdicion() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        ObjetoExterno objeto = crearObjeto("Camara");

        mvc.perform(con(celador, get("/api/porteria/pases")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objetos[?(@.id == " + objeto.getId() + ")].descripcion").value("Camara"));
    }

    @Test
    void editarObjetoInexistenteDa404() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);

        mvc.perform(conJson(celador, put("/api/porteria/pases/objetos/999999"), Map.of("descripcion", "Camara")))
                .andExpect(status().isNotFound());
    }

    @Test
    void aprendizNoVeFormularioDeEdicion() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        crearObjeto("Camara");

        mvc.perform(con(aprendiz, get("/api/porteria/pases"))).andExpect(status().isForbidden());
    }

    @Test
    void celadorActualizaObjeto() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        ObjetoExterno objeto = crearObjeto("Camara");

        mvc.perform(conJson(celador, put("/api/porteria/pases/objetos/" + objeto.getId()), Map.of(
                        "descripcion", "Camara actualizada", "serial", "SN-1",
                        "propietario", "Nuevo dueño", "motivo", "Actualizado")))
                .andExpect(status().isOk());

        assertThat(objeto(objeto.getId()).getDescripcion()).isEqualTo("Camara actualizada");
        assertThat(objeto(objeto.getId()).getPropietario()).isEqualTo("Nuevo dueño");
    }

    @Test
    void aprendizNoActualizaObjeto() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        ObjetoExterno objeto = crearObjeto("Camara");

        mvc.perform(conJson(aprendiz, put("/api/porteria/pases/objetos/" + objeto.getId()),
                        Map.of("descripcion", "Hackeado")))
                .andExpect(status().isForbidden());

        assertThat(objeto(objeto.getId()).getDescripcion()).isNotEqualTo("Hackeado");
    }

    @Test
    void celadorEliminaObjeto() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        ObjetoExterno objeto = crearObjeto("Camara");

        mvc.perform(con(celador, post("/api/porteria/pases/objetos/" + objeto.getId() + "/desactivar")))
                .andExpect(status().isOk());

        assertThat(objeto(objeto.getId()).isActivo()).isFalse();
    }

    @Test
    void aprendizNoEliminaObjeto() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        ObjetoExterno objeto = crearObjeto("Camara");

        mvc.perform(con(aprendiz, post("/api/porteria/pases/objetos/" + objeto.getId() + "/desactivar")))
                .andExpect(status().isForbidden());

        assertThat(objeto(objeto.getId()).isActivo()).isTrue();
    }

    @Test
    void entradaDeVisitanteSeRegistra() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        Visitante visitante = visitante();

        movimiento(celador, "Visitante", visitante.getId(), "Entrada").andExpect(status().isCreated());
        assertThat(entradas(visitante.getId(), "Visitante")).isEqualTo(1);
    }

    @Test
    void dobleEntradaDeVisitanteSeRechazaYAudita() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        Visitante visitante = visitante();

        movimiento(celador, "Visitante", visitante.getId(), "Entrada");
        movimiento(celador, "Visitante", visitante.getId(), "Entrada").andExpect(status().isConflict());

        assertThat(entradas(visitante.getId(), "Visitante")).isEqualTo(1);
        assertThat(inconsistencias()).isEqualTo(1);
    }

    @Test
    void salidaDeVisitanteSinEntradaSeRechaza() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        Visitante visitante = visitante();

        movimiento(celador, "Visitante", visitante.getId(), "Salida").andExpect(status().isConflict());
        assertThat(accesos(visitante.getId(), "Visitante")).isZero();
    }

    @Test
    void entradaDeVehiculoSeRegistra() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        Vehiculo vehiculo = vehiculo();

        movimiento(celador, "Vehiculo", vehiculo.getId(), "Entrada").andExpect(status().isCreated());
        assertThat(entradas(vehiculo.getId(), "Vehiculo")).isEqualTo(1);
    }

    @Test
    void dobleEntradaDeVehiculoSeRechazaYAudita() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        Vehiculo vehiculo = vehiculo();

        movimiento(celador, "Vehiculo", vehiculo.getId(), "Entrada");
        movimiento(celador, "Vehiculo", vehiculo.getId(), "Entrada").andExpect(status().isConflict());

        assertThat(entradas(vehiculo.getId(), "Vehiculo")).isEqualTo(1);
        assertThat(inconsistencias()).isEqualTo(1);
    }

    @Test
    void salidaDeVehiculoSinEntradaSeRechaza() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        Vehiculo vehiculo = vehiculo();

        movimiento(celador, "Vehiculo", vehiculo.getId(), "Salida").andExpect(status().isConflict());
        assertThat(accesos(vehiculo.getId(), "Vehiculo")).isZero();
    }

    @Test
    void entradaYSalidaCompletaDeVisitante() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        Visitante visitante = visitante();

        movimiento(celador, "Visitante", visitante.getId(), "Entrada");
        movimiento(celador, "Visitante", visitante.getId(), "Salida").andExpect(status().isCreated());
        assertThat(accesos(visitante.getId(), "Visitante")).isEqualTo(2);
    }

    @Test
    void unCeladorNoDeberiaPoderEditarElPaseCreadoPorOtro() throws Exception {
        Sesion celador1 = entrarComo(Perfil.CELADOR);
        crearUsuario("celador2@sena.edu.co", "Celador", Rol.USUARIO, "888888", u -> { });
        Sesion celador2 = new Sesion(null, iniciarSesion("celador2@sena.edu.co", CLAVE));
        long id = leer(enviar(celador1, "objetos", Map.of("descripcion", "Pase de celador 1", "serial", "SN-1"))
                .andExpect(status().isOk()).andReturn()).get("id").asLong();

        mvc.perform(conJson(celador2, put("/api/porteria/pases/objetos/" + id),
                        Map.of("descripcion", "Modificado por celador 2", "serial", "SN-1")))
                .andExpect(status().isForbidden());

        assertThat(objeto(id).getDescripcion()).isEqualTo("Pase de celador 1");
    }
}
