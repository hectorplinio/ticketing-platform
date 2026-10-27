#!/usr/bin/env bash
# PreToolUse (Bash): never push to master; every change goes through a PR.
# GitHub's ruleset rejects it anyway; this stops Claude from trying.
set -euo pipefail

cmd=$(jq -r '.tool_input.command // empty')
current=$(git -C "$CLAUDE_PROJECT_DIR" branch --show-current 2>/dev/null || true)

# Only look at segments that actually run `git [opts] push`, not text that mentions it (e.g. commit messages).
while IFS= read -r segment; do
  segment="${segment#"${segment%%[![:space:]]*}"}"
  [[ "$segment" =~ ^(rtk[[:space:]]+)?git([[:space:]]+-[^[:space:]]+([[:space:]]+[^-[:space:]][^[:space:]]*)?)*[[:space:]]+push([[:space:]]|$) ]] || continue
  if [[ "$segment" =~ (^|[[:space:]:+])master([[:space:]]|$) || "$current" == "master" ]]; then
    echo "Blocked: never push to master. Push a feature branch and open a PR instead." >&2
    exit 2
  fi
done < <(printf '%s\n' "$cmd" | tr ';&|' '\n\n\n')
