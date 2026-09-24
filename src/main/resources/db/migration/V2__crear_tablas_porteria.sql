CREATE TABLE puntos_acceso (
    id     BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    tipo   VARCHAR(50)  NOT NULL
);

-- Cada entrada o salida de una persona, visitante, vehículo u objeto.
-- referencia_id apunta a la tabla que indique tipo_referencia.
CREATE TABLE accesos (
    id              BIGSERIAL PRIMARY KEY,
    punto_id        BIGINT      NOT NULL REFERENCES puntos_acceso (id),
    referencia_id   BIGINT      NOT NULL,
    tipo_referencia VARCHAR(20) NOT NULL,
    tipo            VARCHAR(10) NOT NULL,
    fecha           TIMESTAMP   NOT NULL,
    equipos_ids     VARCHAR(100),
    operador_id     BIGINT REFERENCES usuarios (id) ON DELETE SET NULL
);

-- La consulta más frecuente es el último movimiento de una entidad al escanearla
CREATE INDEX idx_accesos_referencia ON accesos (referencia_id, tipo_referencia, fecha DESC);
CREATE INDEX idx_accesos_fecha ON accesos (fecha);

CREATE TABLE visitantes (
    id             BIGSERIAL PRIMARY KEY,
    nombre         VARCHAR(100) NOT NULL,
    documento      VARCHAR(20)  NOT NULL UNIQUE,
    motivo         VARCHAR(255),
    codigo         VARCHAR(40)  NOT NULL UNIQUE,
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP    NOT NULL
);

CREATE TABLE vehiculos (
    id             BIGSERIAL PRIMARY KEY,
    placa          VARCHAR(10)  NOT NULL UNIQUE,
    tipo           VARCHAR(10)  NOT NULL,
    propietario    VARCHAR(100),
    motivo         VARCHAR(255),
    codigo         VARCHAR(30)  NOT NULL UNIQUE,
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP    NOT NULL
);

CREATE TABLE objetos_externos (
    id             BIGSERIAL PRIMARY KEY,
    descripcion    VARCHAR(150) NOT NULL,
    serial         VARCHAR(60)  NOT NULL UNIQUE,
    propietario    VARCHAR(100),
    motivo         VARCHAR(255),
    codigo         VARCHAR(80)  NOT NULL UNIQUE,
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP    NOT NULL
);

CREATE TABLE equipos (
    id         BIGSERIAL PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL,
    serial     VARCHAR(60) UNIQUE,
    tipo       VARCHAR(20)  NOT NULL,
    estado     VARCHAR(10)  NOT NULL DEFAULT 'Afuera',
    usuario_id BIGINT       NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE
);

CREATE INDEX idx_equipos_usuario ON equipos (usuario_id);

INSERT INTO puntos_acceso (nombre, tipo) VALUES ('Portería Principal', 'General');
