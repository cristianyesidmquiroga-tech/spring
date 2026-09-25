-- Un hilo por persona: se borra con ella; si se borra quien escribió, queda su nombre
CREATE TABLE mensajes (
    id             BIGSERIAL PRIMARY KEY,
    usuario_id     BIGINT       NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    autor_id       BIGINT REFERENCES usuarios (id) ON DELETE SET NULL,
    autor_nombre   VARCHAR(100) NOT NULL,
    autor_es_admin BOOLEAN      NOT NULL DEFAULT FALSE,
    texto          TEXT         NOT NULL,
    fecha          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    leido          BOOLEAN      NOT NULL DEFAULT FALSE,
    automatico     BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_mensajes_usuario ON mensajes (usuario_id);
CREATE INDEX idx_mensajes_autor ON mensajes (autor_id);
CREATE INDEX idx_mensajes_fecha ON mensajes (fecha);

ALTER TABLE usuarios ADD COLUMN tutorial_visto BOOLEAN NOT NULL DEFAULT FALSE;
