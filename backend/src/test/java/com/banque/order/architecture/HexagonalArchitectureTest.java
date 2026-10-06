package com.banque.order.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Garde-fou d'architecture (ArchUnit) : le build échoue si quelqu'un viole les règles de l'hexagone.
 * Très utile à citer en entretien.
 */
@AnalyzeClasses(packages = "com.banque.order", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule le_domaine_ne_depend_de_rien = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "..application..", "..infrastructure..");

    @ArchTest
    static final ArchRule l_application_ignore_l_infrastructure = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "..infrastructure..");
}
