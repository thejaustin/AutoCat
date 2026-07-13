#!/usr/bin/env bash
# Download APK artifacts from the latest successful CI run on the current
# branch into ~/downloads/AutoCat/.
set -euo pipefail
cd "$(dirname "$0")/../.."
dest="$HOME/downloads/AutoCat"
mkdir -p "$dest"
branch=$(git branch --show-current)
run_id=$(gh run list --workflow=ci.yml --branch "$branch" --status success --limit 1 --json databaseId --jq '.[0].databaseId')
[[ -n "$run_id" ]] || { echo "No successful build found on $branch" >&2; exit 1; }
gh run download "$run_id" --dir "$dest"
echo "Downloaded to $dest:"
find "$dest" -name '*.apk'
