package com.rhpaie.chat.context;

import java.util.Optional;

public interface ConversationContextStore {

    Optional<ConversationContext> get(ContextKey key);

    void save(ContextKey key, ConversationContext context);

    void clear(ContextKey key);
}
