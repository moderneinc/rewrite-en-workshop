package com.en;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.test.MockHttpSender;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.groovy.Assertions.groovy;

public class UpdateJavaImageTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        HttpSenderExecutionContextView ctx = HttpSenderExecutionContextView.view(new InMemoryExecutionContext());
        ctx.setHttpSender(new MockHttpSender(() -> requireNonNull(getClass().getResourceAsStream("/quay-response.json"))));
        spec.executionContext(ctx);
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
      1.x,1.1.0_50
      1.1.0,1.1.0_50
      """
    )
    void updateJavaImage(String imageVersion, String expectedImageVersion) {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaImage(17, imageVersion, null, "token")),
          groovy(
            //language=groovy
            """
              env.CNP_DEFAULT_JAVA_IMAGE = "cnp/cnp-docker-maven-java17:0.9.0"
              """,
            //language=groovy
            """
              env.CNP_DEFAULT_JAVA_IMAGE = "cnp/cnp-docker-maven-java17:%s"
              """.formatted(expectedImageVersion),
            spec -> spec.path("Jenkinsfile")
          )
        );
    }

    @SuppressWarnings("GroovyUnusedAssignment")
    @Test
    void addIfMissing() {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaImage(17, "latest.release", null, "token")),
          groovy(
            //language=groovy
            """
              List phasesToRun = []
              """,
            //language=groovy
            """
              env.CNP_DEFAULT_JAVA_IMAGE = "cnp/cnp-docker-maven-java17:1.1.0_50"
              List phasesToRun = []
              """,
            spec -> spec.path("Jenkinsfile")
          )
        );
    }
}
