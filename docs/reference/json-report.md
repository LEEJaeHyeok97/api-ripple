# JSON 리포트

에이전트와 스크립트가 읽는 형식. 스키마: [`schema/report.schema.json`](../../schema/report.schema.json) (JSON Schema 2020-12).

- `schemaVersion`은 `"1"`이다. 필드를 지우거나 의미를 바꾸는 변경은 버전을 올린다. 필드 추가는 올리지 않는다.
- 같은 입력이면 바이트 단위로 같은 출력이 나온다(시각·절대 경로 없음, 줄바꿈 LF, 정렬 고정).
- 설정 **값**은 어떤 필드에도 들어가지 않는다.

## 구조

```json
{
  "schemaVersion": "1",
  "summary": { "profiles": ["dev", "prod", "staging"], "errors": 1, "warnings": 0, "infos": 0 },
  "findings": [
    {
      "ruleId": "CG002",
      "rule": "MISSING_ENV_VAR",
      "severity": "ERROR",
      "profile": "prod",
      "key": "DEFAULT_TTL",
      "location": { "file": "src/main/resources/application.yml", "line": 10 },
      "message": "cache.ttl이 요구",
      "suggestions": [ { "kind": "ADD_ENV_VAR", "file": null, "target": "DEFAULT_TTL" } ]
    }
  ],
  "notices": [
    { "code": "ENV_CHECK_SKIPPED", "profile": "staging", "message": "환경변수 목록 파일이 지정되지 않아 환경변수 검사를 건너뜀" }
  ]
}
```

위 예시는 `fixtures/env-placeholder`의 실제 출력이다.

## 필드

| 필드 | 설명 |
|---|---|
| `findings[].ruleId` | `CG001`~`CG005`. 공개 후 의미를 바꾸지 않는다. 설명: `docs/reference/rules/<ruleId>.md`, `config-guard explain <ruleId>` |
| `findings[].profile` | 대상 프로필. 프로필과 무관한 결과(분석 불가, 기본 파일의 미사용 키)는 `"*"` |
| `findings[].key` | 정규형 설정 키. CG002는 환경변수 이름. 모르면 `""` |
| `findings[].location` | 프로젝트 루트 기준 상대 경로와 1부터 시작하는 라인. 모르면 `null` |
| `findings[].suggestions` | 기계가 바로 적용할 수 있는 수정. **ERROR는 항상 1개 이상** |
| `notices[]` | 규칙 위반이 아닌 안내. 지금은 `ENV_CHECK_SKIPPED`뿐 |

정렬: 지정한 프로필 순서(공통 `*`은 마지막) → 심각도(ERROR, WARNING, INFO) → 규칙 ID → 키.

## Suggestion 종류

| kind | file | target | 적용 방법 |
|---|---|---|---|
| `ADD_YAML_KEY` | 고칠 설정 파일 | 추가할 키 | 그 파일에 키를 추가한다. 값은 secret이면 `${ENV}` 플레이스홀더로 쓴다 |
| `ADD_ENV_VAR` | `null` | 환경변수 이름 | 배포 환경과 환경변수 목록 파일에 이름을 추가한다 |
| `ADD_DEFAULT_VALUE` | 코드 파일 | 키 | 코드에서 기본값을 준다 (예약됨, 현재 생성하지 않음) |
| `IGNORE_KEY` | `null` | glob | `ignoreKeys`에 추가한다 (예약됨, 현재 생성하지 않음) |
