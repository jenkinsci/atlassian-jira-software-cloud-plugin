package com.atlassian.jira.cloud.jenkins;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.Test;

/** Enforces rules using [ArchUnit](https://www.archunit.org). */
public class ArchUnitTest {

    private JavaClasses classes =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DONT_INCLUDE_ARCHIVES)
                    .withImportOption(ImportOption.Predefined.DONT_INCLUDE_JARS)
                    .withImportOption(ImportOption.Predefined.DONT_INCLUDE_TESTS)
                    .importPackages("com.atlassian.jira.cloud.jenkins");

    @Test
    public void testNoPackageCycles() {
        SlicesRuleDefinition.slices()
                .matching("com.atlassian.jira.cloud.jenkins.(**)")
                .should()
                .beFreeOfCycles()
                .check(classes);
    }
}
