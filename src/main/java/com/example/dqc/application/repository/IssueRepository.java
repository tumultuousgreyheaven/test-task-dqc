package com.example.dqc.application.repository;

import com.example.dqc.application.model.IssueFilter;
import com.example.dqc.application.model.PageResult;
import com.example.dqc.domain.model.Issue;

import java.util.Collection;
import java.util.List;

public interface IssueRepository {

    /** Сохраняет проблемы прогона одним пакетом. */
    void saveAll(String runId, Collection<Issue> issues);

    /** Все проблемы прогона, подходящие под фильтр. Порядок: важность, правило, объект. */
    List<Issue> find(String runId, IssueFilter filter);

    /** Страница проблем прогона в том же порядке и общее число подходящих. */
    PageResult<Issue> findPage(String runId, IssueFilter filter, int limit, int offset);
}
