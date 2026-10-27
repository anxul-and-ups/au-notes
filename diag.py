#!/usr/bin/env python3
"""AU Notes — Colored Diagnostic"""
import os, subprocess
from pathlib import Path

# ANSI colors
G, R, Y, B, D, CY = "\033[92m", "\033[91m", "\033[93m", "\033[1m", "\033[0m", "\033[96m"

def ok(m):   print(f"  {G}✓ PASS{D}  {m}")
def fail(m): print(f"  {R}✗ FAIL{D}  {m}")
def warn(m): print(f"  {Y}⚠ WARN{D}  {m}")
def info(m): print(f"  {CY}ℹ INFO{D}  {m}")

WS  = Path(os.path.expanduser("~/claude2-0-tmp"))
APP = WS / "app/src/main/java/com/example"
problems = []

print(f"\n{B}{CY}{'═'*62}\n  🔍 AU NOTES DIAGNOSTIC\n{'═'*62}{D}\n")

# ── 1. HtmlViewerScreen.kt line order ─────────────────────
print(f"{B}[1] HtmlViewerScreen.kt — line order{D}")
p = APP / "ui/screens/HtmlViewerScreen.kt"
if not p.exists():
    fail(f"File missing: {p}")
    problems.append("HtmlViewerScreen.kt missing entirely")
else:
    lines = p.read_text().splitlines()
    l1 = lines[0].strip() if lines else ""
    l2 = lines[1].strip() if len(lines) > 1 else ""
    if l1.startswith("package ") and l2.startswith("import "):
        ok(f"L1 = {l1[:60]}")
        ok(f"L2 = {l2[:60]}")
    elif l1.startswith("import ") and l2.startswith("package "):
        fail("Line order still WRONG (import before package)")
        problems.append("HtmlViewerScreen.kt L1/L2 swapped — run fix_build2.py")
    else:
        fail(f"Unexpected structure — L1: {l1[:70]}")
        problems.append(f"HtmlViewerScreen.kt corrupted: L1='{l1[:50]}'")
    if "fun HtmlViewerScreen" in p.read_text():
        ok("HtmlViewerScreen composable defined")
    else:
        fail("'fun HtmlViewerScreen' NOT FOUND — file incomplete")
        problems.append("HtmlViewerScreen.kt missing the composable function")

# ── 2. ScreenShare.kt null-safety ────────────────────────
print(f"\n{B}[2] ScreenShare.kt — null-safety{D}")
p = APP / "data/ai/ScreenShare.kt"
if not p.exists():
    fail(f"File missing: {p}")
    problems.append("ScreenShare.kt missing")
else:
    lines = p.read_text().splitlines()
    for ln in (92, 124):
        if ln <= len(lines):
            line = lines[ln-1]
            if "mp!!." in line or "mp?." in line:
                ok(f"L{ln}: {line.strip()[:70]}")
            elif "mp." in line:
                fail(f"L{ln}: bare 'mp.' — needs !!.  {line.strip()[:60]}")
                problems.append(f"ScreenShare.kt L{ln}: add !!. after mp")
            else:
                info(f"L{ln}: {line.strip()[:70]}")

# ── 3. AiChatScreen beginVoice stub ──────────────────────
print(f"\n{B}[3] AiChatScreen.kt — beginVoice stub{D}")
p = APP / "ui/screens/AiChatScreen.kt"
if not p.exists():
    fail(f"File missing: {p}")
    problems.append("AiChatScreen.kt missing")
else:
    txt = p.read_text()
    has_stub = "private fun beginVoice()" in txt or "\nfun beginVoice()" in txt
    if has_stub:
        ok("Top-level beginVoice() stub present")
    else:
        fail("Top-level beginVoice() MISSING — L713 will not resolve")
        problems.append("AiChatScreen.kt: add 'private fun beginVoice()' top-level stub")

# ── 4. Git state ─────────────────────────────────────────
print(f"\n{B}[4] Git state{D}")
r = subprocess.run(["git","-C",str(WS),"status","--short"], capture_output=True, text=True)
if r.stdout.strip():
    warn("Uncommitted changes exist")
    info("Files: " + ", ".join(r.stdout.strip().split("\n")[:3]))
    problems.append("Uncommitted changes — run 'git add -A && git commit && git push'")
else:
    ok("Working tree clean")
r = subprocess.run(["git","-C",str(WS),"log","-1","--oneline"], capture_output=True, text=True)
info(f"Last commit: {r.stdout.strip()[:70]}")
r = subprocess.run(["git","-C",str(WS),"status","-sb"], capture_output=True, text=True)
info(f"Branch: {r.stdout.strip().splitlines()[0] if r.stdout.strip() else '?'}")

# ═════════════════════════════════════════════════════════
# COPY BLOCK — sirf yeh neeche wala part copy karna
# ═════════════════════════════════════════════════════════
print(f"\n{B}{'═'*62}{D}")
print(f"{B}  📋 COPY EVERYTHING BETWEEN THE ARROWS  {D}")
print(f"{B}{'═'*62}{D}")
print(f"{B}  ▼▼▼ COPY START ▼▼▼{D}")
print(f"{B}{'─'*62}{D}")
if problems:
    for pr in problems:
        print(f"{R}✗ {pr}{D}")
else:
    print(f"{G}✓✓✓ ALL CHECKS PASSED — ready to build & push ✓✓✓{D}")
print(f"{B}{'─'*62}{D}")
print(f"{B}  ▲▲▲ COPY END ▲▲▲{D}")
print(f"{B}{'═'*62}{D}\n")
