package com.rhpaie.chat.service.eval;

import java.util.List;

/**
 * DEV robustness cases — failures here may be inspected during later work.
 * <p>
 * Source resource: {@code leave-date-eval/robustness-dev.json}
 */
public final class LeaveDateRobustnessDevCases {

    public static final String RESOURCE = "leave-date-eval/robustness-dev.json";

    private LeaveDateRobustnessDevCases() {
    }

    public static List<DateEvalCase> all() {
        return LeaveDateRobustnessCaseLoader.load(RESOURCE);
    }
}
