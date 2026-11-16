# AI 에이전트와 쓰기

config-guard는 LLM 에이전트가 사람에게 묻지 않고 설정 누락을 고칠 수 있게 만들었다.

## 에이전트에게 주는 것

| 무엇 | 어디 |
|---|---|
| 작업 절차 | [`skills/config-guard/SKILL.md`](../../skills/config-guard/SKILL.md) (Agent Skill) |
| 출력 형식 | [`schema/report.schema.json`](../../schema/report.schema.json), [JSON 리포트](../reference/json-report.md) |
| 규칙 설명 | `config-guard explain <ID>` = [`docs/reference/rules/<ID>.md`](../reference/rules/) |
| 문서 색인 | [`llms.txt`](../../llms.txt) |

Claude Code라면 Skill을 프로젝트에 복사한다.

```bash
mkdir -p .claude/skills && cp -r path/to/api-ripple/skills/config-guard .claude/skills/
```

## 수정 루프

```bash
java -jar config-guard-all.jar check . --profile prod --env-file prod=deploy/prod.env.keys --format json
```

1. 종료 코드를 본다. `0`이면 끝. `2`는 명령이 잘못됐고, `3`은 설정 파일을 읽을 수 없다(YAML 문법 오류 위치가 stderr에 나온다).
2. stdout의 JSON에서 `severity == "ERROR"`인 `findings`를 고른다. stderr는 진행 로그라서 파싱하지 않는다.
3. 각 Finding의 `suggestions`를 적용한다.
   - `ADD_YAML_KEY`: `file`에 `target` 키를 추가한다. 값이 secret이면 `${ENV_NAME}` 플레이스홀더로 쓰고, 그 이름을 환경변수 목록에 추가한다.
   - `ADD_ENV_VAR`: 환경변수 목록 파일(예: `deploy/prod.env.keys`)에 `target` 이름을 추가한다. 실제 값은 배포 시스템에 넣어야 하므로 사람에게 알린다.
4. 다시 실행한다. ERROR가 0이 될 때까지 반복한다.

예시 Finding (`fixtures/value-basic`):

```json
{
  "ruleId": "CG001",
  "severity": "ERROR",
  "profile": "prod",
  "key": "payment.api-key",
  "location": { "file": "src/main/java/com/example/PaymentClient.java", "line": 9 },
  "message": "@Value, 기본값 없음",
  "suggestions": [
    { "kind": "ADD_YAML_KEY", "file": "src/main/resources/application-prod.yml", "target": "payment.api-key" },
    { "kind": "ADD_ENV_VAR", "file": null, "target": "PAYMENT_APIKEY" }
  ]
}
```

## 하지 말아야 할 것

- **secret 값을 파일에 쓰지 않는다.** `payment.api-key: sk-...`처럼 값을 하드코딩해서 에러를 없애면 안 된다. 플레이스홀더와 환경변수 이름만 쓴다.
- **에러를 없애려고 `--ignore`/`ignoreKeys`를 넓게 쓰지 않는다.** `--ignore '*'`는 모든 검사를 끈다. 정말 이 도구가 모르는 출처의 키일 때만, 가장 좁은 패턴으로 쓴다.
- **코드에 기본값을 넣어 누락을 숨기지 않는다.** `@Value("${payment.api-key:}")`는 기동은 되게 하지만 빈 키로 운영에 나간다.
- CG003(WARNING)은 조건이 켜지는 환경에서만 문제다. 무조건 키를 추가하지 말고 조건을 확인한다.

## 왜 이렇게 만들었나

- JSON에 `schemaVersion`이 있고 스키마가 공개되어 있어서, 에이전트가 필드 이름을 추측하지 않는다.
- 모든 ERROR에 Suggestion이 있어서, "어디를 고치지?"를 사람에게 묻지 않는다. 스키마가 이 규칙을 강제한다.
- 규칙 설명의 출처가 하나(`docs/reference/rules`)라서 `explain` 출력과 문서가 어긋나지 않는다.
- stdout/stderr를 분리하고 종료 코드를 고정해서, 파이프로 바로 처리할 수 있다.
