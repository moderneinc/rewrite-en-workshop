plugins {
    id("org.openrewrite.build.recipe-library-base") version "latest.release"
    id("org.openrewrite.build.publish") version "latest.release"
    id("nebula.release") version "latest.release"
    id("org.openrewrite.build.recipe-repositories") version "latest.release"
}

// Set as appropriate for your organization
group = "com.en"
description = "Rewrite recipes."

dependencies {
    implementation(platform("org.openrewrite.recipe:rewrite-recipe-bom:latest.release"))

    implementation("org.openrewrite:rewrite-java")
    implementation("org.openrewrite:rewrite-groovy")
    runtimeOnly("org.openrewrite:rewrite-java-17")

    annotationProcessor("org.openrewrite:rewrite-templating:latest.release")
    implementation("org.openrewrite:rewrite-templating")
    compileOnly("com.google.errorprone:error_prone_core:latest.release") {
        exclude("com.google.auto.service", "auto-service-annotations")
    }

    // The RewriteTest class needed for testing recipes
    testImplementation("org.openrewrite:rewrite-test")

    // Need to have a slf4j binding to see any output enabled from the parser.
    runtimeOnly("ch.qos.logback:logback-classic:1.2.+")
}

configure<nebula.plugin.release.git.base.ReleasePluginExtension> {
    defaultVersionStrategy = nebula.plugin.release.NetflixOssStrategies.SNAPSHOT(project)
}

configure<PublishingExtension> {
    publications {
        named("nebula", MavenPublication::class.java) {
            suppressPomMetadataWarningsFor("runtimeElements")
        }
    }
}
