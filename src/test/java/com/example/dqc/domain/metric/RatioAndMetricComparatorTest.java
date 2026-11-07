package com.example.dqc.domain.metric;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RatioAndMetricComparatorTest {

    @Test
    void sumsNumeratorsAndDenominatorsInsteadOfAveragingPercentages() {
        Ratio smallUnit = new Ratio(1, 1);
        Ratio largeUnit = new Ratio(1, 3);

        Ratio total = Ratio.total(List.of(smallUnit, largeUnit));

        assertThat(total).isEqualTo(new Ratio(2, 4));
        assertThat(total.percent()).contains(new BigDecimal("50.0"));
    }

    @Test
    void validatesRatioBounds() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Ratio(-1, 2));
        assertThatIllegalArgumentException().isThrownBy(() -> new Ratio(3, 2));
        assertThatIllegalArgumentException().isThrownBy(() -> new Ratio(0, -1));
    }

    @Test
    void deltaUsesRoundedPercentagesAndReportsTrend() {
        // 13/23 = 56.521... -> 56.5%; 47/75 = 62.666... -> 62.7%.
        MetricReading reading = MetricComparator.compare(new Ratio(13, 23), new Ratio(47, 75), true);

        assertThat(reading.value()).isEqualByComparingTo("56.5");
        assertThat(reading.previous()).isEqualByComparingTo("62.7");
        assertThat(reading.delta()).isEqualByComparingTo("-6.2");
        assertThat(reading.trend()).isEqualTo(Trend.WORSE);
    }

    @Test
    void trendDirectionDependsOnWhetherHigherValuesAreBetter() {
        MetricReading fallingMetric = MetricComparator.compare(new Ratio(1, 4), new Ratio(1, 2), false);
        MetricReading unchangedMetric = MetricComparator.compare(new Ratio(1, 2), new Ratio(2, 4), true);

        assertThat(fallingMetric.trend()).isEqualTo(Trend.BETTER);
        assertThat(unchangedMetric.delta()).isEqualByComparingTo("0.0");
        assertThat(unchangedMetric.trend()).isEqualTo(Trend.SAME);
    }

    @Test
    void undefinedCurrentOrPreviousValueProducesNullComparisonFields() {
        MetricReading withoutPrevious = MetricComparator.compare(new Ratio(1, 2), null, true);
        MetricReading withoutCurrentObjects = MetricComparator.compare(Ratio.ZERO, new Ratio(1, 2), true);
        MetricReading bothUndefined = MetricComparator.compare(Ratio.ZERO, Ratio.ZERO, true);

        assertThat(withoutPrevious.value()).isEqualByComparingTo("50.0");
        assertThat(withoutPrevious.previous()).isNull();
        assertThat(withoutPrevious.delta()).isNull();
        assertThat(withoutPrevious.trend()).isNull();

        assertThat(withoutCurrentObjects.value()).isNull();
        assertThat(withoutCurrentObjects.previous()).isEqualByComparingTo("50.0");
        assertThat(withoutCurrentObjects.delta()).isNull();
        assertThat(withoutCurrentObjects.trend()).isNull();

        assertThat(bothUndefined.value()).isNull();
        assertThat(bothUndefined.previous()).isNull();
        assertThat(bothUndefined.delta()).isNull();
        assertThat(bothUndefined.trend()).isNull();
    }
}
