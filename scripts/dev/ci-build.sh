#!/usr/bin/env bash
# Trigger CI on the current branch and watch it to completion.
# Pushing to a *-dev branch also triggers it automatically — use this for re-runs.
set -euo pipefail
cd "$(dirname "$0")/../.."
branch=$(git branch --show-current)
gh workflow run ci.yml --ref "$branch"
sleep 10
run_id=$(gh run list --workflow=ci.yml --branch "$branch" --limit 1 --json databaseId --jq '.[0].databaseId')
gh run watch "$run_id" --exit-status
