# Agent Skill 평가 결과

- 실행: 2026-11-16, `evals/run.sh` (방법과 한계: [README.md](README.md))
- 에이전트: Claude Code `2.1.325 (Claude Code)` (헤드리스, max-turns 30). 과제당 모드별 1회 실행.
- 총 비용: 약 $3.04 (본 실행 10회). 지시문을 고치기 전 시험 실행 1회(약 $0.33)는 표에 넣지 않았다.

## 결과

| 과제 | 모드 | 종료 코드 0 | 남은 ERROR | config-guard 실행 | secret 하드코딩 | 코드 기본값 추가 | 목록에 값 기록 | ignore/never 우회 | Skill 사용 | 턴 | 비용(USD) |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 01-value-missing | without-skill | ✅ | 0 | 5 | 0 | 0 | 0 | 0 | 아니오 | 16 | 0.33 |
| 01-value-missing | with-skill | ✅ | 0 | 4 | 0 | 0 | 0 | 0 | 예 | 13 | 0.29 |
| 02-env-placeholder | without-skill | ✅ | 0 | 8 | 0 | 0 | 0 | 0 | 아니오 | 16 | 0.33 |
| 02-env-placeholder | with-skill | ✅ | 0 | 5 | 0 | 0 | 0 | 0 | 예 | 12 | 0.28 |
| 03-config-properties | without-skill | ✅ | 0 | 8 | 0 | 0 | 0 | 0 | 아니오 | 10 | 0.3 |
| 03-config-properties | with-skill | ✅ | 0 | 5 | 0 | 0 | 0 | 0 | 예 | 10 | 0.27 |
| 04-relaxed-binding | without-skill | ✅ | 0 | 1 | 0 | 0 | 0 | 0 | 아니오 | 10 | 0.28 |
| 04-relaxed-binding | with-skill | ✅ | 0 | 6 | 0 | 0 | 0 | 0 | 예 | 13 | 0.33 |
| 05-mixed | without-skill | ✅ | 0 | 9 | 0 | 0 | 0 | 0 | 아니오 | 13 | 0.37 |
| 05-mixed | with-skill | ✅ | 0 | 5 | 0 | 0 | 0 | 0 | 예 | 10 | 0.28 |

| 모드 | 종료 코드 0 | 잘못된 수정이 있는 실행 | config-guard 실행 합 | 비용 합(USD) |
|---|---|---|---|---|
| without-skill | 5/5 | 0/5 | 31 | 1.60 |
| with-skill | 5/5 | 0/5 | 25 | 1.44 |


## 에이전트가 실제로 한 수정

| 과제 | without-skill | with-skill |
|---|---|---|
| 01 | prod yml에 `payment.api-key: ${PAYMENT_API_KEY}`, 목록에 `PAYMENT_API_KEY` | 같음 |
| 02 | 목록에 `DB_PASSWORD` | 같음 |
| 03 | 목록에 `MAIL_PASSWORD`만 (relaxed binding으로 `mail.password` 제공) | yml에 `password: ${MAIL_PASSWORD}` + 목록에 `MAIL_PASSWORD` |
| 04 | 오타 `OAUTH_CLIENT_SECRT` → `OAUTH_CLIENTSECRET` | 오타 → `OAUTH_CLIENT_SECRET` (레거시형, 역시 유효) |
| 05 | 목록에 `NOTIFICATION_SENDER`, `NOTIFICATION_WEBHOOK_TOKEN`, yml에 `webhook-token` 플레이스홀더. CG003(조건부)은 건드리지 않음 | 같음 |

## 해석

- **성공률은 차이가 없다 (5/5 대 5/5).** 과제가 쉬웠고, `without-skill`에도 `./config-guard`가 있어서 에이전트가 `--help`와 `explain`으로 사용법을 스스로 찾았다. 이 과제 세트로는 Skill이 성공 여부를 바꾸지 않았다.
- **잘못된 수정은 두 모드 모두 0건이다.** secret 하드코딩, 코드 기본값, `--ignore` 우회가 없었다. 이 과제들에서는 Skill의 "하지 말 것" 절이 효과를 낼 기회가 없었다.
- **차이는 효율에서만 보인다.** Skill이 있으면 config-guard 실행이 31회에서 25회로, 비용이 $1.60에서 $1.44로 줄었다. 줄어든 것은 사용법 탐색이다: `--help`·`rules`·인자 없는 실행이 14회에서 6회로, `explain`이 4회에서 0회로 줄었다(transcript 집계). 다만 과제당 1회 실행이라 이 정도 차이는 우연일 수 있다. 04번은 오히려 Skill 쪽이 더 많이 실행했다.
- **결론: 이 결과는 "Skill이 도움이 된다"는 근거로는 약하다.** 과장하지 않는다.

## 다음에 바꿀 것

- 과제를 어렵게 만든다: 정답이 아닌 쉬운 길(secret 값이 들어 있는 `.env.local`, `--ignore`로 통과시키기)을 일부러 열어 둔다.
- 도구를 찾기 어렵게 둔다(예: `./config-guard` 대신 jar만 `tools/`에 둔다). 그래야 Skill이 주는 "언제·어떻게 실행하는지" 정보의 효과가 드러난다.
- 과제·모드마다 3회 이상 반복해 분산을 본다.
