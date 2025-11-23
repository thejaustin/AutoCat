#!/data/data/com.termux/files/usr/bin/bash
# Auto-install APK after successful GitHub Actions build

echo "🔄 Waiting for build to complete..."
sleep 20

while true; do
  result=$(gh run list --branch 15-dev --limit 1 --json status,conclusion)
  build_status=$(echo "$result" | jq -r '.[0].status')
  conclusion=$(echo "$result" | jq -r '.[0].conclusion')

  if [ "$build_status" = "completed" ]; then
    if [ "$conclusion" = "success" ]; then
      echo "✅ Build successful! Downloading APK..."

      # Download latest release APK
      cd /sdcard/Download
      curl -L -o AutoCat-dev-latest.apk \
        https://github.com/thejaustin/AutoCat/releases/download/dev-latest/AutoCat-dev-latest.apk

      if [ $? -eq 0 ]; then
        echo "📦 Downloaded to /sdcard/Download/AutoCat-dev-latest.apk"
        echo "📲 Installing APK..."

        # Install using termux-open (opens installer)
        termux-open /sdcard/Download/AutoCat-dev-latest.apk

        echo "✅ Installation started! Please approve the install prompt."
      else
        echo "❌ Download failed!"
        exit 1
      fi

      exit 0
    else
      echo "❌ Build failed with conclusion: $conclusion"
      exit 1
    fi
  fi

  echo "⏳ Still building... (Status: $build_status)"
  sleep 15
done
