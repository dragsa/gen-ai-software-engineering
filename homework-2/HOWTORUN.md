# ▶️ How to Run the application

> **Status:** module scaffolded only. No application sources have been created yet, so there is nothing runnable. This file will be updated automatically once the application is implemented.

## Prerequisites

- JDK 21+ installed

## Module wiring

The `homework-2` Gradle subproject is registered in the root `settings.gradle.kts` and uses the same dependency stack as `homework-1` (Kotlin JVM, Kotlinx Serialization, Ktor server + content negotiation + Swagger).

Once sources exist under `homework-2/src/main/kotlin`, the application will be runnable from the repository root with:

```bash
./gradlew :homework-2:run
```

And tests will be runnable with:

```bash
./gradlew :homework-2:clean :homework-2:test --rerun-tasks --console=plain
```

## Verify the module is recognized by Gradle

From the repository root:

```bash
./gradlew projects
```

`homework-2` must appear in the project list.
