# config-guard 명세서

Spring Boot 애플리케이션이 배포 전에 환경별 설정 누락을 잡아내도록 하는 Gradle 플러그인 + GitHub Action. 이 문서는 Claude Code가 구현할 때 따르는 기준 문서다. 범위 밖의 기능은 만들지 않는다.

> 원문은 일반 텍스트로 전달되었다. 이 파일은 내용을 바꾸지 않고 Markdown 구조(제목, 표, 코드 블록)만 복원한 것이다.

## 1. 배경과 목표

### 문제

`@Value("${payment.api-key}")`, `@ConfigurationProperties` 등으로 주입받는 값이 특정 프로필(예: prod) 설정에 빠져 있으면, 배포는 성공하지만 애플리케이션이 기동 중에 실패한다. 이 사실은 보통 배포 후 로그를 보고 나서야 드러난다.

### 목표 (단 하나)

"코드가 요구하는 설정 키"와 "환경별로 실제 제공되는 설정 키"를 정적으로 비교해서, 누락을 CI(Continuous Integration, 지속적 통합) 단계에서 실패시킨다.

### 성공 기준

- 샘플 프로젝트에서 의도적으로 빠뜨린 설정 키를 100% 탐지한다.
- 기본값이 있는 키, 조건부 키를 누락으로 잘못 보고하지 않는다(오탐 0건, 테스트 fixture 기준).
- `./gradlew configGuardCheck` 한 줄로 실행되고, PR(Pull Request)에 결과 코멘트가 달린다.

## 2. 범위

### MVP(Minimum Viable Product, 최소 기능 제품)에 포함

- 코드에서 필요한 설정 키 추출: `@Value`, `@ConfigurationProperties`, `Environment.getProperty` / `getRequiredProperty`
- 설정 소스 수집: `application.yml`, `application.properties`, `application-{profile}.yml|properties`, 멀티 도큐먼트 yml(`spring.config.activate.on-profile`)
- 설정 값 안의 플레이스홀더(`${DB_URL}`)가 요구하는 환경변수 이름 추적
- 환경변수 이름 목록 입력: 사용자가 지정한 파일(.env 형식 또는 키 이름만 한 줄씩 적은 파일)
- Relaxed binding(느슨한 바인딩) 정규화
- 리포트 출력: 콘솔, Markdown(PR 코멘트용), JSON
- Gradle 플러그인 + GitHub Action
- 단독 실행 CLI(Command Line Interface) — 사람과 LLM(Large Language Model) 에이전트가 모두 쓰는 1차 인터페이스
- LLM 친화 인터페이스: 안정적인 JSON 스키마, 규칙 ID, 수정 제안, explain 명령, Agent Skill, llms.txt
- 레퍼런스 문서 + 가이드 문서 (docs/)

### 포함하지 않음 (Non-goals)

- 설정 값의 유효성 검사(URL 형식, 숫자 범위 등). 키 존재 여부만 본다.
- Secret 값 읽기. 키 이름만 다룬다. 값은 절대 로그·리포트에 출력하지 않는다.
- Kubernetes ConfigMap/Secret, AWS Parameter Store, Spring Cloud Config 연동 (확장 포인트만 인터페이스로 남긴다)
- 애플리케이션 실행(ApplicationContext 기동). 순수 정적 분석만 한다.
- Kotlin 소스 지원
- Maven 플러그인

## 3. 기술 스택

| 항목 | 선택 |
|---|---|
| 언어 | Java 21로 빌드, 바이트코드 타깃 17 (Spring Boot 3 최소 요구 버전과 맞춰 사용자 프로젝트에서 돌아가게) |
| 빌드 | Gradle (Kotlin DSL), 멀티 모듈, version catalog(gradle/libs.versions.toml) |
| CLI | picocli |
| 포맷/린트 | Spotless(google-java-format), Error Prone + NullAway |
| 커버리지 | JaCoCo (core 라인 커버리지 90% 미만이면 빌드 실패) |
| 소스 파싱 | JavaParser (com.github.javaparser:javaparser-symbol-solver-core) |
| YAML 파싱 | SnakeYAML |
| 테스트 | JUnit 5, AssertJ, Gradle TestKit(플러그인 통합 테스트) |
| JSON 출력 | Jackson |

## 4. 모듈 구조

```
config-guard/
├── core/                  # 분석 엔진. Gradle 의존성 없음. 순수 라이브러리.
│   └── src/main/java/.../
│       ├── extract/       # 코드 → 필요한 키 (RequiredKey)
│       ├── source/        # 설정 파일 → 제공되는 키 (ProvidedKey)
│       ├── normalize/     # relaxed binding 정규화
│       ├── compare/       # 비교 → Finding
│       └── report/        # Console / Markdown / JSON 렌더러
├── cli/                   # core를 감싼 CLI (picocli). fat jar로 배포
├── gradle-plugin/         # core를 감싼 Gradle 플러그인 (configGuardCheck 태스크)
├── action/                # GitHub Action (composite action, action.yml)
├── skills/config-guard/   # LLM 에이전트용 Agent Skill (SKILL.md)
├── schema/                # report.schema.json (JSON 출력 스키마, 버전 관리)
├── docs/
│   ├── guide/             # getting-started, ci-integration, using-with-ai-agents
│   ├── reference/         # rules/CG001.md ..., cli.md, gradle-dsl.md, json-report.md
│   ├── decisions.md       # 설계 결정 기록
│   └── ideas.md           # 범위 밖 아이디어
├── samples/               # 데모용 Spring Boot 샘플 프로젝트 1개
├── CLAUDE.md              # 이 저장소에서 LLM이 작업할 때의 규칙 (빌드 명령, 디렉토리 역할, 금지 사항)
├── llms.txt               # LLM이 저장소/문서를 빠르게 파악하기 위한 인덱스
└── CHANGELOG.md
```

core는 gradle-plugin에 의존하지 않는다. 나중에 CLI나 Maven 플러그인을 붙일 수 있게 한다.

## 5. 도메인 모델

```java
record RequiredKey(
    String canonicalKey,        // 정규화된 키. 예: "payment.api-key"
    Requirement requirement,    // REQUIRED | OPTIONAL | CONDITIONAL
    SourceLocation location,    // 파일 경로, 라인 번호
    String reason               // 예: "@Value, 기본값 없음", "@NotBlank"
) {}

record ProvidedKey(
    String canonicalKey,
    String profile,             // "default" | "dev" | "prod" ...
    SourceLocation location,
    List<String> requiredEnvVars // 값 안의 ${ENV} 중 기본값 없는 것
) {}

record Finding(
    String ruleId,              // CG001 ... (아래 표)
    Severity severity,          // ERROR | WARNING | INFO
    String profile,
    String key,
    SourceLocation location,
    String message,             // 사람용 설명
    List<Suggestion> suggestions // 기계가 바로 적용할 수 있는 수정 제안
) {}

record Suggestion(
    SuggestionKind kind,        // ADD_YAML_KEY | ADD_ENV_VAR | ADD_DEFAULT_VALUE | IGNORE_KEY
    String file,                // 예: "src/main/resources/application-prod.yml" (ADD_ENV_VAR면 null)
    String target               // 예: "payment.api-key" 또는 "PAYMENT_APIKEY"
) {}
```

### 규칙 ID

| ID | 이름 | 기본 심각도 |
|---|---|---|
| CG001 | MISSING_KEY | ERROR |
| CG002 | MISSING_ENV_VAR | ERROR |
| CG003 | CONDITIONAL_KEY | WARNING |
| CG004 | UNUSED_KEY | INFO |
| CG005 | UNANALYZABLE | INFO |

규칙 ID는 한 번 공개하면 의미를 바꾸지 않는다. 규칙마다 `docs/reference/rules/CGxxx.md`가 있어야 한다.

## 6. 상세 요구사항

### 6.1 필요한 키 추출 (extract)

| 패턴 | 판정 |
|---|---|
| `@Value("${a.b}")` | REQUIRED |
| `@Value("${a.b:default}")`, `@Value("${a.b:}")` | OPTIONAL (빈 기본값도 기본값으로 본다) |
| `@Value("${a:${b}}")` 중첩 | a는 OPTIONAL, b는 REQUIRED. 재귀적으로 처리 |
| `@Value("${a:${b:x}}")` | 둘 다 OPTIONAL |
| `@Value("#{...}")` SpEL(Spring Expression Language) | 분석하지 않고 INFO로 "분석 불가" 보고 |
| `@Value("prefix-${a.b}-suffix")` | 문자열 안의 모든 `${}`를 추출 |
| `@ConfigurationProperties(prefix = "x")` 클래스의 필드 | `x.{필드명 kebab-case}` |
| ├ 필드에 `@NotNull` / `@NotBlank` / `@NotEmpty` 있고 클래스에 `@Validated` | REQUIRED |
| ├ 필드에 초기값 있음 (`int timeout = 30;`) | OPTIONAL |
| ├ 위 둘 다 아님 | OPTIONAL (Spring은 null로 바인딩하고 기동은 성공하므로) |
| ├ 중첩 클래스 타입 필드 | 재귀적으로 `x.nested.field` |
| ├ record / 생성자 바인딩 | 생성자 파라미터 기준으로 동일 규칙 적용. `@DefaultValue` 있으면 OPTIONAL |
| ├ Map, List 타입 필드 | 하위 키를 열거하지 않고 해당 prefix 존재 여부만 OPTIONAL로 기록 |
| `environment.getRequiredProperty("k")` | REQUIRED |
| `environment.getProperty("k")`, `getProperty("k", default)` | OPTIONAL |
| 키가 문자열 리터럴이 아님 (변수, 상수 조합) | INFO로 "동적 키, 분석 불가" 보고 |
| 위 어노테이션이 붙은 클래스/메서드에 `@ConditionalOnProperty` 또는 `@Profile` | REQUIRED → CONDITIONAL로 강등 |

- static final String 상수로 키를 정의한 경우(`@Value("${" + KEY + "}")`)는 같은 파일 안의 상수까지만 해석한다. 그 외는 INFO.
- src/test는 분석하지 않는다.

### 6.2 설정 소스 수집 (source)

- 기본 위치: `src/main/resources/` 아래 `application*.yml|yaml|properties`
- 프로필 결정:
  - `application.yml` → default
  - `application-prod.yml` → prod
  - 멀티 도큐먼트 yml에서 `spring.config.activate.on-profile: prod` → prod
- 어떤 프로필의 유효 키 집합 = default ∪ 해당 프로필. (Spring의 프로필 병합 규칙과 같게)
- `spring.profiles.include` / `spring.profiles.group`은 MVP에서 1단계까지만 따라간다.
- YAML 중첩 구조는 점 표기로 평탄화한다. 리스트는 `a.b[0].c` 형태로 평탄화한다.
- 값이 `${ENV_NAME}`(기본값 없음)이면 그 키는 제공된 것으로 보되, `ENV_NAME` 환경변수를 요구하는 것으로 기록한다.

### 6.3 환경변수 이름 목록 (source)

- 프로필별로 사용자가 파일을 지정한다 (6.7 DSL 참고).
- 지원 형식:
  - .env 형식: `KEY=value` → 키 이름만 읽고 값은 즉시 버린다
  - 이름 목록 형식: 한 줄에 하나 (`DB_URL`), `#` 주석 허용
- 환경변수는 두 가지 역할을 한다.
  - yml 플레이스홀더 `${DB_URL}`의 충족 여부 판정
  - Spring relaxed binding으로 설정 키를 직접 제공 (`PAYMENT_APIKEY` → `payment.apikey`)
- 환경변수 파일을 지정하지 않은 프로필은 환경변수 검사를 건너뛰고 INFO로 알린다.

### 6.4 정규화 (normalize)

Spring Boot relaxed binding 규칙을 따른다.

- 정규형: 소문자 + kebab-case + 점 구분. 예: `payment.api-key`
- `payment.apiKey`, `payment.api_key`, `payment.api-key` → 모두 `payment.api-key`
- 환경변수 → 키: `_`를 `.`로, 소문자화. `PAYMENT_APIKEY` → `payment.apikey`
- 환경변수는 대시를 표현할 수 없으므로, 비교 시 `payment.api-key`와 `payment.apikey`를 대시 제거 후 같다고 본다 (Spring 동작과 동일).
- 리스트 인덱스 `a.b[0]` 유지.
- 정규화 함수는 단독 단위 테스트를 충분히 갖춘다 (최소 15케이스).

### 6.5 비교 (compare)

대상 프로필마다:

| 조건 | Finding |
|---|---|
| REQUIRED 키가 유효 키 집합에도, 환경변수에도 없음 | ERROR MISSING_KEY |
| 제공된 키의 값이 요구하는 `${ENV}`가 환경변수 목록에 없음 | ERROR MISSING_ENV_VAR |
| CONDITIONAL 키가 없음 | WARNING CONDITIONAL_KEY (조건 정보 메시지 포함) |
| 설정 파일엔 있는데 코드 어디서도 요구하지 않는 키 | INFO UNUSED_KEY |
| 분석 불가 패턴 | INFO |

- UNUSED_KEY 판정에서 `spring.*`, `server.*`, `management.*`, `logging.*` 등 프레임워크 키는 제외한다. 제외 prefix 목록은 상수로 관리하고 DSL로 추가할 수 있다.
- `ignoreKeys`(DSL)에 있는 키는 모든 판정에서 제외한다. glob(`payment.*`)을 지원한다.

### 6.6 리포트 (report)

**콘솔**

```
config-guard: prod
  ERROR  payment.api-key       PaymentClient.java:23  @Value, 기본값 없음
  ERROR  env DB_URL            application-prod.yml:4  spring.datasource.url이 요구
  WARN   alarm.push.fcm-key    FcmProperties.java:12  @ConditionalOnProperty(alarm.push.enabled)
  INFO   legacy.s3.bucket      application-prod.yml:41  사용되지 않는 키
2 errors, 1 warning, 1 info
```

**Markdown** (`build/reports/config-guard/report.md`)

- 최상단에 숨은 마커 `<!-- config-guard-report -->` (Action이 기존 코멘트를 찾아 갱신하는 데 사용)
- 프로필별 섹션, ERROR가 없으면 `✅ prod: 문제 없음`
- INFO는 `<details>`로 접는다

**JSON** (`build/reports/config-guard/report.json`): Finding 배열 + 요약

**보안 규칙**: 어떤 출력에도 설정 값을 포함하지 않는다. 테스트로 검증한다 (6.9).

### 6.7 Gradle 플러그인

```kotlin
plugins {
    id("io.github.leejaehyeok97.config-guard") version "0.1.0"
}

configGuard {
    profiles.set(listOf("dev", "prod"))                 // 필수
    envFiles.put("prod", file("deploy/prod.env.keys"))  // 선택
    failOnError.set(true)                               // 기본 true
    failOnWarning.set(false)                            // 기본 false
    ignoreKeys.set(listOf("legacy.*"))
    extraFrameworkPrefixes.set(listOf("resilience4j"))
}
```

- 태스크: `configGuardCheck` (group: verification)
- `check` 태스크에 자동으로 걸지 않는다. 사용자가 원하면 `tasks.check { dependsOn("configGuardCheck") }`.
- 입력: `src/main/java`, `src/main/resources`, envFiles. 출력: `build/reports/config-guard/`. Gradle 증분 빌드 캐시가 동작하도록 `@InputFiles` / `@OutputDirectory`를 정확히 선언한다.
- ERROR가 있고 `failOnError=true`면 태스크 실패.
- Gradle Configuration Cache와 호환되게 작성한다.

### 6.8 GitHub Action

`action/action.yml` (composite action):

```yaml
- uses: LEEJaeHyeok97/config-guard/action@v0
  with:
    working-directory: .
    gradle-task: configGuardCheck
    comment: true
```

동작:

- `./gradlew configGuardCheck --continue` 실행 (실패해도 리포트 생성까지는 진행)
- `comment: true`이고 PR 이벤트면 report.md를 PR 코멘트로 게시. 마커로 기존 코멘트를 찾아 새로 달지 않고 수정한다. `gh api` 사용.
- Gradle 태스크의 종료 코드를 그대로 Action 결과로 반환

### 6.9 CLI

```
config-guard check [PATH] --profile prod --profile dev \
    --env-file prod=deploy/prod.env.keys \
    --format console|markdown|json \
    --fail-on error|warning|never \
    --ignore 'legacy.*'

config-guard explain CG001     # 규칙 설명, 예시, 수정 방법 출력
config-guard rules             # 규칙 목록
config-guard --version
```

- 종료 코드는 문서화하고 고정한다: 0 문제 없음, 1 fail-on 기준을 넘는 Finding 있음, 2 사용법 오류, 3 분석 중 내부 오류.
- `--format json`은 stdout에 JSON만 출력한다. 진행 로그는 전부 stderr로 보낸다 (에이전트가 파이프로 바로 파싱할 수 있게).
- `--help`는 옵션마다 실제 예시를 포함한다.
- Gradle 플러그인과 CLI는 같은 core를 쓰므로 같은 입력에 같은 결과가 나와야 한다 (테스트로 보장).

### 6.10 LLM 친화 인터페이스

이 도구는 "사람과 LLM 에이전트가 모두 쉽게 쓰는 도구"를 목표로 한다.

- **JSON 스키마**: `schema/report.schema.json`(JSON Schema)을 공개하고, 리포트에 `"schemaVersion": "1"`을 넣는다. 깨지는 변경은 메이저 버전을 올린다. 실제 출력이 스키마를 통과하는지 테스트한다.
- **수정 제안**: 모든 ERROR Finding은 최소 1개의 Suggestion을 가진다. 에이전트가 사람에게 묻지 않고도 수정 위치를 알 수 있어야 한다.
- **explain 명령**: 규칙 문서와 같은 내용을 터미널에서 출력한다. 출처는 `docs/reference/rules/*.md` 하나로 통일해서 문서와 CLI 설명이 어긋나지 않게 한다 (빌드 시 리소스로 포함).
- **Agent Skill**: `skills/config-guard/SKILL.md`
  - 언제 쓰는지 (설정 키를 추가·변경했을 때, 배포 설정을 바꿨을 때)
  - 실행 명령 (`config-guard check --format json`)
  - 결과 해석과 수정 절차 (Suggestion 적용 → 재실행 → 0이 될 때까지)
  - 하지 말아야 할 것 (Secret 값을 파일에 쓰지 않기, 에러를 없애려고 ignore를 남발하지 않기)
- **llms.txt**: 저장소 루트에 문서 위치와 한 줄 설명을 나열한다.
- **CLAUDE.md**: 빌드/테스트 명령, 모듈별 역할, 이 명세서 위치, 금지 사항을 짧게 적는다. 이 저장소를 LLM이 수정할 때 쓰는 규칙이다.

### 6.11 Skill 평가 (선택, M10)

Agent Skill이 실제로 도움이 되는지 숫자로 보여 준다.

- `evals/` 아래 설정 누락이 있는 과제 프로젝트 5개를 둔다.
- 같은 에이전트에게 "배포 전에 설정 문제를 고쳐라"를 (a) Skill 없이 (b) Skill과 함께 시킨다.
- 측정: `config-guard check` 종료 코드가 0이 되었는지, 시도 횟수, 잘못된 수정(Secret 값 하드코딩, 과도한 ignore) 여부.
- 결과를 `evals/RESULTS.md`에 표로 남긴다. 실행 스크립트를 함께 커밋해서 재현 가능하게 한다.

### 6.12 저장소 품질

- CI(GitHub Actions): `./gradlew build`(테스트, Spotless check, Error Prone, JaCoCo 기준) + JDK 17/21 매트릭스
- samples/에 대해 CLI와 Gradle 플러그인을 실제로 돌리는 e2e(end-to-end, 처음부터 끝까지) 잡
- Dependabot으로 Gradle·GitHub Actions 의존성 업데이트
- 의존성 버전은 전부 libs.versions.toml에서만 관리
- PR 템플릿: 변경 내용, 관련 규칙 ID, 문서 갱신 여부 체크박스

### 6.13 문서

- README.md: 문제 정의(기동 실패 시나리오 한 단락) → 30초 설치(CLI / Gradle / Action) → 출력 예시 → AI 에이전트와 함께 쓰기 → 한계 → 문서 링크
- docs/guide/: getting-started, ci-integration, using-with-ai-agents
- docs/reference/: 규칙별 문서(무엇을 잡는지, 왜 문제인지, 예시 코드, 수정 방법, 오탐 가능성), CLI 옵션, Gradle DSL, JSON 리포트 필드
- 문서의 코드 예시는 fixture에서 가져온 실제 코드여야 한다.

### 6.14 릴리스

- SemVer(Semantic Versioning, 유의적 버전), CHANGELOG.md
- 태그 푸시 시 GitHub Release에 CLI fat jar 첨부
- Gradle Plugin Portal 배포는 설정만 해 두고 실제 배포는 사람이 결정한다

### 6.15 테스트 전략

- `core/src/test/resources/fixtures/` 아래 미니 프로젝트(소스 + resources) 여러 개를 두고, 각각 기대 Finding 목록과 비교한다.
- 필수 fixture:
  - `value-basic`: REQUIRED / OPTIONAL / 빈 기본값 / 중첩 플레이스홀더
  - `config-properties`: prefix, kebab 변환, 중첩 클래스, @Validated + @NotBlank, 초기값, record + @DefaultValue
  - `conditional`: @ConditionalOnProperty, @Profile
  - `profiles`: default + prod 병합, 멀티 도큐먼트 yml
  - `env-placeholder`: `${DB_URL}` 충족 / 미충족
  - `relaxed-binding`: 환경변수로 직접 제공되는 키
  - `unanalyzable`: SpEL, 동적 키
  - `secret-leak`: 설정 값에 `SUPER_SECRET_VALUE_123`을 넣고 콘솔/MD/JSON 어디에도 이 문자열이 없음을 검증
- gradle-plugin: Gradle TestKit으로 samples/ 프로젝트에 대해 태스크 성공/실패, 리포트 파일 생성 검증

## 7. 구현 순서

각 단계는 테스트가 통과하는 상태로 끝내고 커밋한다. 다음 단계로 넘어가기 전에 이전 단계 테스트가 깨지지 않아야 한다.

- **M1 뼈대**: 멀티 모듈 Gradle 프로젝트, version catalog, Spotless·Error Prone·JaCoCo, 도메인 모델, CI, CLAUDE.md
- **M2 정규화**: normalize + 단위 테스트
- **M3 설정 소스**: yml/properties 파싱, 프로필 병합, 플레이스홀더 추출, env 파일 읽기
- **M4 키 추출**: @Value → Environment → @ConfigurationProperties → 조건부 강등 순서로
- **M5 비교 + 리포트**: Finding·Suggestion 생성, 3가지 렌더러, JSON 스키마 + 스키마 검증 테스트, secret-leak 테스트
- **M6 CLI**: check / explain / rules, 종료 코드, stdout/stderr 분리
- **M7 Gradle 플러그인**: DSL, 태스크, TestKit 테스트, CLI와 결과 일치 테스트
- **M8 GitHub Action + 샘플 + e2e**
- **M9 문서**: README, guide, reference(규칙별 문서), Agent Skill, llms.txt, CHANGELOG, 릴리스 워크플로
- **M10 (선택) Skill 평가**: evals/

## 8. Claude Code 작업 규칙

- 이 명세의 범위 밖 기능은 추가하지 않는다. 필요해 보이면 구현하지 말고 docs/ideas.md에 적는다.
- 판단이 애매한 규칙은 "Spring Boot 실제 동작과 같게"를 기준으로 하고, 결정 내용을 docs/decisions.md에 한 줄씩 남긴다 (README "설계 결정" 섹션의 재료).
- 각 마일스톤 끝에 `./gradlew build`가 통과해야 한다.
- 커밋은 마일스톤 단위 이상으로 잘게, 메시지는 `feat(core): ...`, `test(core): ...` 형식.
- 외부 라이브러리는 3장 표에 있는 것만 쓴다. 추가가 필요하면 먼저 이유를 설명한다.
- 설정 값을 다루는 모든 코드 경로에서 값을 저장하거나 출력하지 않는다.
- 동작을 바꾸는 커밋은 같은 커밋에서 관련 문서(docs/reference)와 테스트도 함께 바꾼다.
- 명세가 모호해서 구현 중에 해석을 정한 경우, docs/decisions.md에 "명세의 어느 부분이 모호했고 어떻게 정했는지"를 적는다. (LLM이 이해하기 쉬운 명세가 무엇인지에 대한 회고 자료로 쓴다.)
- 패키지 하나가 300줄을 넘는 클래스를 갖게 되면 분리를 먼저 제안한다.

## 9. 완료 정의 (Definition of Done)

- [ ] 8개 fixture 테스트 전부 통과
- [ ] samples/ 앱에 configGuardCheck 실행 시 의도한 ERROR 2건, WARNING 1건이 정확히 나옴
- [ ] PR에서 Action이 코멘트를 달고, 재실행 시 같은 코멘트를 갱신함
- [ ] README만 보고 다른 프로젝트에 5분 안에 붙일 수 있음
- [ ] CLI와 Gradle 플러그인이 같은 입력에 같은 JSON을 출력함
- [ ] 모든 ERROR Finding에 Suggestion이 있고, JSON 출력이 스키마를 통과함
- [ ] 규칙 5개 모두 docs/reference/rules/에 문서가 있고 explain으로 출력됨
- [ ] skills/config-guard/SKILL.md만 준 에이전트가 samples/의 누락을 고쳐 종료 코드 0을 만듦
- [ ] CI에서 테스트·포맷·정적 분석·커버리지·e2e 잡이 모두 통과
