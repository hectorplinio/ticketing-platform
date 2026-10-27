#!/usr/bin/env bash
# PreToolUse: block edits to Flyway migrations already on the default branch.
# Flyway checksums applied migrations, so editing a merged one breaks every database that ran it.
set -euo pipefail

file=$(jq -r '.tool_input.file_path // empty')
[[ "$file" == "$CLAUDE_PROJECT_DIR"/*/src/main/resources/db/migration/*.sql ]] || exit 0

cd "$CLAUDE_PROJECT_DIR"
rel=${file#"$CLAUDE_PROJECT_DIR"/}
for ref in master $(git for-each-ref --format='%(refname:short)' 'refs/remotes/*/master'); do
  if git cat-file -e "$ref:$rel" 2>/dev/null; then
    echo "Blocked: $rel is already merged into $ref. Never edit a merged migration; add a new V<n>__<description>.sql instead." >&2
    exit 2
  fi
done
