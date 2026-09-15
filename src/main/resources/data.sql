-- Seed automático que Spring ejecuta en cada arranque (solo para desarrollo).

-- 1) Cuentas
INSERT INTO account (id, name, active)
VALUES (1, 'net2Rent Demo', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO account (id, name, active)
VALUES (2, 'Otra Empresa', true)
ON CONFLICT (id) DO NOTHING;

-- 2) Usuarios
INSERT INTO app_user (account_id, first_name, last_name, email, password_hash, role, active)
VALUES
  (1, 'Admin',       'Demo', 'admin@net2rent.com',       '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'ADMIN',       true),
  (1, 'Coordinador', 'Demo', 'coordinador@net2rent.com', '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'COORDINATOR', true),
  (1, 'Operario',    'Demo', 'operario@net2rent.com',    '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'OPERATOR',    true),
  (1, 'Inactivo',    'Demo', 'inactivo@net2rent.com',    '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'OPERATOR',    false)
ON CONFLICT (email) DO NOTHING;

INSERT INTO app_user (account_id, first_name, last_name, email, password_hash, role, active)
VALUES
    (2, 'Admin', 'Otra', 'admin@otraempresa.com',
     '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'ADMIN', true)
    ON CONFLICT (email) DO NOTHING;

-- 3) Contador de códigos
INSERT INTO incident_counter (id, account_id, counter_year, last_number)
VALUES
    (1, 1, 2026, 7),
    (2, 2, 2026, 1)
ON CONFLICT (id) DO NOTHING;

-- 4) Alojamiento
INSERT INTO lodging (id, account_id, ref, pin_hash, name, address, active)
VALUES
    (1, 1, 'APT-1001',
 '$2a$12$6a7rJB14vKRzFx/w4kWtLe1/8mp6ByGksfnjxLIHHkyN0XrTXTbTe', 'Piso Centro', 'Calle Mayor 12, 3ºB', true),
(2, 2, 'APT-2001',
 '$2a$12$6a7rJB14vKRzFx/w4kWtLe1/8mp6ByGksfnjxLIHHkyN0XrTXTbTe', 'Piso Playa', 'Paseo Marítimo 25, 1ºA', true)
ON CONFLICT (id) DO NOTHING;

-- 5) Incidencias APT-1001 (account 1)
INSERT INTO incident (id, account_id, lodging_id, code, title, description,
                      category, priority, source, status,
                      guest_first_name, guest_last_name, guest_contact,
                      assignee_id, opened_at, created_at, assigned_at, started_at,
                      resolved_at, closed_at, minutes_spent, resolution_note)
VALUES
    (1, 1, 1, 'INC-2026-000001', 'Aire acondicionado no enfría',
     'El aire acondicionado del salón no enfría aunque está encendido y el termostato marca 22°C.',
     'HVAC', 'HIGH', 'GUEST_PORTAL', 'NEW',
     'María', 'García', 'maria@email.com',
     NULL, '2026-08-21 10:30:00', '2026-08-21 10:30:00', NULL, NULL,
     NULL, NULL, NULL, NULL),

    (2, 1, 1, 'INC-2026-000002', 'Fuga de agua en baño',
     'Hay una fuga de agua pequeña bajo el lavabo del baño principal. El suelo se moja.',
     'PLUMBING', 'NORMAL', 'PHONE', 'IN_PROGRESS',
     'Carlos', 'López', 'carlos@email.com',
     (SELECT id FROM app_user WHERE email = 'operario@net2rent.com'),
     '2026-08-22 09:15:00', '2026-08-22 09:15:00', '2026-08-22 09:45:00', '2026-08-22 10:00:00',
     NULL, NULL, NULL, NULL),

    (3, 1, 1, 'INC-2026-000003', 'Persiana del dormitorio atascada',
     'La persiana eléctrica del dormitorio principal no sube ni baja. Se escucha motor pero no mueve.',
     'OTHER', 'LOW', 'GUEST_PORTAL', 'RESOLVED',
     'Ana', 'Martínez', 'ana@email.com',
     (SELECT id FROM app_user WHERE email = 'operario@net2rent.com'),
     '2026-08-20 14:00:00', '2026-08-20 14:00:00', '2026-08-20 14:30:00', '2026-08-20 15:00:00',
     '2026-08-20 18:00:00', NULL, 45, 'Se ha reiniciado el motor de la persiana y ha vuelto a funcionar correctamente.')
ON CONFLICT (id) DO NOTHING;

-- 6) Incidencia APT-2001 (account 2)
INSERT INTO incident (id, account_id, lodging_id, code, title, description,
                      category, priority, source, status,
                      guest_first_name, guest_last_name, guest_contact,
                      opened_at, created_at, resolved_at, closed_at)
VALUES
(4, 2, 2, 'INC-2026-000099', 'Cerradura de la puerta principal',
     'La cerradura de la puerta principal está difícil de girar con la llave.',
     'LOCKSMITH', 'HIGH', 'GUEST_PORTAL', 'NEW',
     'Pedro', 'Sánchez', 'pedro@email.com',
     '2026-08-23 11:00:00', '2026-08-23 11:00:00',
     NULL, NULL)
ON CONFLICT (id) DO NOTHING;

-- 7) Incidencias adicionales para cubrir el resto de estados (demo)
INSERT INTO incident (id, account_id, lodging_id, code, title, description,
                      category, priority, source, status,
                      guest_first_name, guest_last_name, guest_contact,
                      assignee_id, opened_at, created_at, assigned_at, started_at,
                      resolved_at, closed_at, minutes_spent, resolution_note,
                      pause_reason, rejection_reason)
VALUES
    (5, 1, 1, 'INC-2026-000004', 'Bombilla del pasillo fundida',
     'La bombilla del pasillo de entrada no enciende. Puede ser la bombilla o el interruptor.',
     'ELECTRICITY', 'LOW', 'GUEST_PORTAL', 'CLOSED',
     'Laura', 'Fernández', 'laura@email.com',
     (SELECT id FROM app_user WHERE email = 'operario@net2rent.com'),
     '2026-08-18 09:00:00', '2026-08-18 09:00:00', '2026-08-18 09:30:00', '2026-08-18 10:00:00',
     '2026-08-18 10:20:00', '2026-08-19 08:00:00', 20, 'Se ha sustituido la bombilla fundida por una nueva LED.',
     NULL, NULL),

    (6, 1, 1, 'INC-2026-000005', 'Wifi no funciona en el dormitorio',
     'El router no llega con buena señal al dormitorio principal, el huésped no tiene wifi ahí.',
     'OTHER', 'NORMAL', 'GUEST_PORTAL', 'ASSIGNED',
     'Jorge', 'Ruiz', 'jorge@email.com',
     (SELECT id FROM app_user WHERE email = 'operario@net2rent.com'),
     '2026-08-24 12:00:00', '2026-08-24 12:00:00', '2026-08-24 12:30:00', NULL,
     NULL, NULL, NULL, NULL,
     NULL, NULL),

    (7, 1, 1, 'INC-2026-000006', 'Nevera hace ruido extraño',
     'La nevera de la cocina hace un ruido intermitente, parece el compresor.',
     'APPLIANCES', 'NORMAL', 'PHONE', 'PAUSED',
     'Elena', 'Torres', 'elena@email.com',
     (SELECT id FROM app_user WHERE email = 'operario@net2rent.com'),
     '2026-08-19 08:00:00', '2026-08-19 08:00:00', '2026-08-19 08:30:00', '2026-08-19 09:00:00',
     NULL, NULL, NULL, NULL,
     'Se necesita pieza de recambio, pendiente de pedido al proveedor.', NULL),

    (8, 1, 1, 'INC-2026-000007', 'Solicitud de cambio de muebles',
     'El huésped pide cambiar el sofá del salón porque no le gusta el color.',
     'FURNITURE', 'LOW', 'GUEST_PORTAL', 'REJECTED',
     'Pablo', 'Díaz', 'pablo@email.com',
     NULL, '2026-08-25 16:00:00', '2026-08-25 16:00:00', NULL, NULL,
     NULL, NULL, NULL, NULL,
     NULL, 'No es una incidencia de mantenimiento, es una preferencia estética. Fuera del alcance del servicio.')
ON CONFLICT (id) DO NOTHING;

-- 8) Checklist de incidencias resueltas/cerradas
INSERT INTO incident_checklist_item (id, incident_id, text, position, done, checked_by_id, checked_at)
VALUES
    (1, 3, 'Revisar el motor de la persiana', 0, true,
     (SELECT id FROM app_user WHERE email = 'operario@net2rent.com'), '2026-08-20 17:30:00'),
    (2, 3, 'Reiniciar el mecanismo', 1, true,
     (SELECT id FROM app_user WHERE email = 'operario@net2rent.com'), '2026-08-20 17:50:00'),
    (3, 5, 'Comprobar el interruptor', 0, true,
     (SELECT id FROM app_user WHERE email = 'operario@net2rent.com'), '2026-08-18 10:10:00'),
    (4, 5, 'Sustituir la bombilla', 1, true,
     (SELECT id FROM app_user WHERE email = 'operario@net2rent.com'), '2026-08-18 10:15:00')
ON CONFLICT (id) DO NOTHING;

-- 9) Comentarios
INSERT INTO incident_comment (id, incident_id, author_id, text, created_at)
VALUES
    (1, 5, (SELECT id FROM app_user WHERE email = 'coordinador@net2rent.com'),
     'Asignado al operario, prioridad baja.', '2026-08-18 09:30:00'),
    (2, 5, (SELECT id FROM app_user WHERE email = 'operario@net2rent.com'),
     'Revisado, era la bombilla. Sustituida.', '2026-08-18 10:20:00')
ON CONFLICT (id) DO NOTHING;


-- 10) Historial
INSERT INTO incident_history (id, incident_id, actor_id, event_type, previous_value, new_value, note, created_at)
VALUES
    (1, 1, NULL, 'CREATED', NULL, NULL, NULL, '2026-08-21 10:30:00'),
    (2, 4, NULL, 'CREATED', NULL, NULL, NULL, '2026-08-23 11:00:00'),

    (3, 2, NULL, 'CREATED', NULL, NULL, NULL, '2026-08-22 09:15:00'),
    (4, 2, (SELECT id FROM app_user WHERE email = 'coordinador@net2rent.com'), 'STATUS_CHANGED', 'NEW', 'ASSIGNED', NULL, '2026-08-22 09:45:00'),
    (5, 2, (SELECT id FROM app_user WHERE email = 'operario@net2rent.com'), 'STATUS_CHANGED', 'ASSIGNED', 'IN_PROGRESS', NULL, '2026-08-22 10:00:00'),

    (6, 3, NULL, 'CREATED', NULL, NULL, NULL, '2026-08-20 14:00:00'),
    (7, 3, (SELECT id FROM app_user WHERE email = 'coordinador@net2rent.com'), 'STATUS_CHANGED', 'NEW', 'ASSIGNED', NULL, '2026-08-20 14:30:00'),
    (8, 3, (SELECT id FROM app_user WHERE email = 'operario@net2rent.com'), 'STATUS_CHANGED', 'ASSIGNED', 'IN_PROGRESS', NULL, '2026-08-20 15:00:00'),
    (9, 3, (SELECT id FROM app_user WHERE email = 'operario@net2rent.com'), 'STATUS_CHANGED', 'IN_PROGRESS', 'RESOLVED', 'Persiana reparada, motor reiniciado.', '2026-08-20 18:00:00'),

    (10, 5, NULL, 'CREATED', NULL, NULL, NULL, '2026-08-18 09:00:00'),
    (11, 5, (SELECT id FROM app_user WHERE email = 'coordinador@net2rent.com'), 'STATUS_CHANGED', 'NEW', 'ASSIGNED', 'Asignada al operario.', '2026-08-18 09:30:00'),
    (12, 5, (SELECT id FROM app_user WHERE email = 'operario@net2rent.com'), 'STATUS_CHANGED', 'ASSIGNED', 'IN_PROGRESS', NULL, '2026-08-18 10:00:00'),
    (13, 5, (SELECT id FROM app_user WHERE email = 'operario@net2rent.com'), 'STATUS_CHANGED', 'IN_PROGRESS', 'RESOLVED', NULL, '2026-08-18 10:20:00'),
    (14, 5, (SELECT id FROM app_user WHERE email = 'coordinador@net2rent.com'), 'STATUS_CHANGED', 'RESOLVED', 'CLOSED', 'Confirmado con el huésped que quedó resuelto.', '2026-08-19 08:00:00'),

    (15, 6, NULL, 'CREATED', NULL, NULL, NULL, '2026-08-24 12:00:00'),
    (16, 6, (SELECT id FROM app_user WHERE email = 'coordinador@net2rent.com'), 'STATUS_CHANGED', 'NEW', 'ASSIGNED', NULL, '2026-08-24 12:30:00'),

    (17, 7, NULL, 'CREATED', NULL, NULL, NULL, '2026-08-19 08:00:00'),
    (18, 7, (SELECT id FROM app_user WHERE email = 'coordinador@net2rent.com'), 'STATUS_CHANGED', 'NEW', 'ASSIGNED', NULL, '2026-08-19 08:30:00'),
    (19, 7, (SELECT id FROM app_user WHERE email = 'operario@net2rent.com'), 'STATUS_CHANGED', 'ASSIGNED', 'IN_PROGRESS', NULL, '2026-08-19 09:00:00'),
    (20, 7, (SELECT id FROM app_user WHERE email = 'operario@net2rent.com'), 'STATUS_CHANGED', 'IN_PROGRESS', 'PAUSED', 'Se necesita pieza de recambio.', '2026-08-19 09:15:00'),

    (21, 8, NULL, 'CREATED', NULL, NULL, NULL, '2026-08-25 16:00:00'),
    (22, 8, (SELECT id FROM app_user WHERE email = 'coordinador@net2rent.com'), 'STATUS_CHANGED', 'NEW', 'REJECTED', 'Fuera del alcance del servicio de mantenimiento.', '2026-08-25 16:20:00')
ON CONFLICT (id) DO NOTHING;


-- Resincroniza las secuencias tras insertar IDs explícitos en el seed
SELECT setval(pg_get_serial_sequence('account', 'id'), COALESCE((SELECT MAX(id) FROM account), 1));
SELECT setval(pg_get_serial_sequence('lodging', 'id'), COALESCE((SELECT MAX(id) FROM lodging), 1));
SELECT setval(pg_get_serial_sequence('incident', 'id'), COALESCE((SELECT MAX(id) FROM incident), 1));
SELECT setval(pg_get_serial_sequence('incident_counter', 'id'), COALESCE((SELECT MAX(id) FROM incident_counter), 1));
SELECT setval(pg_get_serial_sequence('incident_checklist_item', 'id'), COALESCE((SELECT MAX(id) FROM incident_checklist_item), 1));
SELECT setval(pg_get_serial_sequence('incident_comment', 'id'), COALESCE((SELECT MAX(id) FROM incident_comment), 1));
SELECT setval(pg_get_serial_sequence('incident_history', 'id'), COALESCE((SELECT MAX(id) FROM incident_history), 1));

-- Por si el contador ya existía de un arranque anterior con un número más bajo
UPDATE incident_counter SET last_number = 7 WHERE account_id = 1 AND counter_year = 2026;