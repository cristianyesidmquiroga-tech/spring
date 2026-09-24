package co.sena.adso.porteria.soporte;

import co.sena.adso.porteria.entity.Rol;

// Los mismos 12 perfiles de tests/conftest.py de Portería 2
public enum Perfil {
    ADMIN("admin", Rol.ADMIN, "Administrador", "Admin de Prueba", "1000000001"),
    ADMINISTRADOR("administrador", Rol.USUARIO, "Administrador", "Administrador de Prueba", "1000000002"),
    ADMINISTRATIVO("administrativo", Rol.USUARIO, "Administrativo", "Administrativo de Prueba", "1000000003"),
    APRENDIZ("aprendiz", Rol.USUARIO, "Aprendiz", "Aprendiz de Prueba", "1000000004"),
    CELADOR("celador", Rol.USUARIO, "Celador", "Celador de Prueba", "1000000005"),
    CONTRATISTA("contratista", Rol.USUARIO, "Contratista", "Contratista de Prueba", "1000000006"),
    COORDINACION("coordinacion", Rol.USUARIO, "Coordinacion", "Coordinacion de Prueba", "1000000007"),
    FUNCIONARIO("funcionario", Rol.USUARIO, "Funcionario", "Funcionario de Prueba", "1000000008"),
    INSTRUCTOR("instructor", Rol.USUARIO, "Instructor", "Instructor de Prueba", "1000000009"),
    PORTERIA("porteria", Rol.USUARIO, "Portería", "Porteria de Prueba", "1000000010"),
    SUBDIRECTOR("subdirector", Rol.USUARIO, "Subdirector", "Subdirector de Prueba", "1000000011"),
    TRABAJADOR("trabajador", Rol.TRABAJADOR, "Funcionario", "Trabajador de Prueba", "1000000012");

    public final String clave;
    public final String rol;
    public final String cargo;
    public final String nombre;
    public final String documento;

    Perfil(String clave, String rol, String cargo, String nombre, String documento) {
        this.clave = clave;
        this.rol = rol;
        this.cargo = cargo;
        this.nombre = nombre;
        this.documento = documento;
    }

    @Override
    public String toString() {
        return clave;
    }

    public String correo() {
        return clave + "@sena.edu.co";
    }
}
