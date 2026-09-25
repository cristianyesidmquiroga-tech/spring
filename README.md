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
| POST | /api/auth/cambiar-contrasena | solo quien tiene contraseña temporal |
| GET, PUT | /api/perfil | con sesión |
| POST | /api/perfil/foto | con sesión |
| GET | /api/perfil/carnet | con sesión |
| GET | /api/usuarios/{id}/foto | la persona o quien tenga permiso |
| GET | /api/catalogos | con sesión |
| GET, POST, PUT, DELETE | /api/admin/usuarios | Admin |
| POST | /api/admin/usuarios/{id}/desbloquear | Admin |
| GET | /api/admin/fotos?estado=&pagina= (pendiente, aprobada, rechazada, todos; 24 por página) | Admin |
| POST | /api/admin/fotos/{id}/revision | Admin |

## Rutas de la fase 2 (portería)

| Método | Ruta | Quién |
|---|---|---|
| GET | /api/porteria/verificar?codigo= | quien opera portería |
| POST | /api/porteria/movimientos | quien opera portería |
| POST | /api/porteria/incidentes | quien opera portería |
| GET, POST, PUT | /api/porteria/pases/... (visitantes, vehículos, objetos) | quien opera portería; un objeto solo lo edita quien lo creó o un Admin |
| GET | /api/porteria/panel, /panel/accesos, /panel/exportar, /panel/reportes/{cargo o Personal} | quien opera portería |
| GET | /api/historial | cada quien el suyo; portería e instructores el de otros |
| GET, POST, DELETE | /api/equipos | la persona dueña (el celador no registra equipos) |

El escáner reconoce el documento del carnet y los códigos `SENA-VISIT:`, `SENA-VEH-S:`, `SENA-VEH-E:`
y `SENA-OBJ:`. Una doble entrada o una salida sin entrada se rechaza (409) y queda en la auditoría.
A las 00:00:05 se registra la salida de todo lo que quedó adentro y se cierran los pases del día.

## Rutas de la fase 3 (formación)

| Método | Ruta | Quién |
|---|---|---|
| GET, POST, PUT | /api/admin/fichas | Admin |
| PATCH | /api/admin/fichas/{id}/archivar (archiva o reactiva) | Admin |
| GET | /api/admin/clases?ficha= (últimos 100 registros) | Admin |
| GET, POST | /api/asistencia?ficha= | instructores y Admin |
| GET, POST | /api/comunicados | instructores y Admin |
| GET | /api/ambientes, /api/ambientes/{ficha} | Coordinación, Subdirección y Admin |

La lista de clase solo trae aprendices de la ficha que cruzaron portería hoy; guardarla otra vez el mismo
día reemplaza la anterior. Los comunicados salen por correo (variables `SMTP_*`); sin `SMTP_HOST` la API
responde a quién no se le pudo enviar.

## Rutas de la fase 4 (soporte)

| Método | Ruta | Quién |
|---|---|---|
| GET, POST | /api/mensajes | cada quien su propio hilo |
| GET | /api/avisos (mensajes sin leer, hilos pendientes, fotos por revisar) | con sesión |
| GET | /api/bandeja, /api/bandeja/{usuarioId} | Admin y cargos Administrador o Administrativo |
| POST | /api/bandeja/{usuarioId} | los mismos |
| GET | /api/ayuda | con sesión |
| POST | /api/ayuda/contacto | con sesión (5 por hora) |
| GET | /api/tutorial | con sesión |
| POST | /api/tutorial/completar | con sesión, siempre sobre la propia cuenta |

Aprobar o rechazar una foto deja un mensaje automático en el hilo de la persona. El asesor ve solo los
últimos cuatro dígitos del documento y nunca el correo.

## Rutas de la fase 5 (cuentas y operación)

| Método | Ruta | Quién |
|---|---|---|
| GET | /api/auth/captcha | público (desafío anti-bot o `{"activo": false}`) |
| POST | /api/auth/registro | público; solo un Admin con sesión elige cargo |
| POST | /api/auth/verificacion, /api/auth/verificacion/reenviar | quien tiene el correo sin verificar |
| POST | /api/auth/recuperacion, /recuperacion/verificar, /recuperacion/cambiar | público |
| GET | /api/politica-privacidad | público |
| GET | /api/admin/auditoria?pagina= | Admin |
| GET | /api/admin/respaldos, /api/admin/respaldos/{archivo} | Admin |
| POST | /api/admin/usuarios/importar (multipart, campo `archivo`) | Admin |
| GET | /api/admin/correos/fallidos | Admin |

- Una cuenta con el correo sin verificar solo puede usar `/api/auth/**` (la API responde 403 `CORREO_SIN_VERIFICAR`).
- Los códigos de verificación y recuperación vencen a los 15 minutos y se anulan tras 5 intentos; fallarlos no
  bloquea el login. La recuperación responde lo mismo exista o no la cuenta.
- El correo sale en segundo plano: los fallos definitivos (credenciales, destinatario inexistente, 5xx) no se
  reintentan; los pasajeros se reintentan dos veces. En el log las direcciones van ofuscadas.
- El respaldo del mes anterior se genera el día 1 a las 00:00:10 en `CARPETA_RESPALDOS`; solo borra datos con
  `PURGAR_TRAS_RESPALDO=true` y después de comprobar que el archivo se puede releer.
- La importación nunca crea administradores, degrada cargos inválidos a Aprendiz y da a cada fila una contraseña
  temporal distinta que llega por correo.

## Seguridad

- Contraseñas con BCrypt, mínimo 8 caracteres combinando letras y números.
- Bloqueo de 10 minutos tras 5 intentos fallidos, y el mismo mensaje si la cuenta no existe.
- Sesión única: iniciar sesión en otro equipo invalida el token anterior.
- El token dura 12 horas para quien opera portería y 10 minutos para el resto (el frontend lo renueva mientras hay actividad).
- La contraseña temporal se debe cambiar antes de usar cualquier otra ruta.
- Límites de peticiones por IP real (detrás del proxy) o por usuario, con aviso y `Retry-After` (`config/LimitePeticionesFilter.java`).
- Cada campo tiene límite de caracteres, se rechazan campos que no estén en el contrato y se quitan etiquetas HTML del texto libre.
- Las fotos (JPG, PNG o WEBP) se guardan fuera de carpetas públicas, se re-codifican a 512 px sin metadatos GPS y solo se entregan con permiso. La foto rechazada se borra.
- Toda acción del administrador queda en la tabla `auditoria`.

## Pruebas

Migradas una a una desde las pruebas pytest de Portería 2 a JUnit 5, con la misma división en carpetas
(`modulos`, `roles`, `vistas`). La equivalencia completa está en `docs/PRUEBAS.md`. Necesitan Docker
encendido (Testcontainers levanta un PostgreSQL 16 desechable) y GitHub Actions las corre en cada push.

```
.\mvnw.cmd verify
```

El informe de cobertura queda en `target/site/jacoco/index.html`.
