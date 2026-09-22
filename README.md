# API de Fincas y Cultivos

Backend en Spring Boot 3 para registrar fincas, cultivos y lo que se siembra en cada finca.
Proyecto de la fase 2 de ADSO (SENA), hecho siguiendo la guía de Spring Boot módulo por módulo.

## Qué se necesita

- JDK 21 o superior
- PostgreSQL 16 en el puerto 5434
- NetBeans 25 (o cualquier IDE con soporte Maven)

## Cómo ponerlo a correr

1. Crear la base de datos:

   ```
   psql -h localhost -p 5434 -U postgres -c "CREATE DATABASE spring_fincas;"
   ```

2. Copiar `.env.example` como `.env` y llenar `DB_PASSWORD` y `JWT_SECRET`.
3. Abrir la carpeta en NetBeans (File > Open Project) y darle Run. También funciona por consola:

   ```
   .\mvnw.cmd spring-boot:run
   ```

Al arrancar, Flyway crea las tablas y carga unos datos de ejemplo.

- API: http://localhost:31026/api/hello
- Swagger: http://localhost:31026/swagger-ui.html

## Estructura

```
src/main/java/co/sena/adso/fincasapi
├── controller      rutas HTTP
├── service         reglas del negocio
├── repository      consultas JPA
├── entity          tablas
├── dto             lo que entra y sale en JSON
├── exception       errores 400, 404 y 422
├── config          seguridad, CORS y Swagger
├── enums           temporada, estado y rol
└── specification   filtros dinámicos de búsqueda
```

## Rutas

| Método | Ruta | Respuesta |
|---|---|---|
| GET | /api/fincas | 200 |
| GET | /api/fincas/{id} | 200 / 404 |
| POST | /api/fincas | 201 / 400 |
| PUT | /api/fincas/{id} | 200 / 400 / 404 |
| DELETE | /api/fincas/{id} | 204 / 404 |
| GET | /api/fincas/paginado?page=0&size=10&sort=nombre,asc | 200 |
| GET | /api/fincas/buscar?municipio=&propietario=&hectareasMin= | 200 |
| GET, POST, PUT, DELETE | /api/cultivos | igual que fincas |
| GET | /api/finca-cultivos | 200 |
| GET | /api/finca-cultivos/finca/{fincaId} | 200 / 404 |
| POST, PUT | /api/finca-cultivos | 201 / 400 / 404 / 422 |
| POST | /api/auth/login | 200 / 401 |

La regla de negocio (422): lo que se siembra en cultivos activos no puede superar las hectáreas de la finca.

## Seguridad

En desarrollo (`application-dev.properties`) está `app.security.enabled=false` para probar sin token.
En producción queda en `true` y las rutas piden `Authorization: Bearer <token>`.

Para practicar el login en local hay un usuario de prueba: `instructor@adso.co` / `Adso2026*`.
Solo sirve para el entorno local; en otro ambiente se debe cambiar.

## Pruebas

```
.\mvnw.cmd test
```

Hay pruebas unitarias del Service con Mockito y pruebas del Controller con MockMvc.
El archivo `requests.http` tiene las peticiones de verificación de la guía en orden.
