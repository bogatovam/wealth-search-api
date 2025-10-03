package com.wealthsearch.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class SearchQueryUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "'Hello World', 'hello world'",
            "'UPPERCASE', 'uppercase'",
            "'MiXeD CaSe', 'mixed case'",
            "'  extra   spaces  ', 'extra spaces'",
            "'multiple     tabs', 'multiple tabs'",
            "'café', 'cafe'",
            "'naïve résumé', 'naive resume'",
            "'Zürich', 'zurich'",
            "'São Paulo', 'sao paulo'"
    })
    void normalizeHandlesBasicCases(String input, String expected) {
        String result = SearchQueryUtils.normalize(input);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void normalizeRemovesControlCharacters() {
        String input = "hello\u0000world\u0001test";
        String result = SearchQueryUtils.normalize(input);
        assertThat(result).isEqualTo("hello world test");
    }

    @Test
    void normalizeRemovesQuotes() {
        String input = "\"quoted\" \u00abguillemets\u00bb \u201ccurly\u201d \u201eGerman\u201f \u2039single\u203a";
        String result = SearchQueryUtils.normalize(input);
        assertThat(result).isEqualTo("quoted guillemets curly german single");
    }

    @Test
    void normalizeHandlesCombiningMarks() {
        // NFD decomposition creates combining marks
        String input = "é";  // Single character
        String result = SearchQueryUtils.normalize(input);
        assertThat(result).isEqualTo("e");
    }

    @Test
    void normalizeRemovesDisallowedCharacters() {
        String input = "hello@world.com & company-name_123";
        String result = SearchQueryUtils.normalize(input);
        assertThat(result).isEqualTo("hello world com company name 123");
    }

    @Test
    void normalizeRemovesSpecialSymbols() {
        String input = "test!@#$%^*()+=[]{}|\\:;,<>?/~`";
        String result = SearchQueryUtils.normalize(input);
        // Only @, ., _, ', &, - should remain
        assertThat(result).doesNotContain("!", "#", "$", "%", "^", "*", "(", ")", "+", "=");
    }

    @Test
    void normalizeCollapsesMultipleWhitespace() {
        String input = "hello    world   \t  test\n\nnewline";
        String result = SearchQueryUtils.normalize(input);
        assertThat(result).isEqualTo("hello world test newline");
    }

    @Test
    void normalizeTrimSpaces() {
        String input = "   leading and trailing   ";
        String result = SearchQueryUtils.normalize(input);
        assertThat(result).isEqualTo("leading and trailing");
    }

    @Test
    void normalizeHandlesEmptyString() {
        String result = SearchQueryUtils.normalize("");
        assertThat(result).isEmpty();
    }

    @Test
    void normalizeHandlesWhitespaceOnly() {
        String result = SearchQueryUtils.normalize("   \t  \n  ");
        assertThat(result).isEmpty();
    }

    @Test
    void normalizeHandlesSpecialCharactersOnly() {
        String result = SearchQueryUtils.normalize("!@#$%^&*()");
        assertThat(result).isEqualTo("");
    }

    @Test
    void normalizeStripsSurrogatePairs() {
        // Emoji (surrogate pair)
        String input = "hello 🌟 world";
        String result = SearchQueryUtils.normalize(input);
        assertThat(result).isEqualTo("hello world");
    }

    @Test
    void normalizeStripsPrivateUseCharacters() {
        String input = "test\uE000private\uF8FFuse";
        String result = SearchQueryUtils.normalize(input);
        assertThat(result).isEqualTo("testprivateuse");
    }

    @Test
    void normalizeHandlesRealWorldCompanyNames() {
        assertThat(SearchQueryUtils.normalize("Nevis Wealth Management")).isEqualTo("nevis wealth management");
        assertThat(SearchQueryUtils.normalize("J.P. Morgan & Co.")).isEqualTo("j p morgan co");
        assertThat(SearchQueryUtils.normalize("AT&T Inc.")).isEqualTo("at t inc");
        assertThat(SearchQueryUtils.normalize("Schröder & Associates")).isEqualTo("schroder associates");
    }

    @Test
    void normalizeHandlesEmailDomains() {
        assertThat(SearchQueryUtils.normalize("user@company.com")).isEqualTo("user company com");
        assertThat(SearchQueryUtils.normalize("ADMIN@NEVISWEALTH.COM")).isEqualTo("admin neviswealth com");
    }

    @Test
    void removeSpacesStripsAllWhitespace() {
        String input = "hello world test";
        String result = SearchQueryUtils.removeSpaces(input);
        assertThat(result).isEqualTo("helloworldtest");
    }

    @Test
    void removeSpacesHandlesEmptyString() {
        String result = SearchQueryUtils.removeSpaces("");
        assertThat(result).isEmpty();
    }

    @Test
    void removeSpacesHandlesNoSpaces() {
        String result = SearchQueryUtils.removeSpaces("nospaceshere");
        assertThat(result).isEqualTo("nospaceshere");
    }

    @Test
    void removeSpacesHandlesMultipleSpaces() {
        String result = SearchQueryUtils.removeSpaces("multiple   spaces   here");
        assertThat(result).isEqualTo("multiplespaceshere");
    }

    @Test
    void collectTermsAddsValidStrings() {
        List<String> source = Arrays.asList("term1", "term2", "term3");
        Set<String> target = new HashSet<>();

        SearchQueryUtils.collectTerms(source, target);

        assertThat(target).containsExactlyInAnyOrder("term1", "term2", "term3");
    }

    @Test
    void collectTermsIgnoresBlankStrings() {
        List<String> source = Arrays.asList("valid", "", "  ", "also-valid");
        Set<String> target = new HashSet<>();

        SearchQueryUtils.collectTerms(source, target);

        assertThat(target).containsExactlyInAnyOrder("valid", "also-valid");
    }

    @Test
    void collectTermsHandlesEmptyList() {
        List<String> source = Collections.emptyList();
        Set<String> target = new HashSet<>();

        SearchQueryUtils.collectTerms(source, target);

        assertThat(target).isEmpty();
    }

    @Test
    void collectTermsAppendsToExistingSet() {
        List<String> source = Arrays.asList("new1", "new2");
        Set<String> target = new HashSet<>(Arrays.asList("existing1", "existing2"));

        SearchQueryUtils.collectTerms(source, target);

        assertThat(target).containsExactlyInAnyOrder("existing1", "existing2", "new1", "new2");
    }

    @Test
    void collectTermsHandlesDuplicates() {
        List<String> source = Arrays.asList("term1", "term2", "term1");
        Set<String> target = new HashSet<>();

        SearchQueryUtils.collectTerms(source, target);

        assertThat(target).containsExactlyInAnyOrder("term1", "term2");
    }

    @Test
    void collectTermsHandlesNullElements() {
        List<String> source = Arrays.asList("valid", null, "also-valid");
        Set<String> target = new HashSet<>();

        // Note: isNoneBlank handles null by returning false, which filters it out
        SearchQueryUtils.collectTerms(source, target);

        assertThat(target).containsExactlyInAnyOrder("valid", "also-valid");
    }
}
