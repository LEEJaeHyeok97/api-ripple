# config-guard GitHub Action

`./gradlew configGuardCheck`를 실행하고, PR이면 결과(report.md)를 코멘트로 남긴다. 다시 실행하면 새 코멘트를 달지 않고 같은 코멘트를 갱신한다.

```yaml
permissions:
  contents: read
  pull-requests: write   # PR 코멘트에 필요

steps:
  - uses: actions/checkout@v4
  - uses: actions/setup-java@v4
    with:
      distribution: temurin
      java-version: "21"
  - uses: LEEJaeHyeok97/config-guard/action@v0
    with:
      working-directory: .          # gradlew가 있는 디렉터리
      gradle-task: configGuardCheck
      comment: true
```

| 입력 | 기본 | 설명 |
|---|---|---|
| `working-directory` | `.` | Gradle 프로젝트 디렉터리 |
| `gradle-task` | `configGuardCheck` | 실행할 태스크 |
| `comment` | `true` | PR 이벤트에서 코멘트 게시·갱신 |

| 출력 | 설명 |
|---|---|
| `exit-code` | Gradle 태스크 종료 코드 (Action 결과도 이 값) |
| `report-markdown`, `report-json` | 리포트 경로 |

- `./gradlew <task> --continue`로 실행해, 태스크가 실패해도 리포트는 만든다.
- 리포트는 잡 요약(Step Summary)에도 붙는다.
- 포크 PR처럼 토큰에 쓰기 권한이 없으면 코멘트만 경고로 건너뛰고 검사 결과는 그대로 돌려준다.
- 코멘트는 첫 줄의 `<!-- config-guard-report -->` 마커로 찾는다.
- `@v0`는 0.x의 최신 릴리스를 가리킨다. 정확히 고정하려면 `@v0.1.0` 또는 커밋 SHA를 쓴다.
