#!/usr/bin/env bash
# report.md를 PR 코멘트로 게시한다. 첫 줄의 마커로 기존 코멘트를 찾아 새로 달지 않고 수정한다.
# 필요한 환경변수: GH_TOKEN, REPO(owner/name), PR_NUMBER, REPORT(report.md 경로)
set -euo pipefail

marker='<!-- config-guard-report -->'

if [[ ! -f "$REPORT" ]]; then
  echo "::warning::config-guard 리포트가 없어 PR 코멘트를 건너뛴다: $REPORT"
  exit 0
fi
if [[ "$(head -n 1 "$REPORT")" != "$marker" ]]; then
  echo "::warning::리포트 첫 줄에 마커가 없어 PR 코멘트를 건너뛴다: $REPORT"
  exit 0
fi

existing="$(gh api --paginate "repos/$REPO/issues/$PR_NUMBER/comments" \
  --jq ".[] | select(.body | startswith(\"$marker\")) | .id" | head -n 1 || true)"

# 포크 PR처럼 토큰에 쓰기 권한이 없으면 코멘트만 실패하고 검사 결과는 그대로 둔다.
if [[ -n "$existing" ]]; then
  if gh api -X PATCH "repos/$REPO/issues/comments/$existing" -F "body=@$REPORT" > /dev/null; then
    echo "config-guard: PR 코멘트를 갱신했다 (id $existing)"
  else
    echo "::warning::PR 코멘트를 갱신하지 못했다. 워크플로에 'pull-requests: write' 권한이 있는지 확인한다."
  fi
else
  if gh api -X POST "repos/$REPO/issues/$PR_NUMBER/comments" -F "body=@$REPORT" > /dev/null; then
    echo "config-guard: PR 코멘트를 게시했다"
  else
    echo "::warning::PR 코멘트를 게시하지 못했다. 워크플로에 'pull-requests: write' 권한이 있는지 확인한다."
  fi
fi
