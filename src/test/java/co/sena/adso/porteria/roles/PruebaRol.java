package co.sena.adso.porteria.roles;

import co.sena.adso.porteria.entity.Rol;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// Portería 2: tests/roles/conftest.py
public abstract class PruebaRol extends PruebaIntegracion {

    // CARPETAS: la carpeta celador corre para celador y porteria
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Perfil.class, names = {"CELADOR", "PORTERIA"})
    public @interface CeladorYPorteria {
    }

    protected Perfil perfil() {
        String paquete = getClass().getPackageName();
        return Perfil.valueOf(paquete.substring(paquete.lastIndexOf('.') + 1).toUpperCase());
    }

    protected Sesion sesion() throws Exception {
        return entrarComo(perfil());
    }

    protected Usuario otroUsuario() {
        return crearUsuario("otra.persona@sena.edu.co", "Aprendiz", Rol.USUARIO, "1000000099",
                u -> u.setNombre("Otra Persona"));
    }
}
