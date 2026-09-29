package com.rhpaie.chat.service;

import java.time.LocalDate;

/**
 * Isolates natural-language leave date/duration understanding from draft mutation.
 */
public interface LeaveDateExtractor {

    DateExtractionResult extract(String message, LocalDate referenceDate);
}
