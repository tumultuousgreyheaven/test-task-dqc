package com.example.dqc.application.model;

import com.example.dqc.domain.model.Issue;

import java.util.List;

/** Страница проблем с применёнными параметрами постраничности. */
public record IssuePage(List<Issue> items, long total, int limit, int offset) {

    public IssuePage {
        items = List.copyOf(items);
    }
}
