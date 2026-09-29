package com.rhpaie.chat.context;

import com.rhpaie.chat.entity.ChatIntent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Result of deterministic READ follow-up resolution.
 */
public record ResolvedReadFollowUp(
        ChatIntent intent,
        Map<String, String> filters,
        boolean contextUsed
) {
    public ResolvedReadFollowUp {
        filters = filters == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(filters));
    }

    public static ResolvedReadFollowUp of(ChatIntent intent, Map<String, String> filters) {
        return new ResolvedReadFollowUp(intent, filters, true);
    }

    public static ResolvedReadFollowUp of(ChatIntent intent) {
        return new ResolvedReadFollowUp(intent, Map.of(), true);
    }
}
