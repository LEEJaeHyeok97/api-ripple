# config-guard

[![CI](https://github.com/LEEJaeHyeok97/api-ripple/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/LEEJaeHyeok97/api-ripple/actions/workflows/ci.yml)
[![e2e](https://github.com/LEEJaeHyeok97/api-ripple/actions/workflows/e2e.yml/badge.svg?branch=main)](https://github.com/LEEJaeHyeok97/api-ripple/actions/workflows/e2e.yml)
[![release](https://img.shields.io/github/v/release/LEEJaeHyeok97/api-ripple)](https://github.com/LEEJaeHyeok97/api-ripple/releases)
[![license](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

Spring Boot 애플리케이션의 **환경별 설정 누락을 배포 전에 CI에서 잡아내는** 정적 분석 도구. CLI, Gradle 플러그인, GitHub Action으로 쓴다.

## 해결하려는 문제

`PaymentClient`가 `@Value("${payment.api-key}")`로 API 키를 주입받는데, 이 키가 `application-dev.yml`에만 있고 `application-prod.yml`에는 없다. 컴파일, 테스트, 이미지 빌드, 배포가 모두 통과한다. 그리고 운영 서버에서 애플리케이션이 `Could not resolve placeholder 'payment.api-key'`를 내며 기동 중에 죽는다. 롤링 배포라면 새 파드가 계속 재시작되고, 누군가 로그를 열어 볼 때까지 원인이 드러나지 않는다. 문제는 코드가 아니라 **코드가 요구하는 키와 그 환경이 실제로 제공하는 키의 차이**이고, 이 차이를 배포 전에 확인하는 단계가 없다.

config-guard는 애플리케이션을 띄우지 않고 이 두 집합을 프로필마다 비교한다.

| 코드가 요구하는 키 | 프로필이 제공하는 키 |
|---|---|
| `@Value("${...}")`: 기본값 유무, 중첩 플레이스홀더 | `application.yml` ∪ `application-{profile}.yml` ∪ 멀티 도큐먼트 `on-profile` |
| `Environment.getProperty` / `getRequiredProperty` | `spring.profiles.include` / `group` (1단계) |
| `@ConfigurationProperties`: `@Validated` + `@NotBlank` 등 | 환경변수 이름 목록 (`PAYMENT_APIKEY` → `payment.api-key`) |
| `@ConditionalOnProperty` / `@Profile` 아래면 조건부 | 설정 값의 `${ENV}`가 요구하는 환경변수 |

키 이름만 다룬다. **설정 값은 읽거나 출력하지 않는다.**

## 30초 설치

### CLI

Java 17 이상이면 jar 하나로 돌아간다.

```bash
curl -LO https://github.com/LEEJaeHyeok97/api-ripple/releases/download/v0.1.0/config-guard-0.1.0-all.jar
java -jar config-guard-0.1.0-all.jar check /path/to/your-app --profile prod
```

### Gradle 플러그인

> 아직 Gradle Plugin Portal 배포 전이다. 아래 접힌 절차로 로컬 Maven 저장소에 먼저 배포해야 이 설정이 동작한다.

```kotlin
plugins {
    id("io.github.leejaehyeok97.config-guard") version "0.1.0"
}

configGuard {
    profiles.set(listOf("dev", "prod"))
    envFiles.put("prod", file("deploy/prod.env.keys"))  // 선택: prod에 주입되는 환경변수 이름 목록
}
```

```bash
./gradlew configGuardCheck
```

<details>
<summary>로컬 Maven 저장소에 배포하기 (Plugin Portal 배포 전까지)</summary>

```bash
git clone --branch v0.1.0 https://github.com/LEEJaeHyeok97/api-ripple.git && cd api-ripple
./gradlew :gradle-plugin:publishToMavenLocal -PreleaseVersion=0.1.0
```

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}
```

</details>

### GitHub Action

Gradle 플러그인을 적용한 프로젝트에서 `configGuardCheck`를 돌리고, PR에 결과 코멘트를 단다. 다시 실행하면 같은 코멘트를 갱신한다.

```yaml
permissions:
  contents: read
  pull-requests: write
steps:
  - uses: actions/checkout@v4
  - uses: actions/setup-java@v4
    with: { distribution: temurin, java-version: "21" }
  - uses: LEEJaeHyeok97/api-ripple/action@v0
```

## 출력 예시

[samples/demo](samples/demo)(일부러 운영 설정을 빠뜨린 Spring Boot 앱)의 실제 출력:

```
config-guard: dev
  문제 없음
config-guard: prod
  ERROR  payment.api-key       PaymentClient.java:14  @Value, 기본값 없음
  ERROR  env DB_URL            application-prod.yml:7  spring.datasource.url이 요구
  WARN   alarm.push.fcm-key    FcmSender.java:13  @ConditionalOnProperty(alarm.push.enabled)
2 errors, 1 warning, 0 info
```

PR 코멘트는 프로필별 표로 나온다. 실제 예: [#17](https://github.com/LEEJaeHyeok97/api-ripple/pull/17).

| 규칙 | 심각도 | 잡는 것 |
|---|---|---|
| [CG001](docs/reference/rules/CG001.md) MISSING_KEY | ERROR | 기본값 없이 요구하는 키가 설정에도 환경변수에도 없다 |
| [CG002](docs/reference/rules/CG002.md) MISSING_ENV_VAR | ERROR | 설정 값의 `${ENV}`가 요구하는 환경변수가 목록에 없다 |
| [CG003](docs/reference/rules/CG003.md) CONDITIONAL_KEY | WARNING | 조건부 키가 없다 |
| [CG004](docs/reference/rules/CG004.md) UNUSED_KEY | INFO | 설정에 있는데 코드가 쓰지 않는 키 |
| [CG005](docs/reference/rules/CG005.md) UNANALYZABLE | INFO | SpEL, 동적 키처럼 분석하지 못하고 건너뛴 곳 |

`config-guard explain CG001`이 같은 규칙 문서를 터미널에 출력한다.

## AI 에이전트와 함께 쓰기

에이전트가 사람에게 묻지 않고 고칠 수 있게 만들었다.

```bash
java -jar config-guard-0.1.0-all.jar check . --profile prod --env-file prod=deploy/prod.env.keys --format json
```

- **안정적인 JSON**: stdout에는 JSON만 나온다. [스키마](schema/report.schema.json)(`schemaVersion: "1"`)를 공개한다.
- **수정 제안**: 모든 ERROR에 고칠 파일과 키(`ADD_YAML_KEY`) 또는 환경변수 이름(`ADD_ENV_VAR`)이 붙는다.
- **고정된 종료 코드**: 0 문제 없음, 1 기준 초과, 2 사용법 오류, 3 내부 오류.
- **[Agent Skill](skills/config-guard/SKILL.md)**: 언제 돌리고, 어떻게 고치고, 무엇을 하면 안 되는지(secret 하드코딩, ignore 남발).
- **[llms.txt](llms.txt)**: 저장소 문서 색인.

Skill 유무에 따른 에이전트 수정 결과는 [evals/RESULTS.md](evals/RESULTS.md)에 있다. 이 과제 세트에서는 성공률 차이가 없었다.

## 한계

- **정적 분석이다.** `spring.config.import`, Spring Cloud Config, Kubernetes ConfigMap, 명령행 인자처럼 이 도구가 읽지 않는 출처의 키는 모른다. `ignoreKeys`로 뺀다.
- **조건 값을 평가하지 않는다.** `@Profile("prod")` 아래의 키는 dev에서도 CG003 WARNING이 날 수 있다.
- **같은 파일 안의 상수까지만** 키를 해석한다. 그 밖은 CG005로 알리고 건너뛴다.
- **환경변수는 목록 파일에 적은 이름만** 안다. 목록이 실제 배포 환경과 다르면 결과도 다르다.
- 값의 유효성(URL 형식, 숫자 범위), Kotlin 소스, Maven 플러그인은 다루지 않는다.

## 문서

| 문서 | 내용 |
|---|---|
| [시작하기](docs/guide/getting-started.md) · [CI 연동](docs/guide/ci-integration.md) · [AI 에이전트와 쓰기](docs/guide/using-with-ai-agents.md) | 가이드 |
| [CLI](docs/reference/cli.md) · [Gradle DSL](docs/reference/gradle-dsl.md) · [JSON 리포트](docs/reference/json-report.md) · [규칙](docs/reference/rules/) | 레퍼런스 |
| [키 정규화](docs/reference/key-normalization.md) · [설정 소스](docs/reference/config-sources.md) · [키 추출](docs/reference/key-extraction.md) | 분석 규칙 |
| [CHANGELOG](CHANGELOG.md) · [릴리스 절차](docs/releasing.md) · [범위 밖 아이디어](docs/ideas.md) | 프로젝트 |

## 개발 방식

이 프로젝트는 [명세](docs/spec.md)를 기준으로 LLM(Claude Code)과 함께 개발했다. 마일스톤마다 브랜치와 PR을 거쳤고, main은 CI(JDK 17·21 테스트, Spotless, Error Prone + NullAway, JaCoCo core 90%, e2e)를 통과해야 머지된다.

- [CLAUDE.md](CLAUDE.md): LLM이 이 저장소에서 작업할 때 따르는 규칙
- [설계 결정](docs/decisions.md): 명세가 모호했던 부분과 정한 해석. 기준은 "Spring Boot 실제 동작과 같게"
- [회고](docs/retrospective/spec-driven-dev.md): LLM이 명세를 잘못 이해했던 사례와, 명세를 어떻게 고쳤어야 했는지

다음 할 일: Gradle Plugin Portal 배포.

## License

[MIT](LICENSE)
