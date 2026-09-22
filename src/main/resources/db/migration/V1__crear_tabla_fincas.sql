CREATE TABLE fincas (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(100)     NOT NULL,
    propietario VARCHAR(100)     NOT NULL,
    vereda      VARCHAR(100)     NOT NULL,
    municipio   VARCHAR(100)     NOT NULL,
    hectareas   DOUBLE PRECISION NOT NULL CHECK (hectareas > 0)
);

CREATE INDEX idx_fincas_municipio ON fincas (municipio);
