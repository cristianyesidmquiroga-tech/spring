CREATE TABLE finca_cultivo (
    id               BIGSERIAL PRIMARY KEY,
    finca_id         BIGINT           NOT NULL REFERENCES fincas (id) ON DELETE CASCADE,
    cultivo_id       BIGINT           NOT NULL REFERENCES cultivos (id),
    area_sembrada_ha DOUBLE PRECISION NOT NULL CHECK (area_sembrada_ha > 0),
    fecha_siembra    DATE             NOT NULL,
    temporada        VARCHAR(20)      NOT NULL,
    estado           VARCHAR(20)      NOT NULL
);

CREATE INDEX idx_finca_cultivo_finca ON finca_cultivo (finca_id);
CREATE INDEX idx_finca_cultivo_cultivo ON finca_cultivo (cultivo_id);
