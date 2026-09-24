-- Quién creó el pase: un celador no puede editar ni retirar el de otro
ALTER TABLE objetos_externos ADD COLUMN creado_por BIGINT REFERENCES usuarios (id) ON DELETE SET NULL;
CREATE INDEX idx_objetos_creado_por ON objetos_externos (creado_por);
