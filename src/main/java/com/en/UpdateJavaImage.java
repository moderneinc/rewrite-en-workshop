package com.en;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateJavaImage extends Recipe {

    @Option(displayName = "Java version",
            description = "The Java version to update to.",
            example = "17")
    Integer javaVersion;

    @Option(displayName = "New version",
            description = "An exact version number or node-style semver selector used to select the version number. " +
                          "You can also use `latest.release` for the latest available version and `latest.patch` if " +
                          "the current version is a valid semantic version. For more details, you can look at the documentation " +
                          "page of [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors)",
            example = "29.X")
    String imageVersion;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                          "Setting 'newVersion' to \"1.x\" can be paired with a metadata pattern of \"-dev\" to select `1.2.0-dev`.",
            example = "-dev",
            required = false)
    @Nullable
    String versionPattern;

    @Option(displayName = "Bearer token",
            description = "Used to authenticate with the registry to retrieve tag versions.")
    String bearerToken;

    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Update the base Java docker image";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Update to either `latest` or a specific version.";
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (imageVersion != null) {
            validated = validated.and(Semver.validate(imageVersion, versionPattern));
        }
        return validated;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        VersionComparator versionComparator =
                requireNonNull(Semver.validate(imageVersion, versionPattern).getValue());

        return Preconditions.check(new FindSourceFiles("**/Jenkinsfile"), new GroovyIsoVisitor<ExecutionContext>() {
            @Override
            public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                G.CompilationUnit c = super.visitCompilationUnit(cu, ctx);

                if (!getCursor().getMessage("hasDefaultJavaImage", false)) {
                    // Add the assignment if there isn't one at all
                    c = c.withStatements(ListUtils.concat(
                            buildJavaImageAssignment(ctx),
                            ListUtils.mapFirst(c.getStatements(), s -> s.withPrefix(s.getPrefix().withWhitespace("\n")))
                    ));
                }

                return c;
            }

            private Statement buildJavaImageAssignment(ExecutionContext ctx) {
                G.CompilationUnit g = (G.CompilationUnit) GroovyParser.builder()
                        .build()
                        .parse(String.format("env.CNP_DEFAULT_JAVA_IMAGE = \"cnp/cnp-docker-maven-java%d:%s\"",
                                javaVersion, getImageVersion(null, ctx)))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Unable to parse"));
                return g.getStatements().get(0);
            }


            @Override
            public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                J.Assignment assign = getCursor().firstEnclosing(J.Assignment.class);
                if (assign != null &&
                    assign.getVariable().printTrimmed(getCursor()).contains("CNP_DEFAULT_JAVA_IMAGE") &&
                    literal.getValue() instanceof String) {

                    getCursor().putMessageOnFirstEnclosing(G.CompilationUnit.class,
                            "hasDefaultJavaImage", true);

                    String currentImage = (String) literal.getValue();
                    String currentVersion = currentImage.substring(currentImage.lastIndexOf(':') + 1);
                    if (currentImage.startsWith("cnp/cnp-docker-maven-java" + javaVersion)) {
                        String newImage = currentImage.replaceAll(":.*$", ":" +
                                                                          getImageVersion(currentVersion, ctx));
                        if (!newImage.equals(currentImage)) {
                            return literal
                                    .withValue(newImage)
                                    .withValueSource("\"" + newImage + "\"");
                        }
                    }
                }
                return literal;
            }

            private String getImageVersion(@Nullable String currentVersion, ExecutionContext ctx) {
                HttpSender httpSender = HttpSenderExecutionContextView.view(ctx).getHttpSender();

                List<String> availableTags = QuayTags.getAvailableTags("cnp/cnp-docker-maven-java" + javaVersion, httpSender);
                Map<String, String> mostRecentByMajorMinorPatch = new HashMap<>();
                for (String tag : availableTags) {
                    String[] parts = tag.split("_", 2);
                    if (parts.length > 1) {
                        mostRecentByMajorMinorPatch.put(parts[0], tag);
                    } else {
                        mostRecentByMajorMinorPatch.put(tag, tag);
                    }
                }

                return versionComparator.upgrade(currentVersion == null ? "0" : currentVersion, mostRecentByMajorMinorPatch.keySet())
                        .map(mostRecentByMajorMinorPatch::get)
                        .orElse(currentVersion == null ? "latest" : currentVersion);
            }
        });
    }
}
