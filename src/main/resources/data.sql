-- Seed automático que Spring ejecuta en cada arranque (solo para desarrollo).
INSERT INTO account (id, name, active)
VALUES (1, 'net2Rent Demo', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO app_user (account_id, first_name, last_name, email, password_hash, role, active)
VALUES
  (1, 'Admin',       'Demo', 'admin@net2rent.com',       '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'ADMIN',       true),
  (1, 'Coordinador', 'Demo', 'coordinador@net2rent.com', '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'COORDINATOR', true),
  (1, 'Operario',    'Demo', 'operario@net2rent.com',    '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'OPERATOR',    true),
  (1, 'Inactivo',    'Demo', 'inactivo@net2rent.com',    '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'OPERATOR',    false)
ON CONFLICT (email) DO NOTHING;

-- Segunda cuenta, para probar el AISLAMIENTO entre empresas.
INSERT INTO account (id, name, active)
VALUES (2, 'Otra Empresa', true)
    ON CONFLICT (id) DO NOTHING;

-- Un admin en la cuenta 2 (reutilizo el mismo hash que ya usáis).
INSERT INTO app_user (account_id, first_name, last_name, email, password_hash, role, active)
VALUES
    (2, 'Admin', 'Otra', 'admin@otraempresa.com',
     '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'ADMIN', true)
    ON CONFLICT (email) DO NOTHING;

-- Un alojamiento en CADA cuenta, con id conocido para los tests.
INSERT INTO lodging (id, account_id, ref, pin_hash, name, active)
VALUES
    (1, 1, 'APT-1001',
     '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'Piso Centro', true),
    (2, 2, 'APT-2001',
     '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'Piso Playa', true)
    ON CONFLICT (id) DO NOTHING;