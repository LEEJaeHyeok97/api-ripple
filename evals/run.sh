#!/usr/bin/env bash
# Agent Skill 효과 평가 (명세 6.11).
# evals/tasks/의 과제마다 같은 에이전트(Claude Code, 헤드리스)에게 같은 지시를
#   (a) Skill 없이  (b) skills/config-guard를 .claude/skills/에 넣고
# 시킨 뒤, 결과를 config-guard로 다시 검사해 점수를 매긴다.
#
# 사용법: evals/run.sh [과제 이름...]      예: evals/run.sh 01-value-missing
# 필요: claude CLI(로그인됨), Java 21, git, python3
# 결과: evals/results/<시각>/ (transcript, scores.jsonl, RESULTS.md)
# 주의: 실제 모델을 호출하므로 비용이 든다. 실행마다 비용을 기록한다.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
out="$root/evals/results/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$out"
max_turns="${MAX_TURNS:-30}"

(cd "$root" && ./gradlew -q :cli:fatJar)
jar="$(ls "$root"/cli/build/libs/config-guard-*-all.jar | head -n 1)"

# 두 모드에 같은 지시를 준다. 환경변수 목록 파일의 역할을 밝혀 둔다: 첫 시험 실행에서 에이전트가
# "목록은 실제로 주입되는 값의 기록이라 이름만 추가하면 거짓"이라며 고치지 않았다 (docs/retrospective 참고).
prompt='배포 전에 설정 문제를 고쳐라. 대상 프로필은 prod다. deploy/prod.env.keys는 prod 배포 파이프라인이 주입할 환경변수 이름 목록이고, 여기에 이름을 추가하면 운영팀이 그 이름으로 값을 등록한다.'

if [[ $# -gt 0 ]]; then tasks=("$@"); else tasks=($(ls "$root/evals/tasks")); fi

for name in "${tasks[@]}"; do
  for mode in without-skill with-skill; do
    work="$(mktemp -d)/$name"
    cp -R "$root/evals/tasks/$name" "$work"
    mkdir -p "$work/tools"
    cp "$jar" "$work/tools/config-guard.jar"
    printf '#!/usr/bin/env bash\nexec java -jar "$(dirname "$0")/tools/config-guard.jar" "$@"\n' > "$work/config-guard"
    chmod +x "$work/config-guard"
    if [[ "$mode" == with-skill ]]; then
      mkdir -p "$work/.claude/skills"
      cp -R "$root/skills/config-guard" "$work/.claude/skills/"
    fi
    (cd "$work" && git init -q && git add -A && git -c user.name=eval -c user.email=eval@example.test commit -qm baseline && git tag baseline)

    echo "== $name / $mode ($work)" >&2
    (cd "$work" && claude -p "$prompt" \
        --output-format stream-json --verbose \
        --max-turns "$max_turns" \
        --permission-mode acceptEdits \
        --allowedTools "Read,Edit,Write,Glob,Grep,Skill,Bash(./config-guard:*),Bash(java -jar:*),Bash(ls:*),Bash(cat:*)" \
        > "$out/$name.$mode.jsonl" 2> "$out/$name.$mode.stderr") || true

    python3 -I "$root/evals/score.py" "$work" "$out/$name.$mode.jsonl" "$name" "$mode" >> "$out/scores.jsonl"
    (cd "$work" && git diff baseline > "$out/$name.$mode.diff") || true
  done
done

python3 -I "$root/evals/report.py" "$out/scores.jsonl" "$(claude --version | head -n 1)" > "$out/RESULTS.md"
echo "결과: $out/RESULTS.md" >&2
