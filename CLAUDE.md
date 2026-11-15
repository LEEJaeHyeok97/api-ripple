# CLAUDE.md

이 저장소에서 LLM(Claude Code 등)이 작업할 때 따르는 규칙이다. 사람 기여자에게도 같은 규칙이 적용된다.

## 기준 문서

- 명세: [docs/spec.md](docs/spec.md). 범위 밖 기능은 만들지 않고 [docs/ideas.md](docs/ideas.md)에 적는다.
- 모호한 규칙은 "Spring Boot 실제 동작과 같게"로 정하고 [docs/decisions.md](docs/decisions.md)에 한 줄씩 남긴다.
- 지시·명세를 잘못 해석했거나 되물어야 했던 일은 [docs/retrospective/spec-driven-dev.md](docs/retrospective/spec-driven-dev.md)에 사실대로 기록한다.

## 명령

```bash
./gradlew build        # 컴파일 + 테스트 + 검사 전체. 커밋 전에 반드시 통과해야 한다.
```

모듈과 검사 도구(Spotless, Error Prone, JaCoCo)가 추가되면 이 절을 갱신한다.

## 디렉토리 역할

| 경로 | 역할 | 상태 |
|---|---|---|
| `core/` | 분석 엔진. Gradle 의존성 없는 순수 라이브러리 | M1~ |
| `cli/` | core를 감싼 picocli CLI | M6 |
| `gradle-plugin/` | core를 감싼 Gradle 플러그인 | M7 |
| `action/` | GitHub composite action | M8 |
| `samples/` | 일부러 설정을 빠뜨린 Spring Boot 샘플 | M8 |
| `docs/` | 명세, 설계 결정, 가이드, 레퍼런스 | M0~ |

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
