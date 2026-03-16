#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

export JAVA_HOME="/Users/jonilchan/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

echo "=============================="
echo " Spring Config Jump - Build"
echo "=============================="
echo "JAVA_HOME: $JAVA_HOME"
echo "Java version: $(java -version 2>&1 | head -1)"
echo ""

case "${1:-build}" in
  clean)
    echo "[*] Cleaning..."
    ./gradlew clean
    echo "[OK] Clean complete."
    ;;
  build)
    echo "[*] Building plugin..."
    ./gradlew buildPlugin
    echo ""
    echo "[OK] Build complete. Plugin zip:"
    ls -lh build/distributions/*.zip 2>/dev/null || echo "  (not found)"
    ;;
  run)
    echo "[*] Launching IDE with plugin..."
    ./gradlew runIde
    ;;
  verify)
    echo "[*] Verifying plugin compatibility..."
    ./gradlew verifyPlugin
    echo "[OK] Verification complete."
    ;;
  all)
    echo "[*] Clean + Build..."
    ./gradlew clean buildPlugin
    echo ""
    echo "[OK] All done. Plugin zip:"
    ls -lh build/distributions/*.zip 2>/dev/null || echo "  (not found)"
    ;;
  *)
    echo "Usage: $0 {clean|build|run|verify|all}"
    echo ""
    echo "  clean   - Clean build output"
    echo "  build   - Build plugin zip (default)"
    echo "  run     - Launch a sandbox IDEA with the plugin"
    echo "  verify  - Verify plugin compatibility"
    echo "  all     - Clean + Build + Verify"
    exit 1
    ;;
esac
