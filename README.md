# Portería SENA - API

Backend en Spring Boot 3 del sistema de control de acceso del centro: usuarios con rol y cargo,
carnet digital, portería y formación. Es la versión en Java del proyecto Portería 2, organizada
según la guía de Spring Boot (controller, service, repository, entity, dto, exception, config).

El frontend está en el repositorio `react`.

## Qué se necesita

- JDK 21 o superior
- PostgreSQL 16 en el puerto 5434
- NetBeans 25 (o cualquier IDE con soporte Maven)

## Cómo ponerlo a correr

1. Crear la base de datos:

   ```
   psql -h localhost -p 5434 -U postgres -c "CREATE DATABASE porteria_spring;"
   ```

2. Copiar `.env.example` como `.env` y llenar los valores (base de datos, `JWT_SECRET`,
   `ADMIN_EMAIL` y `ADMIN_PASSWORD`).
3. Abrir el proyecto en NetBeans y darle Run, o por consola:

   ```
   .\mvnw.cmd spring-boot:run
   ```

Flyway crea las tablas. Al arrancar se crea el administrador principal (debe cambiar la
contraseña en su primer ingreso) y, si `DEMO_PASSWORD` tiene valor, un usuario de prueba por perfil.

- API: http://localhost:31026/api/hello
- Swagger: http://localhost:31026/swagger-ui.html

## Estructura

```
src/main/java/co/sena/adso/porteria
├── controller      rutas HTTP
├── service         reglas del negocio
├── repository      consultas JPA
├── entity          tablas y permisos por rol y cargo
├── dto             lo que entra y sale en JSON
├── exception       errores 400, 401, 403, 404, 422 y 429
└── config          seguridad JWT, límites de peticiones, Swagger y datos iniciales
```

## Roles, cargos y permisos

El rol (Admin, Usuario, Trabajador) da el nivel de acceso y el cargo (Aprendiz, Instructor,
Celador, Administrativo...) dice qué es la persona en el centro. Los permisos salen de combinar
los dos y están en `entity/Usuario.java`:

| Permiso | Quién lo tiene |
|---|---|
| Administrar | rol Admin |
| Operar portería | Admin, o rol Usuario con cargo Celador/Portería o Administrador |
| Asesorar | Admin, o rol Usuario con cargo Administrador o Administrativo |
| Pasar asistencia | Admin o cualquier cargo Instructor |
| Registrar equipos | Admin, o rol Usuario que no sea celador |

## Rutas de la fase 1

| Método | Ruta | Quién |
|---|---|---|
| POST | /api/auth/login | público |
| POST | /api/auth/logout, /api/auth/renovar | con sesión |
| GET | /api/auth/yo | con sesión |
| POST | /api/auth/cambiar-contrasena | con sesión |
| GET, PUT | /api/perfil | con sesión |
| POST | /api/perfil/foto | con sesión |
| GET | /api/perfil/carnet | con sesión |
| GET | /api/usuarios/{id}/foto | la persona o quien tenga permiso |
| GET | /api/catalogos | con sesión |
| GET, POST, PUT, DELETE | /api/admin/usuarios | Admin |
| POST | /api/admin/usuarios/{id}/desbloquear | Admin |
| GET | /api/admin/fotos/pendientes | Admin |
| POST | /api/admin/fotos/{id}/revision | Admin |

## Seguridad

- Contraseñas con BCrypt, mínimo 8 caracteres combinando letras y números.
- Bloqueo de 10 minutos tras 5 intentos fallidos, y el mismo mensaje si la cuenta no existe.
- Sesión única: iniciar sesión en otro equipo invalida el token anterior.
- El token dura 12 horas para quien opera portería y 10 minutos para el resto (el frontend lo renueva mientras hay actividad).
- La contraseña temporal se debe cambiar antes de usar cualquier otra ruta.
- Límites de peticiones por IP o por usuario (`config/LimitePeticionesFilter.java`).
- Cada campo tiene límite de caracteres y se rechazan campos que no estén en el contrato.
- Las fotos se guardan fuera de carpetas públicas, se re-codifican (sin metadatos GPS) y solo se entregan con permiso.
- Toda acción del administrador queda en la tabla `auditoria`.

## Pruebas

```
.\mvnw.cmd test
```
