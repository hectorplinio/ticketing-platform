// Technical building blocks shared by the business services: base exceptions, error handling,
// JPA base entity and the Kafka health indicator. Never business/domain code of a service.
plugins {
    `java-library`
    // testFixtures: test code other modules reuse in their tests (ArchUnit rules), never shipped in the jar
    `java-test-fixtures`
    id("io.spring.dependency-management") version "1.1.7"
}

description = "platform-commons"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

// Same Spring Boot BOM as the services, so every library resolves to the same version.
dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:4.1.1")
    }
}

dependencies {
    // compileOnly: the library compiles against these but does not add them to a service.
    // Each service brings its own starters; auto-configurations activate only for what is present
    // (e.g. notification has no JPA).
    compileOnly("org.springframework.boot:spring-boot-starter-actuator")
    compileOnly("org.springframework.boot:spring-boot-starter-data-jpa")
    compileOnly("org.springframework.boot:spring-boot-starter-kafka")
    compileOnly("org.springframework.boot:spring-boot-starter-webmvc")

    testFixturesApi("com.tngtech.archunit:archunit-junit5:1.5.1")

    testImplementation("org.springframework.boot:spring-boot-starter-actuator")
    testImplementation("org.springframework.boot:spring-boot-starter-kafka")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // DataIntegrityViolationException, to test the 409 handler
    testImplementation("org.springframework:spring-tx")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
