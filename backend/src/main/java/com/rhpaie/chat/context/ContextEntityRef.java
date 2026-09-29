package com.rhpaie.chat.context;

/**
 * Lightweight reference to an item shown in a READ list (display index → business id).
 * Stored for future READ follow-ups only — never used for writes in Step 7.
 */
public record ContextEntityRef(
        int displayIndex,
        String entityType,
        long entityId
) {
}
