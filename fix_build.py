#!/usr/bin/env python3
"""AU Notes — build fix script"""
import os, re, shutil, subprocess
from pathlib import Path

WS  = Path(os.path.expanduser("~/claude2-0-tmp"))
APP = WS / "app/src/main/java/com/example"

def backup(p):
    p = Path(p); b = Path(str(p) + ".bak")
    shutil.copy(p, b); return b

def show(p, start=1, end=None):
    p = Path(p)
    if not p.exists():
        print(f"  !! NOT FOUND: {p}"); return
    print(f"\n  --- {p.relative_to(WS)} [{start}-{end or 'end'}] ---")
    for i, line in enumerate(open(p), 1):
        if i < start: continue
        if end and i > end: break
        print(f"  {i:5d}| {line.rstrip()[:120]}")

def run(c):
    return subprocess.run(c, shell=True, capture_output=True, text=True)

print("="*72)
print("AU Notes — Build Fix Script")
print(f"Workspace: {WS}")
print("="*72)

# ─────────────────────────────────────────────────────────
# FIX 1: HtmlViewerScreen.kt — syntax errors on line 2
# ─────────────────────────────────────────────────────────
print("\n[1/3] HtmlViewerScreen.kt")
html = APP / "ui/screens/HtmlViewerScreen.kt"
if not html.exists():
    print(f"  !! File missing: {html}")
else:
    lines = html.read_text().splitlines()
    print(f"  Total lines: {len(lines)}")
    for i, l in enumerate(lines[:5], 1):
        print(f"  L{i}: {l[:110]}")

    first  = lines[0].strip() if lines else ""
    second = lines[1].strip() if len(lines) > 1 else ""

    ok_starts = r'^(//|/\*|import |@|fun |class |object |interface |enum |sealed |data |abstract |internal |private |public |protected |val |var |const |typealias|inline |suspend |external |operator |infix |tailrec |annotation |crossinline |noinline |$)'
    bad = (not first.startswith("package")) or (second and not re.match(ok_starts, second))

    if bad:
        print(f"\n  ⚠️  File structure corrupted!")
        rel = "app/src/main/java/com/example/ui/screens/HtmlViewerScreen.kt"
        r = run(f"git -C {WS} show HEAD:{rel}")
        if r.returncode == 0 and r.stdout.strip():
            b = backup(html)
            html.write_text(r.stdout)
            print(f"  ✅ Restored from git HEAD (backup: {b.name})")
            print(f"  Restored first 3 lines:")
            for i, l in enumerate(r.stdout.splitlines()[:3], 1):
                print(f"    {i}: {l[:110]}")
        else:
            print(f"  ⚠️  Could not restore from git: {r.stderr[:150]}")
            print(f"  ⚠️  Please share L1-L5 output above for manual fix")
    else:
        print(f"  ✓ File structure looks OK")

# ─────────────────────────────────────────────────────────
# FIX 2: ScreenShare.kt — nullable MediaProjection
# ─────────────────────────────────────────────────────────
print("\n[2/3] ScreenShare.kt")
ss = APP / "data/ai/ScreenShare.kt"
if not ss.exists():
    print(f"  !! File missing: {ss}")
else:
    show(ss, 88, 96)
    show(ss, 120, 128)

    content = ss.read_text()
    nullable = set(re.findall(r'(\w+)\s*:\s*MediaProjection\s*\?', content))
    print(f"\n  Nullable MediaProjection vars: {nullable or '(none detected)'}")

    if not nullable:
        print(f"  ⚠️  Cannot auto-detect — manual fix needed")
    else:
        backup(ss)
        out_lines, n_changed = [], 0
        for line in content.split("\n"):
            new = line
            for v in nullable:
                if re.match(rf'\s*(private |public |internal |protected )?(val|var)\s+{re.escape(v)}\b', line):
                    continue  # skip declarations
                # assignment  →  use !!   (preserve original non-null semantics)
                # statement   →  use ?.   (safe-call, skip if null)
                is_assign = re.search(r'(val|var)?\s*\w+\s*=\s*' + re.escape(v) + r'\s*\.', new)
                op = "!!." if is_assign else "?."
                new = re.sub(r'\b' + re.escape(v) + r'(?![!?.])\s*\.', v + op, new)
            if new != line:
                print(f"  L{out_lines.count('')+1 if False else ''}  {line.strip()[:80]}")
                print(f"       → {new.strip()[:80]}")
                n_changed += 1
            out_lines.append(new)

        if n_changed:
            ss.write_text("\n".join(out_lines))
            print(f"  ✅ Updated {n_changed} lines")
        else:
            print(f"  ✓ No changes needed")

# ─────────────────────────────────────────────────────────
# FIX 3: AiChatScreen.kt — unresolved 'beginVoice'
# ─────────────────────────────────────────────────────────
print("\n[3/3] AiChatScreen.kt — beginVoice")
ac = APP / "ui/screens/AiChatScreen.kt"
if not ac.exists():
    print(f"  !! File missing: {ac}")
else:
    show(ac, 708, 720)
    r = run(f"grep -rn 'beginVoice' {APP}")
    print(f"\n  All references:\n  " + (r.stdout.strip().replace("\n", "\n  ") if r.stdout.strip() else "(none)"))

    has_def = bool(run(f"grep -rn 'fun beginVoice' {WS}").stdout.strip())
    if has_def:
        print(f"\n  ✓ Definition found — you may need to add an import.")
    else:
        print(f"\n  ✗ No 'fun beginVoice' anywhere → commenting call with TODO")
        backup(ac)
        txt = ac.read_text()
        out = []
        n = 0
        for line in txt.split("\n"):
            if "beginVoice" in line and not line.lstrip().startswith("//"):
                out.append("// TODO(build-fix): beginVoice is undefined — reimplement or remove")
                out.append("// " + line)
                n += 1
            else:
                out.append(line)
        ac.write_text("\n".join(out))
        print(f"  ✅ Commented {n} line(s)")

# ─────────────────────────────────────────────────────────
print("\n" + "="*72)
print("DONE.")
print("Backup files: *.bak (restore with: mv file.bak file)")
print("Next: ./gradlew assembleDebug")
print("If it still fails → paste the new error log.")
print("="*72)
