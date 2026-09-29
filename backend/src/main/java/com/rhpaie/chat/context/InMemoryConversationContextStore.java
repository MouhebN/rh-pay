package com.rhpaie.chat.context;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local JVM READ-context store keyed by authenticated user id.
 * Not shared across backend instances; TTL expires idle contexts.
 */
@Component
public class InMemoryConversationContextStore implements ConversationContextStore {

    static final Duration TTL = Duration.ofMinutes(30);

    private final ConcurrentHashMap<Long, ConversationContext> byUserId = new ConcurrentHashMap<>();

    @Override
    public Optional<ConversationContext> get(ContextKey key) {
        if (key == null) {
            return Optional.empty();
        }
        ConversationContext context = byUserId.get(key.userId());
        if (context == null) {
            return Optional.empty();
        }
        if (isExpired(context)) {
            byUserId.remove(key.userId(), context);
            return Optional.empty();
        }
        return Optional.of(context.copy());
    }

    @Override
    public void save(ContextKey key, ConversationContext context) {
        if (key == null || context == null) {
            return;
        }
        context.touch();
        byUserId.put(key.userId(), context.copy());
    }

    @Override
    public void clear(ContextKey key) {
        if (key != null) {
            byUserId.remove(key.userId());
        }
    }

    /** Test helper: store without refreshing {@code updatedAt}. */
    void putExpiredForTests(ContextKey key, ConversationContext context) {
        byUserId.put(key.userId(), context.copy());
    }

    private static boolean isExpired(ConversationContext context) {
        Instant updated = context.getUpdatedAt();
        return updated == null || updated.isBefore(Instant.now().minus(TTL));
    }
}
