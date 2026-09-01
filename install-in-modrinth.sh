#!/bin/sh
# Builds a loader's mod jar and installs it into a Modrinth App instance, replacing any
# previously installed copy. Run from the repo root. macOS only.
#
#   sh install-in-modrinth.sh [profile] [loader]
set -eu

profile="${1:-Latest Fabric}"
loader="${2:-fabric}"
mods="$HOME/Library/Application Support/ModrinthApp/profiles/$profile/mods"

[ -d "$mods" ] || { echo "no mods dir: $mods" >&2; exit 1; }

(cd mod && ./gradlew --quiet ":$loader:build")

# gradle.properties is the single source of truth for the mod version.
version=$(sed -n 's/^version=//p' mod/gradle.properties)
jar="mod/$loader/build/libs/letter-banners-$loader-$version.jar"

# Drop every old build, including .disabled ones, so no duplicate mod id loads. Both
# loaders' jars go, so switching loaders never leaves the other one behind.
find "$mods" -maxdepth 1 \( -name 'letter-banners-*.jar' -o -name 'letter-banners-*.jar.disabled' \) \
     -exec rm -f {} +

cp "$jar" "$mods/"
echo "installed $(basename "$jar") into $mods"
