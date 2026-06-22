plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    // Default demo: in-process orchestration via the integrator's main().
    mainClass.set("homework6.agent.IntegratorKt")
}

// Resolve sample-transactions.json and shared/ relative to the subproject directory.
tasks.named<JavaExec>("run") {
    workingDir = projectDir
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlin.test.junit)
}

// --- Per-agent standalone run tasks (separate-execution demo) ---------------
// Each runtime agent also exposes its own main(), so it can run as a separate
// JVM process that communicates only through the shared/ JSON directories.
// Pass arguments with -Pargs="--dry-run".
val agentMains = mapOf(
    "runValidator" to "homework6.agent.TransactionValidatorKt",
    "runFraudDetector" to "homework6.agent.FraudDetectorKt",
    "runReporting" to "homework6.agent.ReportingAgentKt",
)
agentMains.forEach { (taskName, agentMainClass) ->
    tasks.register<JavaExec>(taskName) {
        group = "application"
        description = "Run ${agentMainClass.substringAfterLast('.')} as a standalone process"
        mainClass.set(agentMainClass)
        classpath = sourceSets["main"].runtimeClasspath
        workingDir = projectDir
        if (project.hasProperty("args")) {
            args((project.property("args") as String).split(" "))
        }
    }
}

// --- Coverage gate ----------------------------------------------------------
// Line coverage must be >= 80%. The gate is enforced at PUSH time (Phase 3 pre-push hook)
// and in CI, NOT on every local build — so day-to-day `build`/`run` stay green while the
// test suite is still being written (Phase 5).
//
// koverVerify only fails the build when invoked with -PenforceCoverage (used by the pre-push
// hook and CI). Without the flag it is SKIPPED, so it never blocks a plain `build`/`run`.
kover {
    reports {
        verify {
            rule {
                minBound(80)
            }
        }
    }
}

tasks.named("koverVerify") {
    onlyIf { project.hasProperty("enforceCoverage") }
}

tasks.withType<Test>().configureEach {
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
