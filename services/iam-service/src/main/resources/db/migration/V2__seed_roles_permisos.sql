INSERT INTO roles(nombre,descripcion) VALUES
('USER','Usuario del sistema'),
('ADMIN','Administrador del sistema')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO permisos(nombre,descripcion) VALUES
('SCORE_CREATE','Registrar una solicitud de scoring'),
('SCORE_READ','Consultar solicitudes/resultados propios'),
('SCORE_ADMIN','Administrar solicitudes de scoring'),
('USER_ADMIN','Administrar usuarios y roles')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO rol_permiso(rol_id,permiso_id)
SELECT r.rol_id,p.permiso_id FROM roles r CROSS JOIN permisos p
WHERE r.nombre='USER' AND p.nombre IN ('SCORE_CREATE','SCORE_READ')
ON CONFLICT DO NOTHING;

INSERT INTO rol_permiso(rol_id,permiso_id)
SELECT r.rol_id,p.permiso_id FROM roles r CROSS JOIN permisos p
WHERE r.nombre='ADMIN'
ON CONFLICT DO NOTHING;
