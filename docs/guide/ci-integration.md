# CI 연동

## GitHub Action

```yaml
# .github/workflows/config-guard.yml
name: config-guard
on:
  pull_request:

permissions:
  contents: read
  pull-requests: write   # PR 코멘트

jobs:
  config-guard:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"
      - uses: LEEJaeHyeok97/config-guard/action@v0
        with:
          working-directory: .
          gradle-task: configGuardCheck
          comment: true
```

하는 일:

1. `./gradlew configGuardCheck --continue`를 실행한다. 태스크가 실패해도 리포트는 만든다.
2. PR이면 `build/reports/config-guard/report.md`를 코멘트로 단다. 첫 줄의 `<!-- config-guard-report -->` 마커로 기존 코멘트를 찾아, 다시 실행하면 **같은 코멘트를 갱신한다**.
3. 리포트를 잡 요약(Step Summary)에도 붙인다.
4. Gradle 태스크의 종료 코드를 그대로 Action 결과로 돌려준다. ERROR가 있으면 체크가 빨간색이 된다.

이 저장소의 [e2e 워크플로](../../.github/workflows/e2e.yml)가 실제로 이 Action을 samples/demo에 돌린다.

### 필수 체크로 만들기

Settings → Branches(또는 Rulesets)에서 이 잡을 required status check로 지정하면, 설정 누락이 있는 PR은 머지할 수 없다.

### 멀티 모듈

Action은 `working-directory`의 `build/reports/config-guard/`를 읽는다. 플러그인을 하위 모듈(`app/`)에 적용했다면 그 모듈을 루트로 하는 빌드가 아니므로 다음처럼 한다.

```yaml
- uses: LEEJaeHyeok97/config-guard/action@v0
  with:
    gradle-task: ":app:configGuardCheck"
    comment: false        # 리포트 경로가 루트가 아니라서 코멘트는 직접 단다
```

### 포크 PR

포크에서 온 PR은 토큰에 쓰기 권한이 없다. 이때 Action은 코멘트만 경고로 건너뛰고 검사 결과는 그대로 돌려준다.

## 다른 CI (CLI)

Gradle 없이도 CLI jar 하나로 돌릴 수 있다.

```bash
java -jar config-guard-0.1.0-all.jar check . \
  --profile dev --profile prod \
  --env-file prod=deploy/prod.env.keys \
  --format markdown > config-guard.md
```

종료 코드: 0 문제 없음, 1 기준 초과(`--fail-on`), 2 사용법 오류, 3 내부 오류. GitLab CI 예:

```yaml
config-guard:
  image: eclipse-temurin:21-jre
  script:
    - java -jar tools/config-guard-all.jar check . -p prod --env-file prod=deploy/prod.env.keys
```

## 환경변수 목록을 정확하게

결과의 정확도는 환경변수 목록이 실제 배포 환경과 얼마나 같은지에 달려 있다. 가능하면 배포 설정(Helm values, Terraform, ECS task definition)에서 이름 목록을 생성해 커밋하거나 CI 단계에서 만든다. 값은 절대 목록에 넣지 않는다.
