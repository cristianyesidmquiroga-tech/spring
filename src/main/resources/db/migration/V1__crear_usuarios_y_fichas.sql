CREATE TABLE roles (
    id     BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE fichas (
    id                 BIGSERIAL PRIMARY KEY,
    numero             VARCHAR(20)  NOT NULL UNIQUE,
    programa           VARCHAR(150) NOT NULL,
    fecha_finalizacion DATE,
    activa             BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE usuarios (
    id                      BIGSERIAL PRIMARY KEY,
    nombre                  VARCHAR(100) NOT NULL,
    nombres                 VARCHAR(100),
    apellidos               VARCHAR(100),
    correo                  VARCHAR(100) NOT NULL UNIQUE,
    contrasena              VARCHAR(100) NOT NULL,
    rol_id                  BIGINT       NOT NULL REFERENCES roles (id),
    cargo                   VARCHAR(50),
    tipo_documento          VARCHAR(5)   NOT NULL DEFAULT 'CC',
    documento               VARCHAR(20) UNIQUE,
    programa                VARCHAR(100),
    ficha                   VARCHAR(20),
    ficha_id                BIGINT REFERENCES fichas (id),
    horario                 VARCHAR(20),
    tipo_sangre             VARCHAR(5),
    perfil_completo         BOOLEAN      NOT NULL DEFAULT FALSE,
    correo_verificado       BOOLEAN      NOT NULL DEFAULT FALSE,
    debe_cambiar_contrasena BOOLEAN      NOT NULL DEFAULT FALSE,
    intentos_fallidos       INTEGER      NOT NULL DEFAULT 0,
    bloqueado_hasta         TIMESTAMP,
    session_token           VARCHAR(64),
    foto                    VARCHAR(255),
    foto_estado             VARCHAR(20)  NOT NULL DEFAULT 'sin_foto',
    foto_motivo             TEXT,
    foto_revisada_por       BIGINT,
    foto_fecha_revision     TIMESTAMP,
    foto_fecha_subida       TIMESTAMP
);

CREATE INDEX idx_usuarios_ficha ON usuarios (ficha_id);

CREATE TABLE auditoria (
    id             BIGSERIAL PRIMARY KEY,
    usuario_id     BIGINT REFERENCES usuarios (id) ON DELETE SET NULL,
    nombre_usuario VARCHAR(100) NOT NULL,
    tabla_afectada VARCHAR(100) NOT NULL,
    registro_id    BIGINT       NOT NULL,
    accion         VARCHAR(255) NOT NULL,
    autorizado_por VARCHAR(100),
    motivo         TEXT,
    detalles       TEXT,
    fecha          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auditoria_fecha ON auditoria (fecha);

INSERT INTO roles (nombre) VALUES ('Admin'), ('Usuario'), ('Trabajador');

INSERT INTO fichas (numero, programa, fecha_finalizacion) VALUES
    ('2977385', 'Análisis y Desarrollo de Software', '2027-06-30'),
    ('3235642', 'Análisis y Desarrollo de Software', '2027-12-15'),
    ('2890114', 'Gestión Contable y de Información Financiera', '2026-12-10');
