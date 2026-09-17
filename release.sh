#!/bin/sh
# Tags origin/main as v<version from mod/gradle.properties> and pushes the tag, which
# makes publish.yml ship to GitHub Releases, Modrinth and CurseForge. Then waits on
# that run. Bump `version` and push to main first. Run from the repo root.
set -eu

git fetch -q --tags origin
version=$(sed -n 's/^version=//p' mod/gradle.properties)
tag="v$version"

git rev-parse -q --verify "refs/tags/$tag" >/dev/null &&
  { echo "$tag already exists -- bump version in mod/gradle.properties" >&2; exit 1; }
[ -z "$(git rev-list origin/main..HEAD)" ] ||
  { echo "HEAD has commits not on origin/main -- push them first" >&2; exit 1; }

git tag -a "$tag" -m "$tag" origin/main
git push -q origin "$tag"
echo "pushed $tag ($(git rev-parse --short origin/main))"

command -v gh >/dev/null || exit 0
until run=$(gh run list --workflow publish.yml --branch "$tag" -L 1 --json databaseId -q '.[0].databaseId') && [ -n "$run" ]; do
  sleep 3
done
gh run watch "$run" --exit-status
