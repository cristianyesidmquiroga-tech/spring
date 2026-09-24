-- La asistencia es del aprendiz: se borra con él; si se borra el instructor, el registro queda sin instructor
CREATE TABLE asistencia_clases (
    id            BIGSERIAL PRIMARY KEY,
    instructor_id BIGINT REFERENCES usuarios (id) ON DELETE SET NULL,
    aprendiz_id   BIGINT       NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    ficha         VARCHAR(20)  NOT NULL,
    fecha         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    presente      BOOLEAN      NOT NULL DEFAULT FALSE,
    evaluacion    VARCHAR(255)
);

CREATE INDEX idx_asistencia_instructor ON asistencia_clases (instructor_id);
CREATE INDEX idx_asistencia_aprendiz ON asistencia_clases (aprendiz_id);
CREATE INDEX idx_asistencia_ficha_fecha ON asistencia_clases (ficha, fecha);

-- El programa se copia de la ficha, que admite hasta 150 caracteres
ALTER TABLE usuarios ALTER COLUMN programa TYPE VARCHAR(150);
