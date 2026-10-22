package com.hector.ticketing.reservation;

import com.hector.ticketing.commons.architecture.HexagonalArchitectureRules;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

/** Fails the build if production code breaks the hexagonal rules (see {@link HexagonalArchitectureRules}). */
@AnalyzeClasses(packages = "com.hector.ticketing.reservation", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchTests hexagonal = ArchTests.in(HexagonalArchitectureRules.class);
}
