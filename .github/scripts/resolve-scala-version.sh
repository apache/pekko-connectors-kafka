#!/usr/bin/env bash
set -euo pipefail

scala_version="${1:?scala version is required}"
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

resolve_from_build() {
  local pattern="$1"
  local resolved
  resolved="$(sed -n "$pattern" "$repo_root/project/Versions.scala")"
  if [ -z "$resolved" ]; then
    echo "Unable to resolve Scala version '$scala_version' from project/Versions.scala" >&2
    exit 1
  fi
  printf '%s\n' "$resolved"
}

case "$scala_version" in
  2.13 | 2.13.x | scala213)
    resolve_from_build 's/.*val Scala213 = "\(.*\)".*/\1/p'
    ;;
  3.3 | 3.3.x | scala3)
    resolve_from_build 's/.*val Scala3 = "\(.*\)".*/\1/p'
    ;;
  next)
    resolve_from_build 's/.*val Scala3Next = "\(.*\)".*/\1/p'
    ;;
  *)
    printf '%s\n' "$scala_version"
    ;;
esac
