-- Seed LOCAL para prueba de JWT

-- 1) Una cuenta (empresa). Para el aislamiento por account_id
INSERT INTO account (id, name, active)
VALUES (1, 'net2Rent Demo', true)
ON CONFLICT (id) DO NOTHING;

-- 2) Usuarios de prueba. TODOS con la misma contraseña: Test1234
INSERT INTO app_user (account_id, first_name, last_name, email, password_hash, role, active)
VALUES
  (1, 'Admin',       'Demo', 'admin@net2rent.com',       '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'ADMIN',       true),
  (1, 'Coordinador', 'Demo', 'coordinador@net2rent.com', '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'COORDINATOR', true),
  (1, 'Operario',    'Demo', 'operario@net2rent.com',    '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'OPERATOR',    true),
  (1, 'Inactivo',    'Demo', 'inactivo@net2rent.com',    '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'OPERATOR',    false)
ON CONFLICT (email) DO NOTHING;