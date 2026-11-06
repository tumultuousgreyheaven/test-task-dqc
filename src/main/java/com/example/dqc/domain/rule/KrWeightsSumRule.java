package com.example.dqc.domain.rule;

import com.example.dqc.domain.model.Finding;
import com.example.dqc.domain.model.Goal;
import com.example.dqc.domain.model.KeyResult;
import com.example.dqc.domain.model.QualitySnapshot;
import com.example.dqc.domain.model.Severity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * KR_WEIGHTS_SUM (major): сумма весов КР цели не равна 100%.
 *
 * <p>Сравнение строгое и точное ({@link BigDecimal#compareTo}), без допуска: 33.33 + 33.33 + 33.33 = 99.99 — нарушение.
 * Цели без КР не проверяются: это уже находка GOAL_NO_KR, дублировать её незачем.
 */
public final class KrWeightsSumRule extends BaseQualityRule {

    public static final String CODE = "KR_WEIGHTS_SUM";

    private static final BigDecimal EXPECTED_SUM = BigDecimal.valueOf(100);

    public KrWeightsSumRule() {
        super(CODE, Severity.MAJOR);
    }

    @Override
    public List<Finding> check(QualitySnapshot snapshot) {
        List<Finding> findings = new ArrayList<>();
        for (Goal goal : snapshot.activeGoals()) {
            List<KeyResult> keyResults = snapshot.keyResultsOf(goal.id());
            if (keyResults.isEmpty()) {
                continue;
            }
            BigDecimal sum = keyResults.stream()
                    .map(KeyResult::weight)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sum.compareTo(EXPECTED_SUM) != 0) {
                findings.add(goalFinding(goal,
                        "Сумма весов КР цели %s равна %s%%, а должна быть 100%%"
                                .formatted(goal.id(), sum.stripTrailingZeros().toPlainString())));
            }
        }
        return findings;
    }
}
