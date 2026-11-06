--liquibase formatted sql

--changeset dqc:004-kkd-rules-data
INSERT INTO kkd_rule (code, title, description, severity) VALUES
    ('GOAL_NO_OWNER',  'Цель без ответственного',
     'У цели не назначен ответственный (пусто или только пробелы).', 'critical'),
    ('GOAL_NO_KR',     'У активной цели нет ключевых результатов',
     'У активной цели нет ни одного ключевого результата.', 'critical'),
    ('KR_WEIGHTS_SUM', 'Сумма весов КР цели не равна 100%',
     'Сумма весов ключевых результатов цели отличается от 100%. Цели без КР не проверяются (их ловит GOAL_NO_KR).', 'major'),
    ('KR_NO_TARGET',   'У КР не заполнено целевое значение',
     'У ключевого результата целевое значение не задано (NULL). Значение 0 считается заполненным.', 'major');
-- проблемы по этим правилам удаляются первыми: на правила ссылается kkd_issue
--rollback DELETE FROM kkd_issue WHERE rule_code IN ('GOAL_NO_OWNER', 'GOAL_NO_KR', 'KR_WEIGHTS_SUM', 'KR_NO_TARGET');
--rollback DELETE FROM kkd_rule WHERE code IN ('GOAL_NO_OWNER', 'GOAL_NO_KR', 'KR_WEIGHTS_SUM', 'KR_NO_TARGET');
