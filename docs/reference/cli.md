# CLI

`config-guard`는 사람과 LLM 에이전트가 함께 쓰는 1차 인터페이스다. Gradle 플러그인과 같은 core를 써서 같은 입력에 같은 결과를 낸다.

```bash
java -jar config-guard-<version>-all.jar <명령> [옵션]
```

## 명령

| 명령 | 하는 일 |
|---|---|
| `check [PATH]` | 프로필별 설정 누락 검사 |
| `explain <RULE_ID>` | 규칙 설명·예시·고치는 법 출력 ([rules/](rules/)와 같은 내용) |
| `rules` | 규칙 목록 |
| `--version`, `--help` | 버전, 도움말 (모든 명령에서 `--help` 가능) |

## `check`

```bash
config-guard check [PATH] --profile prod --profile dev \
    --env-file prod=deploy/prod.env.keys \
    --format console|markdown|json \
    --fail-on error|warning|never \
    --ignore 'legacy.*' \
    --framework-prefix resilience4j
```

| 옵션 | 기본 | 설명 |
|---|---|---|
| `PATH` | `.` | 프로젝트 디렉터리. `src/main/java`, `src/main/resources`를 읽는다. `src/test`는 읽지 않는다 |
| `-p`, `--profile` | (필수) | 검사할 프로필. 여러 번 지정 |
| `--env-file PROFILE=FILE` | 없음 | 환경변수 이름 목록. 상대 경로는 **현재 디렉터리** 기준. 프로필은 `--profile`에 있어야 한다 |
| `--format` | `console` | `console`, `markdown`(PR 코멘트), `json`([스키마](json-report.md)). 대소문자 무시 |
| `--fail-on` | `error` | 종료 코드 1을 낼 기준. `warning`이면 CG003도 실패 |
| `--ignore GLOB` | 없음 | 모든 판정에서 뺄 키. `*`는 점을 포함한 임의 문자열, `?`는 한 글자 |
| `--framework-prefix PREFIX` | 없음 | CG004에서 뺄 prefix 추가분 (Gradle DSL `extraFrameworkPrefixes`와 같다) |

## 출력

- 리포트는 **stdout**, 진행 로그와 오류는 **stderr**로 간다.
- `--format json`이면 stdout에는 JSON 하나만 나온다. 에이전트는 `config-guard check ... --format json | jq` 처럼 바로 파싱할 수 있다.
- 어떤 출력에도 설정 값은 나오지 않는다. YAML 문법 오류도 `파일:라인`만 알린다.

## 종료 코드

고정되어 있다. 바꾸면 메이저 버전을 올린다.

| 코드 | 뜻 |
|---|---|
| `0` | 문제 없음 (`--fail-on` 기준을 넘는 Finding 없음) |
| `1` | `--fail-on` 기준을 넘는 Finding 있음 |
| `2` | 사용법 오류: 필수 옵션 누락, 없는 경로·파일, 모르는 형식·규칙 ID, `--env-file`의 프로필이 `--profile`에 없음 |
| `3` | 분석 중 내부 오류: 파일을 읽을 수 없음, YAML 문법 오류 |

## 예시

`core/src/test/resources/fixtures/profiles`에 실행한 실제 출력:

```
$ config-guard check fixtures/profiles -p dev -p prod --env-file prod=fixtures/profiles/deploy/prod.env.keys
config-guard: dev
  ERROR  app.region          AppConfig.java:15  @Value, 기본값 없음
  ERROR  db.pool.max-size    AppConfig.java:21  @Value, 기본값 없음
  ERROR  payment.api-key     AppConfig.java:18  @Value, 기본값 없음
  INFO   app.debug-mode      application.yml:24  사용되지 않는 키
  NOTE   환경변수 목록 파일이 지정되지 않아 환경변수 검사를 건너뜀
config-guard: prod
  ERROR  common.tracing    AppConfig.java:24  @Value, 기본값 없음
config-guard: 모든 프로필 공통
  INFO   spring.config.activate.on-profile    application.yml:26  지원하지 않는 프로필 식(!, &, 괄호). 이 문서는 분석에서 제외
4 errors, 0 warnings, 2 info
$ echo $?
1
```

```
$ config-guard rules
CG001  MISSING_KEY      ERROR    코드가 기본값 없이 요구하는 키가 대상 프로필의 유효 설정(...)에도, 환경변수 목록에도 없다.
CG002  MISSING_ENV_VAR  ERROR    설정 값 안의 기본값 없는 플레이스홀더 `${NAME}`이 요구하는 환경변수가 ...
...
```
