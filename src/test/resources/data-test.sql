INSERT INTO account (id, name, active) VALUES (1, 'net2Rent Demo', true);

INSERT INTO account (id, name, active) VALUES (2, 'Otra Empresa', true);

INSERT INTO app_user (account_id, first_name, last_name, email, password_hash, role, active) VALUES
                                                                                                 (1, 'Admin',    'Demo', 'admin@net2rent.com',    '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'ADMIN',    true),
                                                                                                 (1, 'Operario', 'Demo', 'operario@net2rent.com', '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'OPERATOR', true),
                                                                                                 (2, 'Admin',    'Otra', 'admin@otraempresa.com', '$2b$10$oPN2dLCxpahTO1Af4sFutuMmS/bt3sgJCf/SDpq78qitfdywngNzy', 'ADMIN',    true);

INSERT INTO lodging (id, account_id, ref, pin_hash, name, active) VALUES
                                                                      (1, 1, 'APT-1001', '$2b$10$tX8RJ/AN.9NkzP1fE.DeXu7tAvovFMCwuQwEqHO4r1U6iqfZVuR1i', 'Piso Centro', true),
                                                                      (2, 2, 'APT-2001', '$2b$10$tX8RJ/AN.9NkzP1fE.DeXu7tAvovFMCwuQwEqHO4r1U6iqfZVuR1i', 'Piso Playa',  true);