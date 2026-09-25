-- Códigos de 6 dígitos que vencen a los 15 minutos; los intentos se cuentan aparte del login
ALTER TABLE usuarios ADD COLUMN codigo_verificacion VARCHAR(6);
ALTER TABLE usuarios ADD COLUMN codigo_expiracion TIMESTAMP;
ALTER TABLE usuarios ADD COLUMN codigo_recuperacion VARCHAR(6);
ALTER TABLE usuarios ADD COLUMN recuperacion_expiracion TIMESTAMP;
ALTER TABLE usuarios ADD COLUMN intentos_codigo INTEGER NOT NULL DEFAULT 0;
-- Permiso de un solo uso para el último paso de la recuperación, entregado al acertar el código
ALTER TABLE usuarios ADD COLUMN recuperacion_permiso VARCHAR(64);
