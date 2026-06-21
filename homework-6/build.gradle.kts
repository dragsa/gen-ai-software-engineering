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
    mainClass.set("homework6.IntegratorKt")
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
    "runValidator" to "homework6.TransactionValidatorKt",
    "runFraudDetector" to "homework6.FraudDetectorKt",
    "runReporting" to "homework6.ReportingAgentKt",
)
agentMains.forEach { (taskName, agentMainClass) ->
    tasks.register<JavaExec>(taskName) {
        group = "application"
        description = "Run ${agentMainClass.substringAfterLast('.')} as a standalone process"
        mainClass.set(agentMainClass)
        classpath = sourceSets["main"].runtimeClasspath
        if (project.hasProperty("args")) {
            args((project.property("args") as String).split(" "))
        }
    }
}

// --- Coverage gate ----------------------------------------------------------
// Mirrors the pre-push hook (Phase 3): line coverage must be >= 80%.
kover {
    reports {
        verify {
            rule {
                minBound(80)
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
