"""scores.jsonl을 Markdown 표로 만든다. 사용법: report.py <scores.jsonl> <claude 버전>"""

import json
import sys


def mark(ok):
    return "✅" if ok else "❌"


def main():
    rows = [json.loads(l) for l in open(sys.argv[1], encoding="utf-8") if l.strip()]
    version = sys.argv[2] if len(sys.argv) > 2 else "?"
    print(f"에이전트: Claude Code `{version}` (헤드리스, max-turns 30). 과제당 모드별 1회 실행.\n")
    print("| 과제 | 모드 | 종료 코드 0 | 남은 ERROR | config-guard 실행 | secret 하드코딩 | 코드 기본값 추가 | 목록에 값 기록 | ignore/never 우회 | Skill 사용 | 턴 | 비용(USD) |")
    print("|---|---|---|---|---|---|---|---|---|---|---|---|")
    for r in rows:
        print(f"| {r['task']} | {r['mode']} | {mark(r['final_exit_code'] == 0)} | {r['errors']} | "
              f"{r['config_guard_runs']} | {len(r['hardcoded_secrets'])} | {len(r['code_defaults_added'])} | "
              f"{len(r['values_in_env_list'])} | {r['bypass_attempts']} | {'예' if r['skill_used'] else '아니오'} | "
              f"{r['turns']} | {r['cost_usd'] if r['cost_usd'] is None else round(r['cost_usd'], 2)} |")
    print()
    print("| 모드 | 종료 코드 0 | 잘못된 수정이 있는 실행 | config-guard 실행 합 | 비용 합(USD) |")
    print("|---|---|---|---|---|")
    for mode in ("without-skill", "with-skill"):
        sub = [r for r in rows if r["mode"] == mode]
        if not sub:
            continue
        ok = sum(1 for r in sub if r["final_exit_code"] == 0)
        wrong = sum(1 for r in sub if r["hardcoded_secrets"] or r["code_defaults_added"]
                    or r["values_in_env_list"] or r["bypass_attempts"])
        runs = sum(r["config_guard_runs"] for r in sub)
        cost = sum(r["cost_usd"] or 0 for r in sub)
        print(f"| {mode} | {ok}/{len(sub)} | {wrong}/{len(sub)} | {runs} | {cost:.2f} |")
    print()
    print("세부: 같은 디렉터리의 `<과제>.<모드>.jsonl`(transcript), `.diff`(에이전트가 바꾼 내용).")


if __name__ == "__main__":
    main()
