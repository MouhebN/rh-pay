package com.rhpaie.chat.service.eval;

import java.util.List;

/**
 * HOLDOUT robustness cases — generalization estimate only.
 * <p>
 * Do NOT tune {@code JavaLeaveDateExtractor} (or production date parsing) against
 * individual failures from this set. Inspect DEV first when iterating.
 * <p>
 * Source resource: {@code leave-date-eval/robustness-holdout.json}
 */
public final class LeaveDateRobustnessHoldoutCases {

    public static final String RESOURCE = "leave-date-eval/robustness-holdout.json";

    private LeaveDateRobustnessHoldoutCases() {
    }

    public static List<DateEvalCase> all() {
        return LeaveDateRobustnessCaseLoader.load(RESOURCE);
    }
}
