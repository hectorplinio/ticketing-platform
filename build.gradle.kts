plugins {
    base
    id("com.diffplug.spotless") version "8.8.0"
}

allprojects {
    group = "com.hector.ticketing"
    version = "0.0.1-SNAPSHOT"
}

repositories {
    mavenCentral()
}

spotless {
    java {
        target("services/**/src/**/*.java")
        palantirJavaFormat()
        removeUnusedImports()
        formatAnnotations()
        endWithNewline()
    }
}
