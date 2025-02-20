package com.en;

import com.en.table.BuildModules;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

public class FindBuildModulesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindBuildModules());
    }

    @SuppressWarnings("GroovyUnusedAssignment")
    @Test
    void buildModules() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        rewriteRun(
          spec -> spec.dataTable(BuildModules.Row.class, rows -> {
              latch.countDown();
              assertThat(rows).containsExactlyInAnyOrder(
                new BuildModules.Row("maven", true, true, true, false),
                new BuildModules.Row("docker", true, true, false, true)
              );
          }),
          //language=groovy
          groovy(
            """
              List phasesToRun = [
                [
                  moduleType: "maven",
                  branchPattern: ".*"
                ],
                [
                  moduleType: "docker",
                  branchPattern: /^(feature|feature|release).*$/,
                  isProductionDeployment: true
                ]
              ]
              """,
            spec -> spec.path("Jenkinsfile")
          )
        );

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
    }
}
