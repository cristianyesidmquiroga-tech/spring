CREATE TABLE cultivos (
    id         BIGSERIAL PRIMARY KEY,
    nombre     VARCHAR(80) NOT NULL UNIQUE,
    tipo       VARCHAR(30) NOT NULL,
    ciclo_dias INTEGER     NOT NULL CHECK (ciclo_dias > 0)
);
