package com.rhpaie.chat.service;

/**
 * Where a leave date extraction came from.
 * {@link #OLLAMA} is reserved for a later step — unused in Step 2.
 */
public enum DateExtractionSource {
    JAVA_RULE,
    NONE,
    OLLAMA
}
