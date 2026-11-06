package com.example.dqc.domain.metric;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Optional;

/**
 * Доля в виде числителя и знаменателя. Хранить именно их, а не готовый процент, нужно потому, что
 * значение подразделения с дочерними — это сумма числителей, делённая на сумму знаменателей;
 * среднее процентов по дочерним было бы неверным.
 */
public record Ratio(int numerator, int denominator) {

    public static final Ratio ZERO = new Ratio(0, 0);

    /** Знаков после запятой в проценте. */
    public static final int PERCENT_SCALE = 1;

    public Ratio {
        if (numerator < 0 || denominator < 0 || numerator > denominator) {
            throw new IllegalArgumentException(
                    "Некорректная доля: %d/%d".formatted(numerator, denominator));
        }
    }

    public Ratio plus(Ratio other) {
        return new Ratio(numerator + other.numerator, denominator + other.denominator);
    }

    public static Ratio total(Collection<Ratio> ratios) {
        Ratio sum = ZERO;
        for (Ratio ratio : ratios) {
            sum = sum.plus(ratio);
        }
        return sum;
    }

    /** Значение определено, только если есть объекты, по которым оно считается. */
    public boolean isDefined() {
        return denominator > 0;
    }

    /** Процент с одним знаком после запятой (HALF_UP); пусто, если знаменатель нулевой. */
    public Optional<BigDecimal> percent() {
        if (!isDefined()) {
            return Optional.empty();
        }
        return Optional.of(BigDecimal.valueOf(numerator * 100L)
                .divide(BigDecimal.valueOf(denominator), PERCENT_SCALE, RoundingMode.HALF_UP));
    }
}
