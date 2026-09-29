package com.rhpaie.chat.context;

import com.rhpaie.chat.entity.ChatIntent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Short-lived structured READ context for follow-up resolution.
 * One active domain at a time; no free-form conversation transcript.
 */
public final class ConversationContext {

    private ChatReadDomain domain;
    private ChatIntent lastIntent;
    private final Map<String, String> filters = new LinkedHashMap<>();
    private final List<ContextEntityRef> lastEntities = new ArrayList<>();
    private Instant updatedAt = Instant.now();

    public ChatReadDomain getDomain() {
        return domain;
    }

    public void setDomain(ChatReadDomain domain) {
        this.domain = domain;
    }

    public ChatIntent getLastIntent() {
        return lastIntent;
    }

    public void setLastIntent(ChatIntent lastIntent) {
        this.lastIntent = lastIntent;
    }

    public Map<String, String> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, String> filters) {
        this.filters.clear();
        if (filters != null) {
            this.filters.putAll(filters);
        }
    }

    public List<ContextEntityRef> getLastEntities() {
        return lastEntities;
    }

    public void setLastEntities(List<ContextEntityRef> entities) {
        this.lastEntities.clear();
        if (entities != null) {
            this.lastEntities.addAll(entities);
        }
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public ConversationContext copy() {
        ConversationContext copy = new ConversationContext();
        copy.domain = this.domain;
        copy.lastIntent = this.lastIntent;
        copy.filters.putAll(this.filters);
        copy.lastEntities.addAll(this.lastEntities);
        copy.updatedAt = this.updatedAt;
        return copy;
    }
}
