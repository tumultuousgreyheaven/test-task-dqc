package com.example.dqc.application.model;

/**
 * Параметры поиска проблем в том виде, как они пришли в запросе: всё необязательное, всё строки и числа.
 * Разбор и проверка значений выполняются в сервисе, а не в контроллере.
 *
 * @param runId  null — последний завершённый прогон
 * @param unitId null — все подразделения; иначе подразделение вместе с дочерними
 * @param rule   код правила
 * @param severity код важности: critical, major или minor
 * @param limit  null — значение по умолчанию
 * @param offset null — 0
 */
public record IssueSearchRequest(
        String runId, String unitId, String rule, String severity, Integer limit, Integer offset) {
}
