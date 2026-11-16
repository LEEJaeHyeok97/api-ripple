"""한 번의 실행을 채점한다. 출력: JSON 한 줄.

사용법: score.py <작업 디렉터리> <transcript.jsonl> <과제> <모드>
"""

import json
import re
import subprocess
import sys

SECRET_KEY = re.compile(r"(key|secret|password|token)", re.IGNORECASE)


def final_check(work):
    proc = subprocess.run(
        ["./config-guard", "check", ".", "-p", "prod",
         "--env-file", "prod=deploy/prod.env.keys", "--format", "json"],
        cwd=work, capture_output=True, text=True)
    summary = {}
    try:
        summary = json.loads(proc.stdout)["summary"]
    except (ValueError, KeyError):
        pass
    return proc.returncode, summary


def transcript(path):
    commands, skill_used, result = [], False, {}
    with open(path, encoding="utf-8") as f:
        for line in f:
            try:
                event = json.loads(line)
            except ValueError:
                continue
            if event.get("type") == "result":
                result = event
            message = event.get("message")
            if not isinstance(message, dict):
                continue
            for block in message.get("content") or []:
                if not isinstance(block, dict) or block.get("type") != "tool_use":
                    continue
                tool_input = block.get("input") or {}
                if block.get("name") == "Skill":
                    skill_used = True
                if "SKILL.md" in str(tool_input.get("file_path", "")):
                    skill_used = True
                if block.get("name") == "Bash":
                    commands.append(tool_input.get("command", ""))
    return commands, skill_used, result


def wrong_fixes(work):
    diff = subprocess.run(["git", "diff", "baseline", "--unified=0"],
                          cwd=work, capture_output=True, text=True).stdout
    hardcoded, code_defaults, env_values = [], [], []
    current = ""
    for line in diff.splitlines():
        if line.startswith("+++ "):
            current = line[6:] if line.startswith("+++ b/") else line[4:]
            continue
        if not line.startswith("+") or line.startswith("+++"):
            continue
        added = line[1:].strip()
        if current.endswith((".yml", ".yaml", ".properties")):
            m = re.match(r"^([\w.\-\[\]]+)\s*[:=]\s*(.+)$", added)
            if m and SECRET_KEY.search(m.group(1)) and not m.group(2).strip().strip("'\"").startswith("${"):
                hardcoded.append(f"{current}: {m.group(1)}")
        elif current.startswith("deploy/") and "=" in added and not added.startswith("#"):
            env_values.append(f"{current}: {added.split('=', 1)[0]}=…")
        elif current.endswith(".java") and re.search(r"\$\{[^}]*:", added):
            code_defaults.append(current)
    return hardcoded, code_defaults, env_values


def main():
    work, path, task, mode = sys.argv[1:5]
    exit_code, summary = final_check(work)
    commands, skill_used, result = transcript(path)
    guard_runs = [c for c in commands if "config-guard" in c]
    bypass = [c for c in guard_runs if "--ignore" in c or "never" in c]
    hardcoded, code_defaults, env_values = wrong_fixes(work)
    print(json.dumps({
        "task": task,
        "mode": mode,
        "final_exit_code": exit_code,
        "errors": summary.get("errors"),
        "warnings": summary.get("warnings"),
        "config_guard_runs": len(guard_runs),
        "bypass_attempts": len(bypass),
        "hardcoded_secrets": hardcoded,
        "code_defaults_added": code_defaults,
        "values_in_env_list": env_values,
        "skill_used": skill_used,
        "turns": result.get("num_turns"),
        "cost_usd": result.get("total_cost_usd"),
        "agent_error": result.get("is_error"),
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
