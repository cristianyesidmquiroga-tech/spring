INSERT INTO fincas (nombre, propietario, vereda, municipio, hectareas) VALUES
    ('El Recreo',       'Luis Ardila',   'Alto Jordán', 'Vélez',           12.5),
    ('Villa Esperanza', 'Marta Rueda',   'La Laja',     'Guavatá',          8.0),
    ('Los Naranjos',    'Jorge Pinzón',  'Cuchilla',    'Barbosa',         20.0),
    ('San Isidro',      'Claudia Mejía', 'El Tablón',   'Puente Nacional',  4.2);

INSERT INTO cultivos (nombre, tipo, ciclo_dias) VALUES
    ('Guayaba',       'permanente',  540),
    ('Caña panelera', 'permanente',  450),
    ('Maíz',          'transitorio', 120),
    ('Fríjol',        'transitorio',  90),
    ('Café',          'permanente',  730);

INSERT INTO finca_cultivo (finca_id, cultivo_id, area_sembrada_ha, fecha_siembra, temporada, estado) VALUES
    (1, 3, 2.0, '2026-02-15', 'INVIERNO',  'COSECHADO'),
    (2, 2, 5.5, '2025-08-20', 'VERANO',    'ACTIVO'),
    (3, 1, 3.0, '2025-10-01', 'OTONO',     'ACTIVO'),
    (3, 5, 9.0, '2024-04-10', 'PRIMAVERA', 'ACTIVO');

-- Usuario de práctica para probar el login en local (contraseña: Adso2026*)
INSERT INTO usuarios (nombre, email, password_hash, rol) VALUES
    ('Instructor ADSO', 'instructor@adso.co', '$2a$10$Ykw73efybYvQuEvKIdjrtejABwoWVizUx0vtvZRDYZnJw4j.qcXP.', 'ADMIN');
