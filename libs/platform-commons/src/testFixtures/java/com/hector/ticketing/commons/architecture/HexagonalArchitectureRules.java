package com.hector.ticketing.commons.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Hexagonal architecture rules from CLAUDE.md, enforced in every business service.
 *
 * <p>A service includes them with a test class annotated with {@code @AnalyzeClasses(packages =
 * "com.hector.ticketing.<service>")} and a field {@code @ArchTest static final ArchTests hexagonal =
 * ArchTests.in(HexagonalArchitectureRules.class)}.
 *
 * <p>Packages are matched with {@code com.hector.ticketing..} so a rule never confuses our {@code
 * domain} with a library's (e.g. Spring Data's {@code org.springframework.data.domain}). Every rule
 * allows an empty match: services that have not modeled any aggregate yet still pass.
 */
public final class HexagonalArchitectureRules {

    private static final String DOMAIN = "com.hector.ticketing..domain..";
    private static final String APPLICATION = "com.hector.ticketing..application..";

    private HexagonalArchitectureRules() {}

    // Includes the shared pure Java exceptions in com.hector.ticketing.commons.domain.
    @ArchTest
    static final ArchRule domainIsPureJava = classes()
            .that()
            .resideInAPackage(DOMAIN)
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage("java..", DOMAIN)
            .because("the domain must not depend on frameworks, application or infrastructure")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule applicationDependsOnlyOnDomain = classes()
            .that()
            .resideInAPackage(APPLICATION)
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage("java..", DOMAIN, APPLICATION)
            .because("use cases are plain Java wired from infrastructure/config, with no framework types")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule inputAdaptersDoNotUseOutputAdapters = noClasses()
            .that()
            .resideInAPackage("com.hector.ticketing..infrastructure.input..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.hector.ticketing..infrastructure.output..")
            .because("controllers and consumers go through use cases, never straight to persistence or Kafka")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule jpaEntitiesLiveInPersistence = classes()
            .that()
            .areAnnotatedWith("jakarta.persistence.Entity")
            .should()
            .resideInAPackage("com.hector.ticketing..infrastructure.output.persistence..")
            .because("JPA entities are separate from domain entities")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllersLiveInInputRest = classes()
            .that()
            .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .should()
            .resideInAPackage("com.hector.ticketing..infrastructure.input.rest..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule noFieldInjection = NO_CLASSES_SHOULD_USE_FIELD_INJECTION.allowEmptyShould(true);
}
