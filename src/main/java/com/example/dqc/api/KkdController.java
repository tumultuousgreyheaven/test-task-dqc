package com.example.dqc.api;

import com.example.dqc.api.dto.IssueDto;
import com.example.dqc.api.dto.PagedResponse;
import com.example.dqc.api.dto.SummaryResponse;
import com.example.dqc.application.model.IssuePage;
import com.example.dqc.application.model.IssueSearchRequest;
import com.example.dqc.application.service.IssueQueryService;
import com.example.dqc.application.service.SummaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Результаты ККД: сводка по подразделению и список проблем.
 * Все параметры объявлены необязательными намеренно: обязательность и допустимые значения проверяет сервис,
 * чтобы ошибки имели единый формат и одинаково работали без HTTP.
 */
@RestController
@RequestMapping("/api/kkd")
public class KkdController {

    private final SummaryService summaryService;
    private final IssueQueryService issueQueryService;

    public KkdController(SummaryService summaryService, IssueQueryService issueQueryService) {
        this.summaryService = summaryService;
        this.issueQueryService = issueQueryService;
    }

    @GetMapping("/summary")
    public SummaryResponse summary(
            @RequestParam(name = "run_id", required = false) String runId,
            @RequestParam(name = "unit_id", required = false) String unitId) {
        return SummaryResponse.from(summaryService.summarize(runId, unitId));
    }

    @GetMapping("/issues")
    public PagedResponse<IssueDto> issues(
            @RequestParam(name = "run_id", required = false) String runId,
            @RequestParam(name = "unit_id", required = false) String unitId,
            @RequestParam(name = "rule", required = false) String rule,
            @RequestParam(name = "severity", required = false) String severity,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "offset", required = false) Integer offset) {
        IssuePage page = issueQueryService.search(new IssueSearchRequest(runId, unitId, rule, severity, limit, offset));
        return new PagedResponse<>(page.items().stream().map(IssueDto::from).toList(),
                page.total(), page.limit(), page.offset());
    }
}
