package com.rhpaie.chat.context;

/**
 * Conversation context is scoped to the authenticated user (no widget session id yet).
 */
public record ContextKey(long userId) {
}
