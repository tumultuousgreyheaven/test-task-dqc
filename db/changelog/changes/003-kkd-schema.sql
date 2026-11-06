--liquibase formatted sql

--changeset dqc:003-kkd-rule
CREATE TABLE kkd_rule (
    code        VARCHAR(64)  NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    severity    VARCHAR(16)  NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_kkd_rule PRIMARY KEY (code),
    CONSTRAINT ck_kkd_rule_severity CHECK (severity IN ('critical', 'major', 'minor'))
);

COMMENT ON TABLE kkd_rule IS 'Справочник правил ККД — код совпадает с QualityRule.code() в приложении';
--rollback DROP TABLE kkd_rule;

--changeset dqc:003-kkd-run
-- Идентификаторы прогонов вида RUN-008 формируются из последовательности: 'RUN-' || lpad(nextval('kkd_run_seq')::text, 3, '0')
CREATE SEQUENCE kkd_run_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE kkd_run (
    id                 VARCHAR(32)  NOT NULL,
    snapshot_date      DATE         NOT NULL,
    status             VARCHAR(16)  NOT NULL,
    compared_to_run_id VARCHAR(32),
    started_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    finished_at        TIMESTAMPTZ,
    CONSTRAINT pk_kkd_run PRIMARY KEY (id),
    CONSTRAINT fk_kkd_run_compared_to FOREIGN KEY (compared_to_run_id) REFERENCES kkd_run (id),
    CONSTRAINT ck_kkd_run_status CHECK (status IN ('running', 'completed', 'failed')),
    CONSTRAINT ck_kkd_run_not_self_compared CHECK (compared_to_run_id IS NULL OR compared_to_run_id <> id)
);

CREATE INDEX ix_kkd_run_snapshot_date ON kkd_run (snapshot_date DESC);

COMMENT ON TABLE kkd_run IS 'Прогон ККД на снимке данных за snapshot_date';
COMMENT ON COLUMN kkd_run.compared_to_run_id IS 'Прогон, с которым сравнивается этот — NULL у первого прогона';
--rollback DROP TABLE kkd_run;
--rollback DROP SEQUENCE kkd_run_seq;

--changeset dqc:003-kkd-issue
CREATE TABLE kkd_issue (
    id          BIGINT GENERATED ALWAYS AS IDENTITY,
    run_id      VARCHAR(32)  NOT NULL,
    rule_code   VARCHAR(64)  NOT NULL,
    severity    VARCHAR(16)  NOT NULL,
    object_type VARCHAR(16)  NOT NULL,
    object_id   VARCHAR(32)  NOT NULL,
    goal_id     VARCHAR(32)  NOT NULL,
    unit_id     VARCHAR(32)  NOT NULL,
    message     TEXT         NOT NULL,
    CONSTRAINT pk_kkd_issue PRIMARY KEY (id),
    CONSTRAINT fk_kkd_issue_run FOREIGN KEY (run_id) REFERENCES kkd_run (id) ON DELETE CASCADE,
    CONSTRAINT fk_kkd_issue_rule FOREIGN KEY (rule_code) REFERENCES kkd_rule (code),
    CONSTRAINT fk_kkd_issue_unit FOREIGN KEY (unit_id) REFERENCES org_unit (id),
    CONSTRAINT ck_kkd_issue_severity CHECK (severity IN ('critical', 'major', 'minor')),
    CONSTRAINT ck_kkd_issue_object_type CHECK (object_type IN ('goal', 'key_result')),
    -- «та же самая» проблема в двух прогонах = то же правило и тот же объект
    CONSTRAINT uq_kkd_issue_identity UNIQUE (run_id, rule_code, object_type, object_id)
);

CREATE INDEX ix_kkd_issue_run_unit ON kkd_issue (run_id, unit_id);
CREATE INDEX ix_kkd_issue_run_severity ON kkd_issue (run_id, severity);

COMMENT ON TABLE kkd_issue IS 'Проблемы, найденные в прогоне — снимок: severity, goal_id и unit_id фиксируются на момент прогона';
COMMENT ON COLUMN kkd_issue.object_id IS 'Идентификатор цели или КР (по object_type) — без FK, т.к. объект может быть удалён позже';
COMMENT ON COLUMN kkd_issue.goal_id IS 'Цель, к которой относится проблема (для КР — его цель) — без FK по той же причине';
--rollback DROP TABLE kkd_issue;

--changeset dqc:003-kkd-metric-value
CREATE TABLE kkd_metric_value (
    run_id      VARCHAR(32) NOT NULL,
    unit_id     VARCHAR(32) NOT NULL,
    metric_key  VARCHAR(64) NOT NULL,
    numerator   INTEGER     NOT NULL,
    denominator INTEGER     NOT NULL,
    CONSTRAINT pk_kkd_metric_value PRIMARY KEY (run_id, unit_id, metric_key),
    CONSTRAINT fk_kkd_metric_value_run FOREIGN KEY (run_id) REFERENCES kkd_run (id) ON DELETE CASCADE,
    CONSTRAINT fk_kkd_metric_value_unit FOREIGN KEY (unit_id) REFERENCES org_unit (id),
    CONSTRAINT ck_kkd_metric_value_range CHECK (numerator >= 0 AND denominator >= 0 AND numerator <= denominator)
);

COMMENT ON TABLE kkd_metric_value IS 'Числитель и знаменатель каждой метрики по прогону и подразделению';
COMMENT ON COLUMN kkd_metric_value.numerator IS 'Считается только по объектам самого подразделения — для подразделения с дочерними суммируется по поддереву при чтении';
--rollback DROP TABLE kkd_metric_value;
