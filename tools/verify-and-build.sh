#!/usr/bin/env bash
# 1) run the launch-guard tests · 2) rebuild the APK only if they are all green
set -uo pipefail

echo "=== PHASE A · tests $(date +%H:%M:%S)"
bash /home/user/persian-pintrest/tools/run-tests.sh > /home/user/test-harness.log 2>&1
python3 - <<'PY'
import pathlib, re, sys
d = pathlib.Path('/home/user/persian-pintrest/app/build/test-results/testDebugUnitTest')
tot = bad = 0
for p in sorted(d.glob('*.xml')):
    s = p.read_text(encoding='utf-8', errors='replace')
    for m in re.finditer(r'<testcase name="([^"]+)"[^>]*?time="([^"]+)"\s*(/>|>(.*?)</testcase>)', s, re.S):
        tot += 1
        state = 'FAIL' if '<failure' in (m.group(4) or '') else 'PASS'
        bad += state == 'FAIL'
        print(f"  [{state}] {m.group(1)}")
print(f"TESTS {tot - bad}/{tot} passed")
sys.exit(1 if bad or tot == 0 else 0)
PY
if [ $? -ne 0 ]; then echo "=== TESTS RED → build skipped"; echo "###ALLDONE"; exit 1; fi

echo "=== PHASE B · assembleDebug $(date +%H:%M:%S)"
bash /home/user/persian-pintrest/tools/build-apk.sh > /home/user/build8.log 2>&1
echo "build exit=$?"
tr '\r' '\n' < /home/user/build8.log | grep -aE "BUILD SUCCESSFUL|BUILD FAILED|^e: |error:|apk/debug|pinbom-1.0.0-debug.apk" | tail -20
echo "###ALLDONE $(date +%H:%M:%S)"
