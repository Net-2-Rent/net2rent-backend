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
INSERT INTO incident_counter (id, account_id, year, last_number)
VALUES
    (1, 1, 2026, 3),
    (2, 2, 2026, 1)
ON CONFLICT (id) DO NOTHING;

-- 4) Alojamiento
INSERT INTO lodging (id, account_id, ref, pin_hash, name, active)
VALUES
    (1, 1, 'APT-1001',
 '$2a$12$6a7rJB14vKRzFx/w4kWtLe1/8mp6ByGksfnjxLIHHkyN0XrTXTbTe', 'Piso Centro', true),
(2, 2, 'APT-2001',
 '$2a$12$6a7rJB14vKRzFx/w4kWtLe1/8mp6ByGksfnjxLIHHkyN0XrTXTbTe', 'Piso Playa', true)
ON CONFLICT (id) DO NOTHING;

-- 5) Incidencias APT-1001 (account 1)
INSERT INTO incident (id, account_id, lodging_id, code, title, description,
                      category, priority, source, status,
                      guest_first_name, guest_last_name, guest_contact,
                      opened_at, created_at, resolved_at, closed_at)
VALUES
    (1, 1, 1, 'INC-2026-000001', 'Aire acondicionado no enfría',
     'El aire acondicionado del salón no enfría aunque está encendido y el termostato marca 22°C.',
     'HVAC', 'HIGH', 'GUEST_PORTAL', 'NEW',
     'María', 'García', 'maria@email.com',
     '2026-08-21 10:30:00', '2026-08-21 10:30:00',
     NULL, NULL),

    (2, 1, 1, 'INC-2026-000002', 'Fuga de agua en baño',
     'Hay una fuga de agua pequeña bajo el lavabo del baño principal. El suelo se moja.',
     'PLUMBING', 'NORMAL', 'PHONE', 'IN_PROGRESS',
     'Carlos', 'López', 'carlos@email.com',
     '2026-08-22 09:15:00', '2026-08-22 09:15:00',
     NULL, NULL),

    (3, 1, 1, 'INC-2026-000003', 'Persiana del dormitorio atascada',
     'La persiana eléctrica del dormitorio principal no sube ni baja. Se escucha motor pero no mueve.',
     'OTHER', 'LOW', 'GUEST_PORTAL', 'RESOLVED',
     'Ana', 'Martínez', 'ana@email.com',
     '2026-08-20 14:00:00', '2026-08-20 14:00:00',
     '2026-08-20 18:00:00', NULL)
ON CONFLICT (id) DO NOTHING;

-- 6) Incidencia APT-2001 (account 2)
INSERT INTO incident (id, account_id, lodging_id, code, title, description,
                      category, priority, source, status,
                      guest_first_name, guest_last_name, guest_contact,
                      opened_at, created_at, resolved_at, closed_at)
VALUES
    (4, 2, 2, 'INC-2026-000004', 'Cerradura de la puerta principal',
     'La cerradura de la puerta principal está difícil de girar con la llave.',
     'LOCKSMITH', 'HIGH', 'GUEST_PORTAL', 'NEW',
     'Pedro', 'Sánchez', 'pedro@email.com',
     '2026-08-23 11:00:00', '2026-08-23 11:00:00',
     NULL, NULL)
ON CONFLICT (id) DO NOTHING;