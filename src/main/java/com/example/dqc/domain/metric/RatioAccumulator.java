package com.example.dqc.domain.metric;

import java.util.HashMap;
import java.util.Map;

/** Накапливает "подошло / всего" по подразделениям. */
final class RatioAccumulator {

    private final Map<String, int[]> counts = new HashMap<>();

    void add(String unitId, boolean satisfied) {
        int[] pair = counts.computeIfAbsent(unitId, key -> new int[2]);
        if (satisfied) {
            pair[0]++;
        }
        pair[1]++;
    }

    Map<String, Ratio> result() {
        Map<String, Ratio> result = new HashMap<>();
        counts.forEach((unitId, pair) -> result.put(unitId, new Ratio(pair[0], pair[1])));
        return result;
    }
}
