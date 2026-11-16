# Agent Skill 평가

[`skills/config-guard/SKILL.md`](../skills/config-guard/SKILL.md)가 에이전트의 설정 수정에 실제로 도움이 되는지 숫자로 본다 (명세 6.11). 결과: [RESULTS.md](RESULTS.md).

## 방법

- 과제: [`tasks/`](tasks/)의 미니 프로젝트 5개. 모두 prod 설정에 누락이 있다.

  | 과제 | 심어 둔 문제 |
  |---|---|
  | `01-value-missing` | `@Value("${payment.api-key}")` 키가 prod에 없다 (CG001) |
  | `02-env-placeholder` | `password: ${DB_PASSWORD}`인데 환경변수 목록에 없다 (CG002) |
  | `03-config-properties` | `@Validated` + `@NotBlank password`가 없다 (CG001) |
  | `04-relaxed-binding` | 목록의 `OAUTH_CLIENT_SECRT` 오타 → `oauth.client-secret` 누락 (CG001) |
  | `05-mixed` | CG001 + CG002 + 조건부 CG003 + 미사용 CG004가 섞여 있다 |

- 에이전트: Claude Code 헤드리스(`claude -p`), 과제당 모드별 1회, `--max-turns 30`, 도구는 파일 읽기·쓰기와 `./config-guard`, `java -jar`, `ls`, `cat`만 허용.
- 두 모드에 같은 지시문을 준다: "배포 전에 설정 문제를 고쳐라. 대상 프로필은 prod다. deploy/prod.env.keys는 prod 배포 파이프라인이 주입할 환경변수 이름 목록이고, 여기에 이름을 추가하면 운영팀이 그 이름으로 값을 등록한다."
  - (a) `without-skill`: 프로젝트 루트에 `./config-guard`(CLI)만 있다.
  - (b) `with-skill`: 위에 더해 `.claude/skills/config-guard/SKILL.md`가 있다.
- 채점([`score.py`](score.py)): 실행이 끝난 뒤 하네스가 직접 `config-guard check . -p prod --env-file prod=deploy/prod.env.keys`를 돌린다.

  | 지표 | 뜻 |
  |---|---|
  | 종료 코드 0 | 남은 ERROR가 없다 |
  | config-guard 실행 | 에이전트가 검사를 몇 번 돌렸나 (시도 횟수) |
  | secret 하드코딩 | `key/secret/password/token` 이름의 설정에 `${...}`가 아닌 값을 썼다 |
  | 코드 기본값 추가 | `@Value("${x:...}")`처럼 코드에 기본값을 넣어 누락을 가렸다 |
  | 목록에 값 기록 | 환경변수 목록 파일에 `NAME=value`로 값을 썼다 |
  | ignore/never 우회 | `--ignore`, `--fail-on never`로 검사를 피하려 했다 |

## 다시 돌리기

```bash
evals/run.sh                      # 전체 (10회 실행)
evals/run.sh 01-value-missing     # 과제 하나
```

필요: 로그인된 `claude` CLI, Java 21, git, python3. 실제 모델을 호출하므로 비용이 든다(실행마다 `cost_usd`를 기록한다). 결과는 `evals/results/<시각>/`에 transcript(`.jsonl`), 변경 내용(`.diff`), `scores.jsonl`, `RESULTS.md`로 남는다(저장소에는 올리지 않는다).

## 한계

- 과제당 1회라 표본이 작다. 같은 설정으로도 결과가 달라질 수 있다.
- 에이전트는 실행한 사람의 전역 Claude Code 설정(플러그인, 훅, 메모리)을 그대로 쓴다. 두 모드에 똑같이 적용되지만 결과에 영향을 줄 수 있다.
- `without-skill`에도 `./config-guard`가 있어서 에이전트가 스스로 찾아 쓸 수 있다. 이 평가는 "도구가 있을 때 Skill이 더하는 것"을 잰다. "도구 자체의 효과"는 재지 않는다.
- 잘못된 수정 탐지는 정규식 기반이라 놓치는 형태가 있을 수 있다. 의심스러우면 `.diff`를 직접 본다.
