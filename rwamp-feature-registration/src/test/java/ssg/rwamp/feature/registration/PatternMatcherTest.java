package ssg.rwamp.feature.registration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PatternMatcherTest {

    @Test
    void exactMatch_sameString() {
        assertThat(PatternMatcher.matches("exact", "com.example.hello", "com.example.hello"))
                .isTrue();
    }

    @Test
    void exactMatch_differentString() {
        assertThat(PatternMatcher.matches("exact", "com.example.hello", "com.example.world"))
                .isFalse();
    }

    @Test
    void prefixMatch_matches() {
        assertThat(PatternMatcher.matches("prefix", "com.example.", "com.example.hello"))
                .isTrue();
    }

    @Test
    void prefixMatch_noMatch() {
        assertThat(PatternMatcher.matches("prefix", "com.example.", "com.test.hello"))
                .isFalse();
    }

    @Test
    void wildcard_singleStar() {
        assertThat(PatternMatcher.matches("wildcard", "com.example.*", "com.example.foo"))
                .isTrue();
    }

    @Test
    void wildcard_singleStar_noMatch() {
        assertThat(PatternMatcher.matches("wildcard", "com.example.*", "com.example.foo.bar"))
                .isFalse();
    }

    @Test
    void wildcard_multipleStars() {
        assertThat(PatternMatcher.matches("wildcard", "com.*.bar", "com.example.bar"))
                .isTrue();
    }

    @Test
    void wildcard_twoSegments() {
        assertThat(PatternMatcher.matches("wildcard", "com.*.*", "com.example.foo"))
                .isTrue();
    }

    @Test
    void wildcard_noMatch() {
        assertThat(PatternMatcher.matches("wildcard", "com.*.bar", "com.example.foo"))
                .isFalse();
    }

    @Test
    void unknownMatchType() {
        assertThat(PatternMatcher.matches("unknown", "com.example.*", "com.example.foo"))
                .isFalse();
    }
}
