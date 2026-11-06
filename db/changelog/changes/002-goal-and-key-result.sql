--liquibase formatted sql

-- Ограничения здесь намеренно мягкие там, где качество данных проверяют правила ККД:
-- owner и target_value могут быть NULL, сумма весов КР не контролируется.

--changeset dqc:002-goal
CREATE TABLE goal (
    id             VARCHAR(32)  NOT NULL,
    unit_id        VARCHAR(32)  NOT NULL,
    title          VARCHAR(500) NOT NULL,
    owner          VARCHAR(255),
    parent_goal_id VARCHAR(32),
    status         VARCHAR(16)  NOT NULL,
    due_date       DATE         NOT NULL,
    CONSTRAINT pk_goal PRIMARY KEY (id),
    CONSTRAINT fk_goal_unit FOREIGN KEY (unit_id) REFERENCES org_unit (id),
    CONSTRAINT fk_goal_parent FOREIGN KEY (parent_goal_id) REFERENCES goal (id),
    CONSTRAINT ck_goal_status CHECK (status IN ('draft', 'active', 'achieved', 'cancelled')),
    CONSTRAINT ck_goal_not_own_parent CHECK (parent_goal_id IS NULL OR parent_goal_id <> id)
);

CREATE INDEX ix_goal_unit_status ON goal (unit_id, status);
CREATE INDEX ix_goal_parent ON goal (parent_goal_id);

COMMENT ON TABLE goal IS 'Цели — правила ККД применяются только к status = active';
COMMENT ON COLUMN goal.owner IS 'Ответственный — NULL или пустая строка = не назначен (GOAL_NO_OWNER)';
COMMENT ON COLUMN goal.parent_goal_id IS 'Вышестоящая цель (каскадирование)';
--rollback DROP TABLE goal;

--changeset dqc:002-key-result
CREATE TABLE key_result (
    id              VARCHAR(32)   NOT NULL,
    goal_id         VARCHAR(32)   NOT NULL,
    title           VARCHAR(500)  NOT NULL,
    target_value    NUMERIC(18, 4),
    actual_value    NUMERIC(18, 4),
    weight          NUMERIC(5, 2) NOT NULL,
    fact_updated_at DATE,
    CONSTRAINT pk_key_result PRIMARY KEY (id),
    CONSTRAINT fk_key_result_goal FOREIGN KEY (goal_id) REFERENCES goal (id) ON DELETE CASCADE,
    CONSTRAINT ck_key_result_weight CHECK (weight >= 0 AND weight <= 100)
);

CREATE INDEX ix_key_result_goal ON key_result (goal_id);

COMMENT ON TABLE key_result IS 'Ключевые результаты цели';
COMMENT ON COLUMN key_result.target_value IS 'Целевое значение — NULL = не заполнено (KR_NO_TARGET), 0 — допустимое значение';
COMMENT ON COLUMN key_result.weight IS 'Вес КР в процентах (0..100) — сумма по цели должна быть 100 (KR_WEIGHTS_SUM)';
COMMENT ON COLUMN key_result.fact_updated_at IS 'Дата последнего обновления факта';
--rollback DROP TABLE key_result;
