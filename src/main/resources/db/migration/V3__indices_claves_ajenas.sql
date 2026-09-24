-- Índices sobre claves ajenas: sin ellos borrar un usuario o un punto recorre la tabla completa
CREATE INDEX idx_accesos_punto ON accesos (punto_id);
CREATE INDEX idx_accesos_operador ON accesos (operador_id);
CREATE INDEX idx_auditoria_usuario ON auditoria (usuario_id);
