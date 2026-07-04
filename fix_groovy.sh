#!/bin/bash
find . -name "build.gradle" -print0 | while IFS= read -r -d '' file; do
    sed -i -E -e 's/^(\s*)(namespace|compileSdk|minSdk|targetSdk|versionCode|versionName)\s+([0-9]+|"[^"]+"|\x27[^\x27]+\x27)$/\1\2 = \3/g' "$file"
    sed -i -E -e 's/^(\s*)(buildToolsVersion)\s+([0-9]+|"[^"]+"|\x27[^\x27]+\x27)$/\1\2 = \3/g' "$file"
    sed -i -E -e 's/^(\s*)(abortOnError)\s+(true|false)$/\1\2 = \3/g' "$file"
    sed -i -E -e 's/^(\s*)(checkReleaseBuilds)\s+(true|false)$/\1\2 = \3/g' "$file"
    sed -i -E -e 's/^(\s*)(aidl|buildConfig|resValues|generateLocaleConfig)\s+(true|false)$/\1\2 = \3/g' "$file"
done
