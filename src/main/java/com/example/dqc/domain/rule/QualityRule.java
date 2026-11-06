package com.example.dqc.domain.rule;

import com.example.dqc.domain.model.Finding;
import com.example.dqc.domain.model.QualitySnapshot;
import com.example.dqc.domain.model.Severity;

import java.util.List;

/**
 * Правило контроля качества данных. Чистая функция над снимком: без БД, без Spring, без системных часов.
 *
 * <p>Чтобы добавить правило, достаточно реализовать этот интерфейс (удобнее — унаследовать
 * {@link BaseQualityRule}) и завести строку с тем же кодом и важностью в справочнике {@code kkd_rule}.
 */
public interface QualityRule {

    /** Уникальный код правила, например {@code GOAL_NO_OWNER}; совпадает с {@code kkd_rule.code}. */
    String code();

    Severity severity();

    /** Возвращает находки по снимку; не более одной находки на объект. */
    List<Finding> check(QualitySnapshot snapshot);
}
