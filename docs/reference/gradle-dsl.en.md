# Gradle plugin DSL

[한국어](gradle-dsl.md)

```kotlin
plugins {
    id("io.github.leejaehyeok97.config-guard") version "0.1.1"
}

configGuard {
    profiles.set(listOf("dev", "prod"))                 // required
    envFiles.put("prod", file("deploy/prod.env.keys"))  // optional
    failOnError.set(true)                               // default true
    failOnWarning.set(false)                            // default false
    ignoreKeys.set(listOf("legacy.*"))
    extraFrameworkPrefixes.set(listOf("resilience4j"))
}
```

```bash
./gradlew configGuardCheck
```

## Properties

| Property | Type | Default | Description |
|---|---|---|---|
| `profiles` | `ListProperty<String>` | (required) | Profiles to check. The task fails if empty |
| `envFiles` | `MapProperty<String, File>` | empty | Profile → file listing environment variable names. Profiles without a file skip environment variable checks |
| `failOnError` | `Property<Boolean>` | `true` | Fail the task if there is an ERROR |
| `failOnWarning` | `Property<Boolean>` | `false` | Fail the task if there is a WARNING |
| `ignoreKeys` | `ListProperty<String>` | empty | Key globs excluded from every finding. `*` matches any string, including dots |
| `extraFrameworkPrefixes` | `ListProperty<String>` | empty | Additional prefixes excluded from CG004 (unused key) |

Mapping to CLI options: `profiles` ↔ `--profile`, `envFiles` ↔ `--env-file`, `ignoreKeys` ↔ `--ignore`, `extraFrameworkPrefixes` ↔ `--framework-prefix`, `failOnError`/`failOnWarning` ↔ `--fail-on`. For the same input, `report.json` is identical to the CLI's `--format json` output (covered by tests).

## Environment variable name list

An `envFiles` entry is a text file with one environment variable name per line, or a `.env` file whose values are ignored. Only the names are read.

```
DB_URL
PAYMENT_APIKEY
```

`PAYMENT_APIKEY` provides `payment.api-key`, following Spring Boot relaxed binding. Lines starting with `#` and blank lines are ignored, and an `export ` prefix is allowed. Details: [config sources](config-sources.md) (Korean).

## Task `configGuardCheck`

- Group: `verification`. **It is not attached to `check` automatically.** To attach it:

  ```kotlin
  tasks.check { dependsOn("configGuardCheck") }
  ```

- Inputs: `src/main/java/**/*.java`, `src/main/resources/application*`, `src/main/resources/config/application*`, the files in `envFiles`, and the properties above
- Outputs: `build/reports/config-guard/report.md` (for PR comments), `build/reports/config-guard/report.json` ([JSON report](json-report.md))
- Prints the report to the console in the [console format](../../README.md#example-output).
- Writes the reports first, then fails. This lets the GitHub Action comment even on a failed build.
- Incremental build: `UP-TO-DATE` when inputs are unchanged. It is a `@CacheableTask`, so it also uses the build cache. In that case the console report is not printed again; look in `build/reports/config-guard/`.
- Compatible with the configuration cache (`--configuration-cache`).

## Multiple modules

The plugin only looks at `src/main` of the project it is applied to. In a multi-module build, apply it to the Spring Boot application module. Reports go to that module's `build/reports/config-guard/`.
