# config-guard

[![CI](https://github.com/LEEJaeHyeok97/api-ripple/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/LEEJaeHyeok97/api-ripple/actions/workflows/ci.yml)
[![e2e](https://github.com/LEEJaeHyeok97/api-ripple/actions/workflows/e2e.yml/badge.svg?branch=main)](https://github.com/LEEJaeHyeok97/api-ripple/actions/workflows/e2e.yml)
[![status](https://img.shields.io/badge/status-0.1.0%20pre--release-orange)](#roadmap)
[![license](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

Spring Boot 애플리케이션의 **환경별 설정 누락을 배포 전에 CI에서 잡아내는** 정적 분석 도구. CLI, Gradle 플러그인, GitHub Action으로 쓴다.

## 해결하려는 문제

`PaymentClient`가 `@Value("${payment.api-key}")`로 API 키를 주입받는다고 하자. 개발자는 `application-dev.yml`에만 이 키를 넣고 `application-prod.yml`에는 빠뜨렸다. 컴파일, 테스트, 이미지 빌드, 배포 파이프라인이 모두 초록색으로 통과한다. 그런데 운영 서버에서 애플리케이션이 기동하다가 `Could not resolve placeholder 'payment.api-key'`를 내고 죽는다. 롤링 배포라면 새 파드가 계속 재시작되고, 누군가 로그를 열어 볼 때까지 원인이 드러나지 않는다. 문제는 코드가 아니라 **"코드가 요구하는 키"와 "그 환경이 실제로 제공하는 키"의 차이**에 있다. 그런데 이 차이를 배포 전에 확인하는 단계가 없다.

## 동작 방식

애플리케이션을 띄우지 않고, 두 집합을 프로필마다 정적으로 비교한다.

| 코드가 요구하는 키 | 프로필이 제공하는 키 |
|---|---|
| `@Value("${...}")` (기본값 유무, 중첩 플레이스홀더) | `application.yml` ∪ `application-{profile}.yml` ∪ 멀티 도큐먼트 `on-profile` |
| `Environment.getProperty` / `getRequiredProperty` | `spring.profiles.include` / `group` (1단계) |
| `@ConfigurationProperties` (`@Validated` + `@NotBlank` 등) | 환경변수 이름 목록 (relaxed binding: `PAYMENT_APIKEY` → `payment.api-key`) |
| `@ConditionalOnProperty` / `@Profile`이면 조건부 | 값 안의 `${ENV}`가 요구하는 환경변수 |

키 이름만 다룬다. **설정 값은 읽거나 출력하지 않는다.**

## 30초 설치

> 0.1.0은 아직 릴리스 전이다. 아래 "릴리스 전" 방법은 이 저장소를 받아서 쓴다. 릴리스 절차는 [docs/releasing.md](docs/releasing.md).

### CLI

```bash
# 릴리스 후: GitHub Releases에서 config-guard-<version>-all.jar를 받는다
# 릴리스 전: 소스에서 빌드
git clone https://github.com/LEEJaeHyeok97/api-ripple.git && cd api-ripple
./gradlew :cli:fatJar      # → cli/build/libs/config-guard-0.1.0-SNAPSHOT-all.jar

java -jar cli/build/libs/config-guard-0.1.0-SNAPSHOT-all.jar check /path/to/your-app --profile prod
```

### Gradle 플러그인

```bash
# 릴리스 전: 이 저장소에서 로컬 Maven 저장소로 배포
./gradlew :gradle-plugin:publishToMavenLocal
```

```kotlin
// settings.gradle.kts (릴리스 전에만 필요)
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

// build.gradle.kts
plugins {
    id("io.github.leejaehyeok97.config-guard") version "0.1.0-SNAPSHOT" // Plugin Portal 배포 후: "0.1.0"
}

configGuard {
    profiles.set(listOf("dev", "prod"))
    envFiles.put("prod", file("deploy/prod.env.keys"))  // 선택: prod 배포 환경의 환경변수 이름 목록
}
```

```bash
./gradlew configGuardCheck
```

### GitHub Action

```yaml
permissions:
  contents: read
  pull-requests: write
steps:
  - uses: actions/checkout@v4
  - uses: actions/setup-java@v4
    with: { distribution: temurin, java-version: "21" }
  - uses: LEEJaeHyeok97/api-ripple/action@main   # v0 태그 이후: @v0
    with:
      working-directory: .
```

PR에 결과 코멘트를 달고, 다시 실행하면 같은 코멘트를 갱신한다. 자세히: [CI 연동 가이드](docs/guide/ci-integration.md).

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

PR 코멘트(`report.md`)는 프로필별 표로 나오고, 문제가 없는 프로필은 `✅ prod: 문제 없음`, INFO는 접혀 있다. 이 저장소의 PR에서 e2e 잡이 실제로 단 코멘트를 볼 수 있다 (예: [#17](https://github.com/LEEJaeHyeok97/api-ripple/pull/17)).

## 규칙

| ID | 이름 | 심각도 | 잡는 것 |
|---|---|---|---|
| [CG001](docs/reference/rules/CG001.md) | MISSING_KEY | ERROR | 기본값 없이 요구하는 키가 설정에도 환경변수에도 없다 |
| [CG002](docs/reference/rules/CG002.md) | MISSING_ENV_VAR | ERROR | 설정 값의 `${ENV}`가 요구하는 환경변수가 목록에 없다 |
| [CG003](docs/reference/rules/CG003.md) | CONDITIONAL_KEY | WARNING | 조건부(`@ConditionalOnProperty`/`@Profile`) 키가 없다 |
| [CG004](docs/reference/rules/CG004.md) | UNUSED_KEY | INFO | 설정에 있는데 코드가 쓰지 않는 키 |
| [CG005](docs/reference/rules/CG005.md) | UNANALYZABLE | INFO | SpEL, 동적 키처럼 분석하지 못하고 건너뛴 곳 |

규칙 ID는 공개 후 의미를 바꾸지 않는다. `config-guard explain CG001`이 같은 문서를 터미널에 출력한다.

## AI 에이전트와 함께 쓰기

사람이 읽는 콘솔 출력과 별개로, 에이전트가 사람에게 묻지 않고 고칠 수 있게 만들었다.

```bash
config-guard check . --profile prod --env-file prod=deploy/prod.env.keys --format json
```

- **안정적인 JSON**: stdout에는 JSON만 나온다. 스키마 [`schema/report.schema.json`](schema/report.schema.json) (`schemaVersion: "1"`)
- **수정 제안**: 모든 ERROR에 `suggestions`(`ADD_YAML_KEY` + 고칠 파일, `ADD_ENV_VAR` + 환경변수 이름)가 있다
- **고정된 종료 코드**: 0 문제 없음, 1 기준 초과, 2 사용법 오류, 3 내부 오류
- **Agent Skill**: [`skills/config-guard/SKILL.md`](skills/config-guard/SKILL.md). 언제 돌리고, 어떻게 고치고, 무엇을 하면 안 되는지(secret 하드코딩, ignore 남발)
- **[`llms.txt`](llms.txt)**: 저장소 문서 색인

자세히: [AI 에이전트와 쓰기](docs/guide/using-with-ai-agents.md).

## 한계

- **정적 분석이다.** `spring.config.import`, Spring Cloud Config, Kubernetes ConfigMap, 명령행 인자처럼 이 도구가 읽지 않는 출처의 키는 모른다. `ignoreKeys`로 뺀다.
- **조건 값을 평가하지 않는다.** `@Profile("prod")`가 붙은 키는 dev에서도 CG003 WARNING이 날 수 있다.
- **같은 파일 안의 상수까지만** 키를 해석한다. SpEL, 다른 파일 상수, 변수로 만든 키는 CG005로 알리고 건너뛴다.
- **환경변수는 목록 파일에 적은 이름만** 안다. 목록이 실제 배포 환경과 다르면 결과도 다르다.
- `spring.config.activate.on-profile`의 `!`, `&`, 괄호 식은 지원하지 않는다 (CG005).
- Kotlin 소스, Maven 플러그인은 지원하지 않는다.
- 값의 유효성(URL 형식, 숫자 범위)은 보지 않는다. 키가 있는지만 본다.

## 설계 결정

판단이 애매한 규칙은 "Spring Boot 실제 동작과 같게"를 기준으로 정했고, 모두 [docs/decisions.md](docs/decisions.md)에 한 줄씩 남겼다. 몇 가지:

- 키 비교는 대시를 무시한다 (`payment.api-key` = `payment.apikey` = `PAYMENT_APIKEY`). Spring relaxed binding과 같다.
- `@ConfigurationProperties` 필드는 `@Validated` + `@NotBlank` 등이 있고 **기본값이 없을 때만** 필수다. Spring은 나머지를 null로 바인딩하고 기동에 성공한다.
- 확신할 수 없는 곳은 추측하지 않고 CG005로 알린다. 오탐보다 "분석 못 함"이 낫다.
- 설정 값은 플레이스홀더 이름을 뽑는 즉시 버린다. YAML 문법 오류 메시지도 문제 줄을 인용하지 않는다. 모든 출력 형식에 대해 테스트한다.

## 문서

| 문서 | 내용 |
|---|---|
| [시작하기](docs/guide/getting-started.md) | 처음 붙이기, 결과 읽기, 고치기 |
| [CI 연동](docs/guide/ci-integration.md) | GitHub Action, 다른 CI, 필수 체크 |
| [AI 에이전트와 쓰기](docs/guide/using-with-ai-agents.md) | JSON, Suggestion, Agent Skill |
| [CLI 레퍼런스](docs/reference/cli.md) · [Gradle DSL](docs/reference/gradle-dsl.md) · [JSON 리포트](docs/reference/json-report.md) | 옵션, 종료 코드, 필드 |
| [규칙](docs/reference/rules/) | CG001~CG005 |
| [키 정규화](docs/reference/key-normalization.md) · [설정 소스](docs/reference/config-sources.md) · [키 추출](docs/reference/key-extraction.md) | 분석 규칙 |
| [명세](docs/spec.md) · [설계 결정](docs/decisions.md) · [범위 밖 아이디어](docs/ideas.md) | 개발 기준 |

## Roadmap

- [x] **M0** 첫인상: README, LICENSE, CLAUDE.md, 명세 문서, CI 골격
- [x] **M1** 뼈대: 멀티 모듈 Gradle, version catalog, Spotless·Error Prone·JaCoCo, 도메인 모델
- [x] **M2** 정규화: Spring relaxed binding 규칙 + 단위 테스트
- [x] **M3** 설정 소스: yml/properties 파싱, 프로필 병합, 플레이스홀더, env 파일
- [x] **M4** 키 추출: `@Value`, `Environment`, `@ConfigurationProperties`, 조건부 강등
- [x] **M5** 비교 + 리포트: 규칙 CG001~CG005, 수정 제안, 콘솔/Markdown/JSON, JSON 스키마
- [x] **M6** CLI: `check` / `explain` / `rules`, 고정된 종료 코드
- [x] **M7** Gradle 플러그인: `configGuardCheck` 태스크, TestKit 테스트
- [x] **M8** GitHub Action + 샘플 앱 + e2e
- [x] **M9** 문서: 가이드, 규칙 레퍼런스, Agent Skill, llms.txt, 릴리스 설정
- [ ] **M10** (선택) Agent Skill 효과 평가
- [ ] 첫 릴리스 (GitHub Release CLI jar, Plugin Portal 배포, Action `v0` 태그) — 사람이 결정

## 개발 방식

이 프로젝트는 [명세(docs/spec.md)](docs/spec.md)를 기준으로 LLM(Claude Code)과 함께 개발한다.

- [CLAUDE.md](CLAUDE.md): LLM이 이 저장소에서 작업할 때 따르는 규칙
- [docs/decisions.md](docs/decisions.md): 명세가 모호했던 부분과 정한 해석
- [docs/retrospective/spec-driven-dev.md](docs/retrospective/spec-driven-dev.md): LLM이 명세를 잘못 이해했던 사례와 명세를 어떻게 고쳤어야 했는지

빌드: `./gradlew build` (테스트, Spotless, Error Prone + NullAway, JaCoCo core 90%). CI는 JDK 21로 빌드하고 JDK 17/21에서 테스트한다.

## License

[MIT](LICENSE)
