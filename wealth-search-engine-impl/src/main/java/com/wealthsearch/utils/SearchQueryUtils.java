package com.wealthsearch.utils;

import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Utility methods for grooming user-provided search queries before processing.
 */
public final class SearchQueryUtils {

    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("[\\p{Cntrl}\\p{Cf}]+");
    private static final Pattern QUOTES = Pattern.compile("[\"«»“”„‟‹›]");
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern DISALLOWED = Pattern.compile("[^\\p{L}\\p{N}]");
    private static final Pattern MULTI_WHITESPACE = Pattern.compile("\\s+");

    private SearchQueryUtils() {}

    public static String normalize(@NotNull String rawQuery) {

        String sanitized = stripUnsupportedCharacters(rawQuery);
        sanitized = CONTROL_CHARACTERS.matcher(sanitized)
                                      .replaceAll(" ");
        sanitized = QUOTES.matcher(sanitized)
                          .replaceAll(" ");

        String normalized = Normalizer.normalize(sanitized, Normalizer.Form.NFD);
        normalized = COMBINING_MARKS.matcher(normalized)
                                    .replaceAll("");
        normalized = normalized.toLowerCase(Locale.ROOT);
        normalized = DISALLOWED.matcher(normalized)
                               .replaceAll(" ");
        normalized = MULTI_WHITESPACE.matcher(normalized)
                                     .replaceAll(" ")
                                     .trim();

        return normalized;
    }

    private static String stripUnsupportedCharacters(String input) {
        StringBuilder builder = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            int type = Character.getType(current);
            if (type == Character.SURROGATE || type == Character.PRIVATE_USE || type == Character.UNASSIGNED) {
                continue;
            }
            builder.append(current);
        }
        return builder.toString();
    }

    public static String removeSpaces(String query) {
        return query.replaceAll(" ", "");
    }

    public static void collectTerms(List<String> synonyms, Set<String> terms) {
        terms.addAll(synonyms.stream()
                             .filter(StringUtils::isNoneBlank)
                             .toList());
    }
}
