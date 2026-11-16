-- Seed para AMBIENTE LOCAL (H2). Senhas de teste (apenas desenvolvimento), guardadas como hash BCrypt:
--   admin@cuidar.com        / Admin@123
--   cuidador@cuidar.com     / Cuidador@123
--   responsavel@cuidar.com  / Responsavel@123
--   idoso@cuidar.com        / Idoso@123
-- Os ids sao gerados pelo banco; os relacionamentos usam subconsultas por e-mail/CPF.

INSERT INTO usuario (email, senha_hash, papel, ativo) VALUES
 ('admin@cuidar.com',       '$2a$10$8cmpZTK5GHgbnADFJFByYeozuNoqaVF9oUtx23R.Raid5./7lphIS', 'ADMIN',       TRUE);
INSERT INTO usuario (email, senha_hash, papel, ativo) VALUES
 ('cuidador@cuidar.com',    '$2a$10$NARlpCtJ.MfeENbE6WLw1O9vBZlrAWAFcBJdtjtYp7hdSA/0LJ.n.', 'CUIDADOR',    TRUE);
INSERT INTO usuario (email, senha_hash, papel, ativo) VALUES
 ('responsavel@cuidar.com', '$2a$10$8lFoqczbqb2frQSmP8rrIuKflncmXQW2G5/mKDAqTg0u1ASZDnpoK', 'RESPONSAVEL', TRUE);
INSERT INTO usuario (email, senha_hash, papel, ativo) VALUES
 ('idoso@cuidar.com',       '$2a$10$OSMLPUffY/89xGWuEIS21.wOt512B.vuwzcbdggCWnxr4d1eLmja.', 'IDOSO',       TRUE);

INSERT INTO cuidador (nome, email, telefone, registro_profissional, ativo, usuario_id)
 SELECT 'Carla Cuidadora', 'cuidador@cuidar.com', '11999990001', 'COREN-SP 123456', TRUE, u.id
 FROM usuario u WHERE u.email = 'cuidador@cuidar.com';

INSERT INTO responsavel (nome, email, telefone, ativo, usuario_id)
 SELECT 'Rafael Responsavel', 'responsavel@cuidar.com', '11999990002', TRUE, u.id
 FROM usuario u WHERE u.email = 'responsavel@cuidar.com';

INSERT INTO idoso (nome, data_nascimento, cpf, telefone, endereco, condicoes_medicas, ativo,
                   fc_min, fc_max, glicemia_min, glicemia_max, spo2_min, pressao_sist_max, usuario_id)
 SELECT 'Dona Maria Aparecida', DATE '1945-03-12', '12345678909', '11999990003',
        'Rua das Flores, 100 - Sao Paulo/SP', 'Hipertensao e diabetes tipo 2', TRUE,
        60, 100, 70, 140, 95, 140, u.id
 FROM usuario u WHERE u.email = 'idoso@cuidar.com';

INSERT INTO idoso_cuidador (idoso_id, cuidador_id)
 SELECT i.id, c.id FROM idoso i, cuidador c
 WHERE i.cpf = '12345678909' AND c.email = 'cuidador@cuidar.com';

INSERT INTO idoso_responsavel (idoso_id, responsavel_id, parentesco, prioridade_contato)
 SELECT i.id, r.id, 'FILHO', 1 FROM idoso i, responsavel r
 WHERE i.cpf = '12345678909' AND r.email = 'responsavel@cuidar.com';

INSERT INTO lembrete_remedio (idoso_id, medicamento, dosagem, horario, frequencia, inicio, fim, ativo,
                              omissoes_seguidas, criado_em)
 SELECT i.id, 'Losartana', '50 mg', TIME '08:00:00', 'DIARIA', DATE '2026-11-01', NULL, TRUE, 0, CURRENT_TIMESTAMP
 FROM idoso i WHERE i.cpf = '12345678909';

INSERT INTO lembrete_remedio (idoso_id, medicamento, dosagem, horario, frequencia, inicio, fim, ativo,
                              omissoes_seguidas, criado_em)
 SELECT i.id, 'Metformina', '850 mg', TIME '20:00:00', 'DIARIA', DATE '2026-11-01', NULL, TRUE, 0, CURRENT_TIMESTAMP
 FROM idoso i WHERE i.cpf = '12345678909';

INSERT INTO evento_sensor (idoso_id, tipo, valor, unidade, dispositivo, data_hora, gravidade)
 SELECT i.id, 'BATIMENTO', 78, 'bpm', 'pulseira-001', CURRENT_TIMESTAMP, NULL
 FROM idoso i WHERE i.cpf = '12345678909';

INSERT INTO evento_sensor (idoso_id, tipo, valor, unidade, dispositivo, data_hora, gravidade)
 SELECT i.id, 'SPO2', 97, '%', 'pulseira-001', CURRENT_TIMESTAMP, NULL
 FROM idoso i WHERE i.cpf = '12345678909';
