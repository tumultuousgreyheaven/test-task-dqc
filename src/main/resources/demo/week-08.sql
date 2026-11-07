-- Демо-данные: изменения за неделю 8 (до снимка 2026-10-26). Выполняется после прогона RUN-007.
-- Исправлено 6 нарушений, появилось 6 новых, 8 осталось.

-- Исправлено: у G-003 назначен ответственный (GOAL_NO_OWNER).
UPDATE goal SET owner = 'Крылов Д.С.' WHERE id = 'G-003';

-- Исправлено: у G-025 назначен ответственный вместо пробелов (GOAL_NO_OWNER).
UPDATE goal SET owner = 'Тихонов Е.А.' WHERE id = 'G-025';

-- Исправлено: у K-014 заполнено целевое значение (KR_NO_TARGET).
UPDATE key_result SET target_value = 30, fact_updated_at = '2026-10-24' WHERE id = 'K-014';

-- Исправлено: у G-015 появились КР (GOAL_NO_KR), веса сходятся.
INSERT INTO key_result (id, goal_id, title, target_value, actual_value, weight, fact_updated_at) VALUES
  ('K-050', 'G-015', 'Число новых корпоративных клиентов', 20, 5, 60, '2026-10-23'),
  ('K-051', 'G-015', 'Средний чек нового клиента, млн руб.', 10, 3, 40, '2026-10-23');

-- Исправлено: у G-016 веса КР приведены к 100% (KR_WEIGHTS_SUM).
UPDATE key_result SET weight = 20 WHERE id = 'K-022';

-- Исправлено, но появилось другое нарушение: у G-026 заведены КР (GOAL_NO_KR решено), веса дают 80% (KR_WEIGHTS_SUM новое).
INSERT INTO key_result (id, goal_id, title, target_value, actual_value, weight, fact_updated_at) VALUES
  ('K-052', 'G-026', 'Привлечённые средства по депозитам, млрд руб.', 3, 0.8, 50, '2026-10-22'),
  ('K-053', 'G-026', 'Число новых вкладчиков, тыс.', 2, 0.5, 30, '2026-10-22');

-- Новое: у G-010 ответственный ушёл, нового не назначили (GOAL_NO_OWNER).
UPDATE goal SET owner = NULL WHERE id = 'G-010';

-- Новое: у K-042 стёрли целевое значение (KR_NO_TARGET).
UPDATE key_result SET target_value = NULL WHERE id = 'K-042';

-- Новое: у G-024 веса КР теперь дают 90% (KR_WEIGHTS_SUM).
UPDATE key_result SET weight = 30 WHERE id = 'K-037';

-- Новое: черновик G-005 перевели в активные без ответственного и КР (GOAL_NO_OWNER и GOAL_NO_KR).
UPDATE goal SET status = 'active' WHERE id = 'G-005';
