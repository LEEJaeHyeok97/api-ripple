# CLAUDE.md

이 저장소에서 LLM(Claude Code 등)이 작업할 때 따르는 규칙이다. 사람 기여자에게도 같은 규칙이 적용된다.

## 기준 문서

- 명세: [docs/spec.md](docs/spec.md). 범위 밖 기능은 만들지 않고 [docs/ideas.md](docs/ideas.md)에 적는다.
- 모호한 규칙은 "Spring Boot 실제 동작과 같게"로 정하고 [docs/decisions.md](docs/decisions.md)에 한 줄씩 남긴다.
- 지시·명세를 잘못 해석했거나 되물어야 했던 일은 [docs/retrospective/spec-driven-dev.md](docs/retrospective/spec-driven-dev.md)에 사실대로 기록한다.

## 명령

```bash
./gradlew build                      # 전체 검사: 컴파일(Error Prone + NullAway) + 테스트 + Spotless + JaCoCo(core 90%)
./gradlew spotlessApply              # google-java-format으로 자동 포맷. build 전에 실행한다.
./gradlew :core:test                 # core 테스트만
./gradlew build -PtestJavaVersion=17 # 테스트를 JDK 17 런타임에서 실행 (JDK 17 툴체인 필요)
./gradlew :cli:fatJar                # CLI 단일 jar: cli/build/libs/config-guard-<version>-all.jar
(cd samples/demo && ./gradlew configGuardCheck)  # 데모 앱 검사. 일부러 실패한다 (ERROR 2, WARNING 1)
```

- 컴파일과 린트는 JDK 21 툴체인으로 하고, 바이트코드는 17로 낸다(`--release 17`).
- 의존성과 도구 버전은 `gradle/libs.versions.toml`에서만 바꾼다.
- 공통 빌드 규칙은 `build-logic/`의 `configguard.java-conventions` 플러그인에 있다. 모듈 빌드 파일에 중복해서 쓰지 않는다.
- `main` 코드는 NullAway가 검사한다. null이 될 수 있는 값은 `org.jspecify.annotations.Nullable`로 표시한다.

## 디렉토리 역할

| 경로 | 역할 | 상태 |
|---|---|---|
| `build-logic/` | 공통 빌드 규칙(컨벤션 플러그인) | M1 |
| `core/` | 분석 엔진. Gradle 의존성 없는 순수 라이브러리 | M1~ |
| `cli/` | core를 감싼 picocli CLI. 규칙 문서는 빌드 때 `docs/reference/rules`에서 복사한다 | M6~ |
| `gradle-plugin/` | core를 감싼 Gradle 플러그인. TestKit 테스트는 core fixture를 임시 프로젝트로 복사해 쓴다 | M7~ |
| `action/` | GitHub composite action (`action.yml`, `comment.sh`) | M8~ |
| `samples/demo/` | 일부러 prod 설정을 빠뜨린 Spring Boot 앱 (ERROR 2, WARNING 1). 독립 빌드, 플러그인은 `includeBuild("../..")` | M8~ |
| `schema/` | JSON 리포트 스키마. 깨지는 변경이면 `schemaVersion`을 올린다 | M5~ |
| `skills/config-guard/` | LLM 에이전트용 Agent Skill. CLI 옵션·규칙이 바뀌면 함께 고친다 | M9~ |
| `evals/` | Agent Skill 평가 과제·스크립트·결과. `evals/run.sh`는 실제 모델을 호출해 비용이 든다 | M10 |
| `docs/` | 명세, 설계 결정, 가이드(`guide/`), 레퍼런스(`reference/`), 릴리스 절차 | M0~ |
| `llms.txt` | 문서 색인. 문서를 추가·이동하면 갱신한다 | M9~ |

## 작업 흐름

1. 마일스톤마다 브랜치를 딴다 (`<type>/mN-<topic>`). main에 직접 커밋하지 않는다.
2. PR을 만들고 CI 통과를 확인한 뒤 squash merge한다. main은 항상 CI가 통과하는 상태다.
3. 커밋은 Conventional Commits(`feat|fix|test|docs|chore|build|ci(scope): ...`), 한 커밋에 한 가지 목적만.
4. 마일스톤이 끝나면 README Roadmap 체크박스와 CHANGELOG `Unreleased`를 갱신한다.
5. 마일스톤 하나가 끝나면 멈추고 보고한다. 사람이 "계속"이라고 해야 다음 마일스톤으로 넘어간다.

## 금지 사항

- 설정 값(secret)을 코드, 테스트 출력, 로그, 문서 예시에 넣지 않는다. 예외는 secret-leak fixture의 더미 값 `SUPER_SECRET_VALUE_123`뿐이다.
- 테스트 없는 기능을 머지하지 않는다.
- CI를 통과시키려고 테스트를 지우거나 `@Disabled` 처리하지 않는다. 막히면 멈추고 사람에게 묻는다.
- 명세 3장 표에 없는 외부 라이브러리를 추가하지 않는다. 필요하면 먼저 이유를 설명한다.
- 반쯤 만든 기능을 main에 노출하지 않는다. 미완성 기능은 README Roadmap에만 적는다.
- 릴리스(태그 푸시, Plugin Portal 배포, Action 태그 이동)는 사람이 결정한다. 절차: [docs/releasing.md](docs/releasing.md).
