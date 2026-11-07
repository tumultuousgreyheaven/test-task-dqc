package com.example.dqc.application.model;

import java.util.Objects;

/** Строка списка прогонов: прогон и число найденных в нём проблем. */
public record RunListItem(Run run, int issueCount) {

    public RunListItem {
        Objects.requireNonNull(run, "run");
    }
}
