import re
import subprocess
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "qa-reports"
OUT.mkdir(exist_ok=True)

cp = ";".join([
    str(ROOT / "qa-tests" / "out"),
    str(ROOT / "dist" / "ProyectoMago.jar"),
    str(ROOT / "dist" / "lib" / "sqlite-jdbc-3.39.3.0.jar"),
    str(ROOT / "qa-lib" / "junit-platform-console-standalone-1.10.2.jar"),
    str(ROOT / "qa-lib" / "assertj-core-3.25.3.jar"),
    str(ROOT / "qa-lib" / "assertj-swing-3.17.1.jar"),
    str(ROOT / "qa-lib" / "assertj-swing-junit-3.17.1.jar"),
])

java = r"C:\Program Files\Java\jdk-17\bin\java.exe"
cmd_base = [
    java, "-cp", cp,
    "org.junit.platform.console.ConsoleLauncher",
    "--select-class=AlmacenQaSuite",
    "--details=none",
]

per_test = defaultdict(list)  # id -> list of statuses across runs
all_runs = []

def parse(output: str):
    rows = []
    for line in output.splitlines():
        if line.startswith("RESULT "):
            # RESULT A1 PASA | evidence
            m = re.match(r"RESULT (\S+) (PASA|FALLA|NO_VERIFICADO) \| (.*)$", line)
            if m:
                rows.append((m.group(1), m.group(2), m.group(3)))
    return rows

for run in range(1, 21):
    accumulate = "1" if run >= 11 else "0"
    env = {**dict(**{k: v for k, v in __import__("os").environ.items()}), "QA_ACCUMULATE": accumulate}
    # also system property
    cmd = [java, f"-Dqa.accumulate={accumulate}", "-cp", cp,
           "org.junit.platform.console.ConsoleLauncher",
           "--select-class=AlmacenQaSuite",
           "--details=none"]
    print(f"\n===== RUN {run}/20 accumulate={accumulate} =====", flush=True)
    proc = subprocess.run(cmd, cwd=str(ROOT), capture_output=True, text=True, encoding="utf-8", errors="replace")
    text = (proc.stdout or "") + "\n" + (proc.stderr or "")
    (OUT / f"run_{run:02d}.log").write_text(text, encoding="utf-8")
    rows = parse(text)
    # Also map junit failures without RESULT line
    statuses = {tid: st for tid, st, _ in rows}
    for tid, st, ev in rows:
        per_test[tid].append(st)
    all_runs.append({"run": run, "accumulate": accumulate, "rows": rows, "exit": proc.returncode})
    print(f"parsed {len(rows)} RESULT lines, exit={proc.returncode}", flush=True)
    for tid, st, ev in rows:
        print(f"  {tid} {st}", flush=True)

# Summary table
summary_lines = ["ID | OK/20 | Veredicto | Evidencia_ultima"]
ids = sorted(per_test.keys(), key=lambda x: (x[0], int(re.sub(r'\\D', '', x) or 0), x))
for tid in ids:
    sts = per_test[tid]
    ok = sum(1 for s in sts if s == "PASA")
    falla = sum(1 for s in sts if s == "FALLA")
    nv = sum(1 for s in sts if s == "NO_VERIFICADO")
    if falla == 0 and nv == 0 and ok == len(sts):
        verd = "PASA"
    elif falla == len(sts):
        verd = "FALLA"
    elif falla > 0 and ok > 0:
        verd = "FLAKY"
    elif nv > 0 and falla == 0:
        verd = "NO VERIFICADO"
    else:
        verd = "FLAKY" if falla else "NO VERIFICADO"
    last_ev = ""
    for run in reversed(all_runs):
        for tid2, st, ev in run["rows"]:
            if tid2 == tid:
                last_ev = ev[:120]
                break
        if last_ev:
            break
    flaky_runs = [str(r["run"]) for r in all_runs for tid2, st, _ in r["rows"] if tid2 == tid and st == "FALLA"]
    extra = f" fail@[{','.join(flaky_runs)}]" if verd == "FLAKY" else ""
    summary_lines.append(f"{tid} | {ok}/{len(sts)} | {verd}{extra} | {last_ev}")

report = "\n".join(summary_lines)
(OUT / "summary.txt").write_text(report, encoding="utf-8")
print("\n===== SUMMARY =====")
print(report)
print(f"\nLogs in {OUT}")
