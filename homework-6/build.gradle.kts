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
// Line coverage must be >= 80%. The gate is enforced at PUSH time (Phase 3 pre-push hook),
// NOT on every local build — so day-to-day `build`/`run` stay green.
//
// koverVerify only fails the build when invoked with -PenforceCoverage (used by the pre-push
// hook). Without the flag it is SKIPPED, so it never blocks a plain `build`/`run`.
//
// The standalone CLI entrypoints (the `*Kt` files holding only `fun main`) are excluded from
// coverage: they are thin wrappers whose underlying logic (process/summarize/run) is unit- and
// integration-tested. This keeps the gate focused on real logic.
kover {
    reports {
        filters {
            excludes {
                classes(
                    "homework6.agent.TransactionValidatorKt",
                    "homework6.agent.FraudDetectorKt",
                    "homework6.agent.ReportingAgentKt",
                    "homework6.agent.IntegratorKt",
                )
            }
        }
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
