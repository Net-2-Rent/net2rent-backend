-- net2Rent development seed (runs on every application startup).

-- 1) Accounts
INSERT INTO account (id, name, active) VALUES (1, 'net2Rent Demo', true) ON CONFLICT (id) DO NOTHING;
INSERT INTO account (id, name, active) VALUES (2, 'Otra Empresa', true)   ON CONFLICT (id) DO NOTHING;

-- 2) Users (all share the same test password)
--    Generic ones (admin@/coordinador@/operario@) + named users.
INSERT INTO app_user (account_id, first_name, last_name, email, password_hash, role, active)
VALUES
    (1, 'Admin',         'Demo',    'admin@net2rent.com',        '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'ADMIN',       true),
    (1, 'Coordinador',   'Demo',    'coordinador@net2rent.com',  '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'COORDINATOR', true),
    (1, 'Maria',         'Martin',  'maria.martin@net2rent.com', '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'COORDINATOR', true),
    (1, 'Operario',      'Demo',    'operario@net2rent.com',     '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'OPERATOR',    true),
    (1, 'Jose Antonio',  'Perez',   'jose.perez@net2rent.com',   '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'OPERATOR',    true),
    (1, 'Marc',          'Vidal',   'marc.vidal@net2rent.com',   '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'OPERATOR',    true),
    (1, 'Laia',          'Serra',   'laia.serra@net2rent.com',   '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'OPERATOR',    true),
    (1, 'Inactivo',      'Demo',    'inactivo@net2rent.com',     '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'OPERATOR',    false),
    (2, 'Admin',         'Otra',    'admin@otraempresa.com',     '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'ADMIN',       true)
    ON CONFLICT (email) DO NOTHING;

-- 3) Incident code counter
INSERT INTO incident_counter (id, account_id, counter_year, last_number)
VALUES (1, 1, 2026, 7), (2, 2, 2026, 99)
    ON CONFLICT (id) DO NOTHING;

-- 4) Lodgings
INSERT INTO lodging (id, account_id, ref, pin_hash, name, address, access_notes, active)
VALUES
    (1, 1, 'APT-1001', '$2a$12$6a7rJB14vKRzFx/w4kWtLe1/8mp6ByGksfnjxLIHHkyN0XrTXTbTe',
     'Piso Centro',   'Calle Mayor 12, 3B',            'Llaves en conserjeria. Timbre 3B.', true),
    (3, 1, 'APT-1002', '$2a$12$6a7rJB14vKRzFx/w4kWtLe1/8mp6ByGksfnjxLIHHkyN0XrTXTbTe',
     'Atico Gotico',  'Carrer del Bisbe 4, Atic',      'Llaves en caja de seguridad junto a la puerta, codigo 1948. Ascensor hasta la 5a planta.', true),
    (4, 1, 'APT-1003', '$2a$12$6a7rJB14vKRzFx/w4kWtLe1/8mp6ByGksfnjxLIHHkyN0XrTXTbTe',
     'Estudio Gracia','Carrer de Verdi 88, 2-1',       'Portero automatico 2A. Parking en Torrent de l Olla, plaza 14.', true),
    (5, 1, 'APT-1004', '$2a$12$6a7rJB14vKRzFx/w4kWtLe1/8mp6ByGksfnjxLIHHkyN0XrTXTbTe',
     'Loft Poblenou', 'Carrer de Pujades 130, 4C',     'Cerradura inteligente, codigo temporal por SMS. Contador de agua en el rellano.', true),
    (2, 2, 'APT-2001', '$2a$12$6a7rJB14vKRzFx/w4kWtLe1/8mp6ByGksfnjxLIHHkyN0XrTXTbTe',
     'Piso Playa',    'Paseo Maritimo 25, 1A',         NULL, true)
    ON CONFLICT (id) DO NOTHING;

-- 5) Incidents (account 1 spread across lodgings and operators)
--    Demo: coordinator = Maria Martin ; main operator = Jose Antonio Perez.
--    Most incidents assigned to Jose Antonio; 1 to Marc Vidal.
INSERT INTO incident (id, account_id, lodging_id, code, title, description,
                      category, priority, source, status,
                      guest_first_name, guest_last_name, guest_contact,
                      assignee_id, opened_at, created_at, assigned_at, started_at,
                      resolved_at, closed_at, minutes_spent, resolution_note,
                      pause_reason, rejection_reason)
VALUES
    -- 1) NEW, unassigned (pool)
    (1, 1, 1, 'INC-2026-000001', 'Aire acondicionado no enfria',
     'El aire acondicionado del salon no enfria aunque esta encendido y el termostato marca 22C.',
     'HVAC', 'HIGH', 'GUEST_PORTAL', 'NEW',
     'Maria', 'Garcia', 'maria@email.com',
     NULL, '2026-08-21 10:30:00', '2026-08-21 10:30:00', NULL, NULL,
     NULL, NULL, NULL, NULL, NULL, NULL),

    -- 2) IN_PROGRESS, Jose Antonio, URGENT (a leak can't wait)
    (2, 1, 3, 'INC-2026-000002', 'Fuga de agua en bano',
     'Hay una fuga de agua bajo el lavabo del bano principal. El suelo se moja.',
     'PLUMBING', 'URGENT', 'PHONE', 'IN_PROGRESS',
     'Carlos', 'Lopez', 'carlos@email.com',
     (SELECT id FROM app_user WHERE email = 'jose.perez@net2rent.com'),
     '2026-08-22 09:15:00', '2026-08-22 09:15:00', '2026-08-22 09:45:00', '2026-08-22 10:00:00',
     NULL, NULL, NULL, NULL, NULL, NULL),

    -- 3) RESOLVED, Jose Antonio
    (3, 1, 1, 'INC-2026-000003', 'Persiana del dormitorio atascada',
     'La persiana electrica del dormitorio principal no sube ni baja. Se escucha el motor pero no mueve.',
     'OTHER', 'LOW', 'GUEST_PORTAL', 'RESOLVED',
     'Ana', 'Martinez', 'ana@email.com',
     (SELECT id FROM app_user WHERE email = 'jose.perez@net2rent.com'),
     '2026-08-20 14:00:00', '2026-08-20 14:00:00', '2026-08-20 14:30:00', '2026-08-20 15:00:00',
     '2026-08-20 18:00:00', NULL, 45, 'Se ha reiniciado el motor de la persiana y ha vuelto a funcionar correctamente.',
     NULL, NULL),

    -- 4) NEW, account 2 (tenant isolation), unassigned
    (4, 2, 2, 'INC-2026-000099', 'Cerradura de la puerta principal',
     'La cerradura de la puerta principal esta dificil de girar con la llave.',
     'LOCKSMITH', 'HIGH', 'GUEST_PORTAL', 'NEW',
     'Pedro', 'Sanchez', 'pedro@email.com',
     NULL, '2026-08-23 11:00:00', '2026-08-23 11:00:00', NULL, NULL,
     NULL, NULL, NULL, NULL, NULL, NULL),

    -- 5) CLOSED, Marc Vidal (the one that goes "to another operator")
    (5, 1, 4, 'INC-2026-000004', 'Bombilla del pasillo fundida',
     'La bombilla del pasillo de entrada no enciende. Puede ser la bombilla o el interruptor.',
     'ELECTRICITY', 'LOW', 'GUEST_PORTAL', 'CLOSED',
     'Laura', 'Fernandez', 'laura@email.com',
     (SELECT id FROM app_user WHERE email = 'marc.vidal@net2rent.com'),
     '2026-08-18 09:00:00', '2026-08-18 09:00:00', '2026-08-18 09:30:00', '2026-08-18 10:00:00',
     '2026-08-18 10:20:00', '2026-08-19 08:00:00', 20, 'Se ha sustituido la bombilla fundida por una nueva LED.',
     NULL, NULL),

    -- 6) ASSIGNED, Jose Antonio (not started yet)
    (6, 1, 3, 'INC-2026-000005', 'Wifi no funciona en el dormitorio',
     'El router no llega con buena senal al dormitorio principal, el huesped no tiene wifi ahi.',
     'OTHER', 'NORMAL', 'GUEST_PORTAL', 'ASSIGNED',
     'Jorge', 'Ruiz', 'jorge@email.com',
     (SELECT id FROM app_user WHERE email = 'jose.perez@net2rent.com'),
     '2026-08-24 12:00:00', '2026-08-24 12:00:00', '2026-08-24 12:30:00', NULL,
     NULL, NULL, NULL, NULL, NULL, NULL),

    -- 7) PAUSED, Jose Antonio (waiting for a spare part)
    (7, 1, 5, 'INC-2026-000006', 'Nevera hace ruido extrano',
     'La nevera de la cocina hace un ruido intermitente, parece el compresor.',
     'APPLIANCES', 'NORMAL', 'PHONE', 'PAUSED',
     'Elena', 'Torres', 'elena@email.com',
     (SELECT id FROM app_user WHERE email = 'jose.perez@net2rent.com'),
     '2026-08-19 08:00:00', '2026-08-19 08:00:00', '2026-08-19 08:30:00', '2026-08-19 09:00:00',
     NULL, NULL, NULL, NULL,
     'Se necesita pieza de recambio, pendiente de pedido al proveedor.', NULL),

    -- 8) REJECTED, unassigned
    (8, 1, 4, 'INC-2026-000007', 'Solicitud de cambio de muebles',
     'El huesped pide cambiar el sofa del salon porque no le gusta el color.',
     'FURNITURE', 'LOW', 'GUEST_PORTAL', 'REJECTED',
     'Pablo', 'Diaz', 'pablo@email.com',
     NULL, '2026-08-25 16:00:00', '2026-08-25 16:00:00', NULL, NULL,
     NULL, NULL, NULL, NULL,
     NULL, 'No es una incidencia de mantenimiento, es una preferencia estetica. Fuera del alcance del servicio.')
    ON CONFLICT (id) DO NOTHING;

-- 6) Checklists (rule CU-CHK-08: RESOLVED/CLOSED -> all items done=true)
INSERT INTO incident_checklist_item (id, incident_id, text, position, done, checked_by_id, checked_at)
VALUES
    -- Inc 1 (NEW): prepared, unchecked
    (1, 1, 'Comprobar el mando a distancia y cambiar las pilas', 0, false, NULL, NULL),
    (2, 1, 'Revisar y limpiar el filtro del split', 1, false, NULL, NULL),
    (3, 1, 'Medir con termometro la temperatura de salida del aire', 2, false, NULL, NULL),

    -- Inc 2 (IN_PROGRESS): 1 of 3 -> progress bar
    (4, 2, 'Cerrar la llave de paso del lavabo', 0, true,
     (SELECT id FROM app_user WHERE email = 'jose.perez@net2rent.com'), '2026-08-22 10:05:00'),
    (5, 2, 'Sustituir el sifon danado', 1, false, NULL, NULL),
    (6, 2, 'Comprobar que no gotea tras 10 min con el agua abierta', 2, false, NULL, NULL),

    -- Inc 3 (RESOLVED): all done
    (7, 3, 'Revisar el motor de la persiana', 0, true,
     (SELECT id FROM app_user WHERE email = 'jose.perez@net2rent.com'), '2026-08-20 17:30:00'),
    (8, 3, 'Reiniciar el mecanismo', 1, true,
     (SELECT id FROM app_user WHERE email = 'jose.perez@net2rent.com'), '2026-08-20 17:50:00'),
    (9, 3, 'Probar la subida y bajada completa de la persiana', 2, true,
     (SELECT id FROM app_user WHERE email = 'jose.perez@net2rent.com'), '2026-08-20 17:55:00'),

    -- Inc 5 (CLOSED): all done, by Marc Vidal
    (10, 5, 'Comprobar el interruptor', 0, true,
     (SELECT id FROM app_user WHERE email = 'marc.vidal@net2rent.com'), '2026-08-18 10:10:00'),
    (11, 5, 'Sustituir la bombilla', 1, true,
     (SELECT id FROM app_user WHERE email = 'marc.vidal@net2rent.com'), '2026-08-18 10:15:00'),

    -- Inc 6 (ASSIGNED): prepared, not started
    (12, 6, 'Localizar la posicion actual del router', 0, false, NULL, NULL),
    (13, 6, 'Valorar instalar un repetidor wifi en el pasillo', 1, false, NULL, NULL),

    -- Inc 7 (PAUSED): started, 1 done and 1 pending (spare part)
    (14, 7, 'Escuchar y localizar el origen del ruido del compresor', 0, true,
     (SELECT id FROM app_user WHERE email = 'jose.perez@net2rent.com'), '2026-08-19 09:10:00'),
    (15, 7, 'Sustituir la pieza del compresor (pendiente de recambio)', 1, false, NULL, NULL),

    -- Inc 4 (NEW, account 2): unchecked
    (16, 4, 'Probar la cerradura con la llave de repuesto', 0, false, NULL, NULL),
    (17, 4, 'Lubricar el bombin', 1, false, NULL, NULL)
    ON CONFLICT (id) DO NOTHING;

-- 7) Comments (timeline: long and short text)
INSERT INTO incident_comment (id, incident_id, author_id, text, created_at)
VALUES
    (1, 1, (SELECT id FROM app_user WHERE email = 'maria.martin@net2rent.com'),
     'El huesped avisa de que no enfria desde ayer por la tarde. Pendiente de asignar operario en cuanto haya disponibilidad.',
     '2026-08-21 11:00:00'),

    (2, 2, (SELECT id FROM app_user WHERE email = 'jose.perez@net2rent.com'),
     'Localizada la fuga: viene de la junta del sifon, que esta agrietada. He cerrado la llave de paso para que no siga mojando y voy a la ferreteria a por un sifon nuevo. Vuelvo esta tarde para terminar.',
     '2026-08-22 10:30:00'),
    (3, 2, (SELECT id FROM app_user WHERE email = 'maria.martin@net2rent.com'),
     'Ok, avisame cuando este cerrado.', '2026-08-22 10:35:00'),

    (4, 3, (SELECT id FROM app_user WHERE email = 'jose.perez@net2rent.com'),
     'Motor reiniciado y probado varias veces, sube y baja sin problema.', '2026-08-20 17:58:00'),

    (5, 5, (SELECT id FROM app_user WHERE email = 'maria.martin@net2rent.com'),
     'Asignada a Marc, prioridad baja.', '2026-08-18 09:30:00'),
    (6, 5, (SELECT id FROM app_user WHERE email = 'marc.vidal@net2rent.com'),
     'Revisado, era la bombilla. Sustituida por una LED nueva.', '2026-08-18 10:20:00'),

    (7, 6, (SELECT id FROM app_user WHERE email = 'maria.martin@net2rent.com'),
     'Te la asigno a ti, Jose Antonio. No corre prisa, cuando puedas esta semana.', '2026-08-24 12:30:00'),

    (8, 7, (SELECT id FROM app_user WHERE email = 'jose.perez@net2rent.com'),
     'El ruido viene del compresor. He pedido la pieza al proveedor, pero tarda unos dias, asi que dejo la incidencia en pausa hasta que llegue el recambio. En cuanto lo tenga, la retomo.',
     '2026-08-19 09:15:00'),

    (9, 8, (SELECT id FROM app_user WHERE email = 'maria.martin@net2rent.com'),
     'Comentado con el propietario: el cambio de sofa por gusto no entra en el servicio de mantenimiento.',
     '2026-08-25 16:15:00')
    ON CONFLICT (id) DO NOTHING;

-- 8) History (append-only). Coordinator actor = Maria Martin.
INSERT INTO incident_history (id, incident_id, actor_id, event_type, previous_value, new_value, note, created_at)
VALUES
    -- Inc 1
    (1, 1, NULL, 'CREATED', NULL, NULL, NULL, '2026-08-21 10:30:00'),
    -- Inc 4 (account 2)
    (2, 4, NULL, 'CREATED', NULL, NULL, NULL, '2026-08-23 11:00:00'),
    -- Inc 2
    (3, 2, NULL, 'CREATED', NULL, NULL, NULL, '2026-08-22 09:15:00'),
    (4, 2, (SELECT id FROM app_user WHERE email = 'maria.martin@net2rent.com'), 'STATUS_CHANGED', 'NEW', 'ASSIGNED', NULL, '2026-08-22 09:45:00'),
    (5, 2, (SELECT id FROM app_user WHERE email = 'jose.perez@net2rent.com'),   'STATUS_CHANGED', 'ASSIGNED', 'IN_PROGRESS', NULL, '2026-08-22 10:00:00'),
    -- Inc 3
    (6, 3, NULL, 'CREATED', NULL, NULL, NULL, '2026-08-20 14:00:00'),
    (7, 3, (SELECT id FROM app_user WHERE email = 'maria.martin@net2rent.com'), 'STATUS_CHANGED', 'NEW', 'ASSIGNED', NULL, '2026-08-20 14:30:00'),
    (8, 3, (SELECT id FROM app_user WHERE email = 'jose.perez@net2rent.com'),   'STATUS_CHANGED', 'ASSIGNED', 'IN_PROGRESS', NULL, '2026-08-20 15:00:00'),
    (9, 3, (SELECT id FROM app_user WHERE email = 'jose.perez@net2rent.com'),   'STATUS_CHANGED', 'IN_PROGRESS', 'RESOLVED', 'Persiana reparada, motor reiniciado.', '2026-08-20 18:00:00'),
    -- Inc 5 (Marc Vidal)
    (10, 5, NULL, 'CREATED', NULL, NULL, NULL, '2026-08-18 09:00:00'),
    (11, 5, (SELECT id FROM app_user WHERE email = 'maria.martin@net2rent.com'), 'STATUS_CHANGED', 'NEW', 'ASSIGNED', 'Asignada al operario.', '2026-08-18 09:30:00'),
    (12, 5, (SELECT id FROM app_user WHERE email = 'marc.vidal@net2rent.com'),   'STATUS_CHANGED', 'ASSIGNED', 'IN_PROGRESS', NULL, '2026-08-18 10:00:00'),
    (13, 5, (SELECT id FROM app_user WHERE email = 'marc.vidal@net2rent.com'),   'STATUS_CHANGED', 'IN_PROGRESS', 'RESOLVED', NULL, '2026-08-18 10:20:00'),
    (14, 5, (SELECT id FROM app_user WHERE email = 'maria.martin@net2rent.com'), 'STATUS_CHANGED', 'RESOLVED', 'CLOSED', 'Confirmado con el huesped que quedo resuelto.', '2026-08-19 08:00:00'),
    -- Inc 6
    (15, 6, NULL, 'CREATED', NULL, NULL, NULL, '2026-08-24 12:00:00'),
    (16, 6, (SELECT id FROM app_user WHERE email = 'maria.martin@net2rent.com'), 'STATUS_CHANGED', 'NEW', 'ASSIGNED', NULL, '2026-08-24 12:30:00'),
    -- Inc 7
    (17, 7, NULL, 'CREATED', NULL, NULL, NULL, '2026-08-19 08:00:00'),
    (18, 7, (SELECT id FROM app_user WHERE email = 'maria.martin@net2rent.com'), 'STATUS_CHANGED', 'NEW', 'ASSIGNED', NULL, '2026-08-19 08:30:00'),
    (19, 7, (SELECT id FROM app_user WHERE email = 'jose.perez@net2rent.com'),   'STATUS_CHANGED', 'ASSIGNED', 'IN_PROGRESS', NULL, '2026-08-19 09:00:00'),
    (20, 7, (SELECT id FROM app_user WHERE email = 'jose.perez@net2rent.com'),   'STATUS_CHANGED', 'IN_PROGRESS', 'PAUSED', 'Se necesita pieza de recambio.', '2026-08-19 09:15:00'),
    -- Inc 8
    (21, 8, NULL, 'CREATED', NULL, NULL, NULL, '2026-08-25 16:00:00'),
    (22, 8, (SELECT id FROM app_user WHERE email = 'maria.martin@net2rent.com'), 'STATUS_CHANGED', 'NEW', 'REJECTED', 'Fuera del alcance del servicio de mantenimiento.', '2026-08-25 16:20:00')
    ON CONFLICT (id) DO NOTHING;

-- 9) Time entries (sum matches minutes_spent: inc3=45, inc5=20)
INSERT INTO incident_time_entry (id, incident_id, author_id, concept, minutes, created_at)
VALUES
    (1, 2, (SELECT id FROM app_user WHERE email = 'jose.perez@net2rent.com'), 'Diagnostico de la fuga in situ', 20, '2026-08-22 10:20:00'),
    (2, 2, (SELECT id FROM app_user WHERE email = 'jose.perez@net2rent.com'), 'Desplazamiento y compra del sifon', 30, '2026-08-22 12:00:00'),
    (3, 3, (SELECT id FROM app_user WHERE email = 'jose.perez@net2rent.com'), 'Revision del motor de la persiana', 15, '2026-08-20 15:30:00'),
    (4, 3, (SELECT id FROM app_user WHERE email = 'jose.perez@net2rent.com'), 'Reinicio del mecanismo y pruebas', 30, '2026-08-20 17:55:00'),
    (5, 5, (SELECT id FROM app_user WHERE email = 'marc.vidal@net2rent.com'), 'Comprobacion del interruptor', 5, '2026-08-18 10:05:00'),
    (6, 5, (SELECT id FROM app_user WHERE email = 'marc.vidal@net2rent.com'), 'Sustitucion de la bombilla', 15, '2026-08-18 10:15:00'),
    (7, 7, (SELECT id FROM app_user WHERE email = 'jose.perez@net2rent.com'), 'Diagnostico del ruido del compresor', 25, '2026-08-19 09:10:00')
    ON CONFLICT (id) DO NOTHING;

-- 10) Resync sequences after inserting explicit IDs
SELECT setval(pg_get_serial_sequence('account', 'id'),                 COALESCE((SELECT MAX(id) FROM account), 1));
SELECT setval(pg_get_serial_sequence('lodging', 'id'),                 COALESCE((SELECT MAX(id) FROM lodging), 1));
SELECT setval(pg_get_serial_sequence('incident', 'id'),                COALESCE((SELECT MAX(id) FROM incident), 1));
SELECT setval(pg_get_serial_sequence('incident_counter', 'id'),        COALESCE((SELECT MAX(id) FROM incident_counter), 1));
SELECT setval(pg_get_serial_sequence('incident_checklist_item', 'id'), COALESCE((SELECT MAX(id) FROM incident_checklist_item), 1));
SELECT setval(pg_get_serial_sequence('incident_comment', 'id'),        COALESCE((SELECT MAX(id) FROM incident_comment), 1));
SELECT setval(pg_get_serial_sequence('incident_history', 'id'),        COALESCE((SELECT MAX(id) FROM incident_history), 1));
SELECT setval(pg_get_serial_sequence('incident_time_entry', 'id'),     COALESCE((SELECT MAX(id) FROM incident_time_entry), 1));

-- Sync the counter with the highest existing code number per account/year
UPDATE incident_counter c
SET last_number = GREATEST(c.last_number, COALESCE(sub.max_seq, 0))
    FROM (
    SELECT account_id,
           CAST(split_part(code, '-', 2) AS int) AS yr,
           MAX(CAST(split_part(code, '-', 3) AS int)) AS max_seq
    FROM incident
    GROUP BY account_id, CAST(split_part(code, '-', 2) AS int)
) sub
WHERE c.account_id = sub.account_id AND c.counter_year = sub.yr;