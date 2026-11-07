package com.example.dqc.application.model;

/** Сколько проблем появилось, осталось и исправлено относительно прошлого прогона. */
public record DeltaCounts(int newIssues, int persisting, int resolved) {
}
