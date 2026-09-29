package com.rhpaie.chat.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryConversationContextStoreTest {

    private InMemoryConversationContextStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryConversationContextStore();
    }

    @Test
    void isolatesUsers() {
        ConversationContext a = new ConversationContext();
        a.setDomain(ChatReadDomain.LEAVE);
        ConversationContext b = new ConversationContext();
        b.setDomain(ChatReadDomain.CONTRACT);

        store.save(new ContextKey(1L), a);
        store.save(new ContextKey(2L), b);

        assertEquals(ChatReadDomain.LEAVE, store.get(new ContextKey(1L)).orElseThrow().getDomain());
        assertEquals(ChatReadDomain.CONTRACT, store.get(new ContextKey(2L)).orElseThrow().getDomain());
    }

    @Test
    void expiredContextIsCleared() {
        ConversationContext context = new ConversationContext();
        context.setDomain(ChatReadDomain.LEAVE);
        context.setUpdatedAt(Instant.now().minus(InMemoryConversationContextStore.TTL).minusSeconds(5));
        store.save(new ContextKey(9L), context);
        // save() touches updatedAt — overwrite after save via clear+put path:
        store.clear(new ContextKey(9L));
        ConversationContext expired = new ConversationContext();
        expired.setDomain(ChatReadDomain.LEAVE);
        expired.setUpdatedAt(Instant.now().minus(InMemoryConversationContextStore.TTL).minusSeconds(5));
        // Bypass save().touch() by using clear then direct map — use reflection-free hack:
        // save then manually set expired on a new store API: re-get and we need put without touch.
        // Use save then replace by storing with expired timestamp through a second clear/save cycle
        // that doesn't touch — add package helper:
        store.putExpiredForTests(new ContextKey(9L), expired);
        assertTrue(store.get(new ContextKey(9L)).isEmpty());
    }

    @Test
    void getReturnsCopy() {
        ConversationContext context = new ConversationContext();
        context.setDomain(ChatReadDomain.LEAVE);
        store.save(new ContextKey(3L), context);

        ConversationContext loaded = store.get(new ContextKey(3L)).orElseThrow();
        loaded.setDomain(ChatReadDomain.CONTRACT);

        assertEquals(ChatReadDomain.LEAVE, store.get(new ContextKey(3L)).orElseThrow().getDomain());
    }
}
