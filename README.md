# config-guard

[![CI](https://github.com/LEEJaeHyeok97/config-guard/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/LEEJaeHyeok97/config-guard/actions/workflows/ci.yml)
[![e2e](https://github.com/LEEJaeHyeok97/config-guard/actions/workflows/e2e.yml/badge.svg?branch=main)](https://github.com/LEEJaeHyeok97/config-guard/actions/workflows/e2e.yml)
[![release](https://img.shields.io/github/v/release/LEEJaeHyeok97/config-guard)](https://github.com/LEEJaeHyeok97/config-guard/releases)
[![license](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

**English** | [한국어](README.ko.md)

A static analysis tool that **catches missing per-profile configuration in Spring Boot applications in CI, before deployment**. Use it as a CLI, a Gradle plugin, or a GitHub Action.

## The problem

`PaymentClient` injects an API key with `@Value("${payment.api-key}")`. The key exists in `application-dev.yml` but not in `application-prod.yml`. Compilation, tests, the image build and the deployment all pass. Then, on the production server, the application dies during startup with `Could not resolve placeholder 'payment.api-key'`. With a rolling deployment, new pods keep restarting, and nobody knows why until someone opens the logs. The problem is not the code but **the gap between the keys the code requires and the keys that environment actually provides**, and there is no step that checks this gap before deployment.

config-guard compares these two sets for each profile, without starting the application.

| Keys the code requires | Keys a profile provides |
|---|---|
| `@Value("${...}")`: with or without defaults, nested placeholders | `application.yml` ∪ `application-{profile}.yml` ∪ multi-document `on-profile` |
| `Environment.getProperty` / `getRequiredProperty` | `spring.profiles.include` / `group` (one level) |
| `@ConfigurationProperties`: `@Validated` + `@NotBlank` etc. | A list of environment variable names (`PAYMENT_APIKEY` → `payment.api-key`) |
| Conditional when under `@ConditionalOnProperty` / `@Profile` | Environment variables required by `${ENV}` in config values |

It works with key names only. **It never reads or prints configuration values.**

## 30-second install

### CLI

Runs as a single jar on Java 17 or later.

```bash
curl -LO https://github.com/LEEJaeHyeok97/config-guard/releases/download/v0.1.1/config-guard-0.1.1-all.jar
java -jar config-guard-0.1.1-all.jar check /path/to/your-app --profile prod
```

### Gradle plugin

```kotlin
plugins {
    id("io.github.leejaehyeok97.config-guard") version "0.1.1"
}

configGuard {
    profiles.set(listOf("dev", "prod"))
    envFiles.put("prod", file("deploy/prod.env.keys"))  // optional: names of the environment variables injected in prod
}
```

```bash
./gradlew configGuardCheck
```

The task writes `build/reports/config-guard/report.md` and `report.json`, and fails the build when there are errors. All options: [Gradle DSL reference](docs/reference/gradle-dsl.en.md).

### GitHub Action

Runs `configGuardCheck` in a project that applies the Gradle plugin and posts the result as a PR comment. Re-runs update the same comment.

```yaml
permissions:
  contents: read
  pull-requests: write
steps:
  - uses: actions/checkout@v4
  - uses: actions/setup-java@v4
    with: { distribution: temurin, java-version: "21" }
  - uses: LEEJaeHyeok97/config-guard/action@v0
```

## Example output

Actual output for [samples/demo](samples/demo), a Spring Boot app that deliberately omits production configuration. 

```
config-guard: dev
  문제 없음
config-guard: prod
  ERROR  payment.api-key       PaymentClient.java:14  @Value, 기본값 없음
  ERROR  env DB_URL            application-prod.yml:7  spring.datasource.url이 요구
  WARN   alarm.push.fcm-key    FcmSender.java:13  @ConditionalOnProperty(alarm.push.enabled)
2 errors, 1 warning, 0 info
```

Report messages are currently in Korean; rule IDs, keys and locations are language-neutral. Above: `문제 없음` = no problems, `기본값 없음` = no default value, `spring.datasource.url이 요구` = required by `spring.datasource.url`.

The PR comment shows a table per profile. A real example: [#17](https://github.com/LEEJaeHyeok97/config-guard/pull/17).

| Rule | Severity | Catches |
|---|---|---|
| [CG001](docs/reference/rules/CG001.md) MISSING_KEY | ERROR | A key required without a default is in neither the config nor the environment variables |
| [CG002](docs/reference/rules/CG002.md) MISSING_ENV_VAR | ERROR | An environment variable required by `${ENV}` in a config value is not in the list |
| [CG003](docs/reference/rules/CG003.md) CONDITIONAL_KEY | WARNING | A conditional key is missing |
| [CG004](docs/reference/rules/CG004.md) UNUSED_KEY | INFO | A key is in the config but no code uses it |
| [CG005](docs/reference/rules/CG005.md) UNANALYZABLE | INFO | A place that could not be analyzed and was skipped, such as SpEL or dynamic keys |

`config-guard explain CG001` prints the same rule document in the terminal.

## Using with AI agents

Built so that an agent can fix problems without asking a human.

```bash
java -jar config-guard-0.1.1-all.jar check . --profile prod --env-file prod=deploy/prod.env.keys --format json
```

- **Stable JSON**: stdout contains only JSON. The [schema](schema/report.schema.json) (`schemaVersion: "1"`) is published.
- **Fix suggestions**: every ERROR comes with the file and key to add (`ADD_YAML_KEY`) or the environment variable name (`ADD_ENV_VAR`).
- **Fixed exit codes**: 0 no problems, 1 threshold exceeded, 2 usage error, 3 internal error.
- **[Agent Skill](skills/config-guard/SKILL.md)**: when to run it, how to fix findings, and what not to do (hardcoding secrets, overusing ignore).
- **[llms.txt](llms.txt)**: an index of the repository documentation.

Agent fix results with and without the Skill are in [evals/RESULTS.md](evals/RESULTS.md). On this task set, the success rate did not differ.

## Limitations

- **It is static analysis.** It does not know keys from sources it does not read, such as `spring.config.import`, Spring Cloud Config, Kubernetes ConfigMaps or command-line arguments. Exclude them with `ignoreKeys`.
- **It does not evaluate conditions.** A key under `@Profile("prod")` can produce a CG003 WARNING in dev as well.
- **It resolves keys only through constants in the same file.** Anything else is reported as CG005 and skipped.
- **It only knows the environment variables named in the list file.** If the list differs from the real deployment environment, so do the results.
- It does not cover value validity (URL format, numeric ranges), Kotlin sources, or a Maven plugin.

## Documentation

The guides and references below are written in Korean. English documentation currently covers this README and the [Gradle DSL reference](docs/reference/gradle-dsl.en.md).

| Document | Contents |
|---|---|
| [Getting started](docs/guide/getting-started.md) · [CI integration](docs/guide/ci-integration.md) · [Using with AI agents](docs/guide/using-with-ai-agents.md) | Guides |
| [CLI](docs/reference/cli.md) · [Gradle DSL](docs/reference/gradle-dsl.en.md) · [JSON report](docs/reference/json-report.md) · [Rules](docs/reference/rules/) | Reference |
| [Key normalization](docs/reference/key-normalization.md) · [Config sources](docs/reference/config-sources.md) · [Key extraction](docs/reference/key-extraction.md) | Analysis rules |
| [CHANGELOG](CHANGELOG.md) · [Release process](docs/releasing.md) · [Out-of-scope ideas](docs/ideas.md) | Project |

## How it was built

This project was developed with an LLM (Claude Code) against a [specification](docs/spec.md). Each milestone went through a branch and a PR, and a change merges into main only after passing CI (tests on JDK 17 and 21, Spotless, Error Prone + NullAway, JaCoCo 90% on core, e2e).

- [CLAUDE.md](CLAUDE.md): rules the LLM follows when working in this repository
- [Design decisions](docs/decisions.md): where the specification was ambiguous and how it was interpreted. The rule of thumb is "behave like Spring Boot actually does"
- [Retrospective](docs/retrospective/spec-driven-dev.md): cases where the LLM misread the specification, and how the specification should have been written

## License

[MIT](LICENSE)
