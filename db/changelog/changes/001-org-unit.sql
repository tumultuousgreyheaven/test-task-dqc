--liquibase formatted sql

--changeset dqc:001-org-unit
CREATE TABLE org_unit (
    id        VARCHAR(32)  NOT NULL,
    name      VARCHAR(255) NOT NULL,
    parent_id VARCHAR(32),
    CONSTRAINT pk_org_unit PRIMARY KEY (id),
    CONSTRAINT fk_org_unit_parent FOREIGN KEY (parent_id) REFERENCES org_unit (id),
    CONSTRAINT ck_org_unit_not_own_parent CHECK (parent_id IS NULL OR parent_id <> id)
);

CREATE INDEX ix_org_unit_parent ON org_unit (parent_id);

COMMENT ON TABLE org_unit IS 'Подразделения — иерархия через parent_id';
--rollback DROP TABLE org_unit;
