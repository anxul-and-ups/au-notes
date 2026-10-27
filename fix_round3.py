#!/usr/bin/env python3
"""AU Notes — Round 3: NoteExporter colors + decorateCodeFences"""
import os, re, shutil, subprocess
from pathlib import Path

WS = Path(os.path.expanduser("~/claude2-0-tmp"))
APP = WS / "app/src/main/java/com/example"
G,R,Y,B,D,CY = "\033[92m","\033[91m","\033[93m","\033[1m","\033[0m","\033[96m"
def ok(m):   print(f"  {G}✓ PASS{D}  {m}")
def fail(m): print(f"  {R}✗ FAIL{D}  {m}")
def warn(m): print(f"  {Y}⚠ WARN{D}  {m}")
def info(m): print(f"  {CY}ℹ INFO{D}  {m}")
def bk(p):
    b = Path(str(p) + ".bak3"); shutil.copy(p, b); return b

problems = []

print(f"\n{B}{CY}{'═'*62}\n  🔧 FIX ROUND 3\n{'═'*62}{D}")

# ═══════════════════════════════════════════════════════════
# [1] NoteExporter.kt — color imports
# ═══════════════════════════════════════════════════════════
print(f"\n{B}[1] NoteExporter.kt — color imports{D}")
p = APP / "ui/util/NoteExporter.kt"
if not p.exists():
    fail(f"Missing: {p}"); problems.append("NoteExporter.kt missing")
else:
    txt = p.read_text()
    lines = txt.splitlines()
    print(f"  Current L1-10:")
    for i,l in enumerate(lines[:10], 1):
        m = f"{R}◄{D}" if 5 <= i <= 7 else " "
        print(f"   {i:2d}{m} {l[:100]}")

    # Detect bad imports
    bad = re.findall(r'^import\s+([\w.]+)\.(blue|green|red)\s*$', txt, re.M)
    if not bad:
        warn("No `import X.{blue|green|red}` found")
        info("Paste L1-10 above into copy block for manual analysis")
        problems.append("Colors: unknown import format — see L1-10 above")
    else:
        print(f"\n  Bad imports found: {len(bad)}")
        for pkg, name in bad:
            print(f"   {R}✗{D} import {pkg}.{name}")

        # Apply fix
        bk(p)
        new_txt = txt
        for pkg, name in bad:
            new_txt = re.sub(rf'^import\s+{re.escape(pkg)}\.{name}\s*\n', '', new_txt, flags=re.M)

        # Ensure Color import exists
        if not re.search(r'^import androidx\.compose\.ui\.graphics\.Color\s*$', new_txt, re.M):
            new_txt = re.sub(r'^(package\s+[\w.]+)\n',
                             r'\1\n\nimport androidx.compose.ui.graphics.Color\n',
                             new_txt, count=1, flags=re.M)
            info("Added `import androidx.compose.ui.graphics.Color`")

        # Replace body usages safely (skip definitions/params)
        total_repl = 0
        for _, name in bad:
            cap = name.capitalize()
            new_lines = []
            for line in new_txt.split("\n"):
                if re.search(rf'\b(val|var|fun|class)\s+{name}\b', line):
                    new_lines.append(line); continue
                if re.search(rf'\b{name}\s*:', line):
                    new_lines.append(line); continue
                orig = line
                line = re.sub(rf'\b{name}\b', f'Color.{cap}', line)
                if line != orig: total_repl += 1
                new_lines.append(line)
            new_txt = "\n".join(new_lines)

        p.write_text(new_txt)
        ok(f"Removed {len(bad)} bad import(s), replaced {total_repl} usage line(s)")
        print(f"  New L1-10:")
        for i,l in enumerate(new_txt.splitlines()[:10], 1):
            print(f"   {i:2d}  {l[:100]}")

# ═══════════════════════════════════════════════════════════
# [2] NoteEditorScreen.kt — decorateCodeFences
# ═══════════════════════════════════════════════════════════
print(f"\n{B}[2] NoteEditorScreen.kt — decorateCodeFences{D}")
p = APP / "ui/screens/NoteEditorScreen.kt"
if not p.exists():
    fail(f"Missing: {p}"); problems.append("NoteEditorScreen.kt missing")
else:
    lines = p.read_text().splitlines()
    print(f"  Context around L1286:")
    for i in range(1283, 1290):
        if i <= len(lines):
            m = f"{R}◄{D}" if i == 1286 else " "
            print(f"  {i:5d}{m} {lines[i-1][:105]}")

    # Search codebase for definition
    r = subprocess.run(f"grep -rn 'fun decorateCodeFences' {APP} 2>/dev/null",
                       shell=True, capture_output=True, text=True)
    defs = r.stdout.strip()
    if defs:
        ok("Definition exists elsewhere:")
        for line in defs.splitlines()[:3]:
            print(f"     {CY}→{D} {line[:110]}")
        # Try add import if package differs
        def_file = defs.split(":")[0]
        pkg_m = re.search(r'^package\s+([\w.]+)', Path(def_file).read_text(), re.M)
        cur_m = re.search(r'^package\s+([\w.]+)', p.read_text(), re.M)
        if pkg_m and cur_m and pkg_m.group(1) != cur_m.group(1):
            pkg = pkg_m.group(1)
            ne_txt = p.read_text()
            imp = f"import {pkg}.decorateCodeFences"
            if imp not in ne_txt:
                bk(p)
                ne_txt = re.sub(r'^(package\s+[\w.]+)\n',
                                rf'\1\n\n{imp}\n', ne_txt, count=1, flags=re.M)
                p.write_text(ne_txt)
                ok(f"Added: {imp}")
            else:
                info("Import present — check function visibility (maybe private?)")
        else:
            info("Same file — check function scope (private inside lambda?)")
        problems.append("decorateCodeFences defined elsewhere — verify import resolves")
    else:
        warn("No definition anywhere — commenting out call")
        bk(p)
        new_lines = []; n = 0
        for i,l in enumerate(lines, 1):
            if i == 1286 and "decorateCodeFences" in l:
                new_lines.append("// TODO(build-fix): decorateCodeFences removed/renamed — reimplement")
                new_lines.append("// " + l)
                n += 1
            else:
                new_lines.append(l)
        p.write_text("\n".join(new_lines))
        if n:
            ok(f"Commented out {n} line(s) at L1286 — build will pass")
        problems.append("decorateCodeFences stubbed — feature disabled until reimplemented")

# ═══════════════════════════════════════════════════════════
# COPY BLOCK
# ═══════════════════════════════════════════════════════════
print(f"\n{B}{'═'*62}{D}")
print(f"{B}  📋 COPY BLOCK{D}")
print(f"{B}{'═'*62}{D}")
print(f"{B}  ▼▼▼ COPY START ▼▼▼{D}")
print(f"{B}{'─'*62}{D}")
if problems:
    for pr in problems: print(f"{R}✗ {pr}{D}")
else:
    print(f"{G}✓ Fixes applied successfully{D}")
print(f"{B}{'─'*62}{D}")
print(f"{B}  ▲▲▲ COPY END ▲▲▲{D}")
print(f"{B}{'═'*62}{D}\n")
