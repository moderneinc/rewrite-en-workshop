package com.en;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.MockHttpSender;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

class QuayTagsTest {
    MockHttpSender httpSender = new MockHttpSender(() -> requireNonNull(getClass().getResourceAsStream("/quay-response.json")));

    @Test
    void getAvailableTags() {
        List<String> availableTags = QuayTags.getAvailableTags("cnp/cnp-maven-java17", httpSender, "token");
        assertThat(availableTags).contains("1.0.0_1");
    }
}
