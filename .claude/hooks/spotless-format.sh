#!/usr/bin/env bash
# PostToolUse: format the Java file Claude just edited with the same Spotless config the build checks.
set -euo pipefail

file=$(jq -r '.tool_input.file_path // empty')
[[ "$file" == "$CLAUDE_PROJECT_DIR"/*.java && -f "$file" ]] || exit 0

cd "$CLAUDE_PROJECT_DIR"
# spotlessIdeHook formats only this file; Spotless prints IS CLEAN/IS DIRTY, so keep output for failures only.
if ! out=$(./gradlew spotlessApply -PspotlessIdeHook="$file" --quiet 2>&1); then
  echo "spotlessApply failed for $file:" >&2
  echo "$out" >&2
  exit 2
fi
