package com.example.demo.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Custom deserializer for LocalDateTime that can handle both date-only and
 * date-time formats
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    private static final Logger logger = LoggerFactory.getLogger(FlexibleLocalDateTimeDeserializer.class);

    // Handle ISO date-time format (with time)
    private final LocalDateTimeDeserializer dateTimeDeserializer = new LocalDateTimeDeserializer(
            DateTimeFormatter.ISO_DATE_TIME);

    // Additional formats to try if the standard deserializer fails
    private static final DateTimeFormatter[] EXTRA_FORMATTERS = new DateTimeFormatter[] {
            DateTimeFormatter.ISO_DATE, // For date-only strings like "2025-02-01"
            DateTimeFormatter.ofPattern("yyyy-MM-dd"), // Explicit date-only format
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"), // Without milliseconds
            DateTimeFormatter.ISO_LOCAL_DATE_TIME // Another standard format
    };

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String dateText = parser.getValueAsString();
        if (dateText == null || dateText.isEmpty()) {
            return null;
        }

        // First, try the standard ISO date-time deserializer
        try {
            return dateTimeDeserializer.deserialize(parser, context);
        } catch (Exception e) {
            // If that fails, try our additional formats
            logger.debug("Standard date-time parsing failed for '{}', trying alternative formats", dateText);

            // Try to parse as date-only (adding a default time)
            for (DateTimeFormatter formatter : EXTRA_FORMATTERS) {
                try {
                    if (dateText.length() <= 10) { // Probably date-only
                        return LocalDate.parse(dateText, formatter).atStartOfDay();
                    } else {
                        return LocalDateTime.parse(dateText, formatter);
                    }
                } catch (DateTimeParseException dtpe) {
                    // Continue to the next formatter
                    logger.trace("Failed to parse with formatter {}: {}", formatter, dtpe.getMessage());
                }
            }

            // If we get here, none of our attempts worked
            logger.warn("Could not parse date string: {}", dateText);
            throw new IOException("Unable to parse date text '" + dateText + "' in any supported format", e);
        }
    }
}