import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    id("com.diffplug.spotless") version "8.8.0" apply false
}

allprojects {
    group = "com.hector.ticketing"
    version = "0.0.1-SNAPSHOT"
}

subprojects {
    plugins.withId("java") {
        apply(plugin = "com.diffplug.spotless")

        configure<SpotlessExtension> {
            java {
                palantirJavaFormat()
                removeUnusedImports()
                formatAnnotations()
                endWithNewline()
            }
        }
    }
}
