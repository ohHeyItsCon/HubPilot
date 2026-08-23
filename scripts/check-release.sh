#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-1.0.0}"
DIR="${2:-.}"
components=(Core Hub Interact Link)

for component in "${components[@]}"; do
  jar="$DIR/HubPilot-$component-$VERSION.jar"
  if [[ ! -f "$jar" ]]; then
    echo "MISSING: $jar" >&2
    exit 1
  fi

  echo "Checking $(basename "$jar")"
  unzip -tq "$jar" >/dev/null
  if ! unzip -p "$jar" 2>/dev/null | strings | grep -Fq "$VERSION"; then
    echo "WARNING: could not find internal version string $VERSION in $(basename "$jar")" >&2
  fi
  sha256sum "$jar"
done
