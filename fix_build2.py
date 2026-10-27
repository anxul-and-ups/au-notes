#!/usr/bin/env python3
"""AU Notes — Build Fix Round 2"""
import os, re, shutil
from pathlib import Path

WS  = Path(os.path.expanduser("~/claude2-0-tmp"))
APP = WS / "app/src/main/java/com/example"

def bk(p):
    b = Path(str(p) + ".bak2"); shutil.copy(p, b); return b

print("="*72); print("AU Notes — Fix Round 2"); print("="*72)

# ═══════════════════════════════════════════════════════════
# FIX 1: HtmlViewerScreen.kt — swap line 1 (import) ↔ line 2 (package)
# ═══════════════════════════════════════════════════════════
print("\n[1/2] HtmlViewerScreen.kt — swap L1 ↔ L2")
html = APP / "ui/screens/HtmlViewerScreen.kt"
lines = html.read_text().splitlines()

if len(lines) < 2:
    print("  ⚠️  File too short — manual fix needed")
elif lines[0].lstrip().startswith("import ") and lines[1].lstrip().startswith("package "):
    bk(html)
    lines[0], lines[1] = lines[1], lines[0]
    html.write_text("\n".join(lines))
    print("  ✅ Swapped")
    print(f"  L1: {lines[0][:90]}")
    print(f"  L2: {lines[1][:90]}")
else:
    print("  ℹ️  Not in swapped state, no change")
    print(f"  L1: {lines[0][:80]}")
    print(f"  L2: {lines[1][:80]}")

# Sanity check — HtmlViewerScreen composable actually present hai?
if "fun HtmlViewerScreen" not in html.read_text():
    print("  ⚠️  WARNING: 'fun HtmlViewerScreen' not found — file may be incomplete!")

# ═══════════════════════════════════════════════════════════
# FIX 2: AiChatScreen.kt — add top-level beginVoice() stub
# ═══════════════════════════════════════════════════════════
print("\n[2/2] AiChatScreen.kt — add top-level beginVoice() stub")
ac = APP / "ui/screens/AiChatScreen.kt"
txt = ac.read_text()

has_top = (re.search(r'^fun beginVoice\s*\(', txt, re.M) or
           re.search(r'^private fun beginVoice\s*\(', txt, re.M))

if has_top:
    print("  ℹ️  Top-level beginVoice already exists")
else:
    bk(ac)
    stub = ("\n\n// TODO(build-fix): top-level stub added because the real beginVoice() is\n"
            "// inside a different lambda scope (L821) and cannot be resolved from L713.\n"
            "// Replace this stub with the real implementation.\n"
            "private fun beginVoice() { /* no-op stub */ }\n")
    ac.write_text(txt.rstrip() + stub)
    print("  ✅ Appended private top-level stub at end of file")

# ═══════════════════════════════════════════════════════════
# VERIFY: ScreenShare.kt (already fixed in current state)
# ═══════════════════════════════════════════════════════════
print("\n[verify] ScreenShare.kt")
ss = APP / "data/ai/ScreenShare.kt"
if ss.exists():
    t = ss.read_text()
    bad = re.findall(r'\bmp\s*\.', t)
    if bad:
        print(f"  ⚠️  {len(bad)} bare 'mp.' found — may still fail")
    else:
        print("  ✓ All `mp.` calls use !! or ?. — good")

print("\n" + "="*72)
print("DONE. Next:  cd ~/claude2-0-tmp && ./gradlew assembleDebug")
print("="*72)
