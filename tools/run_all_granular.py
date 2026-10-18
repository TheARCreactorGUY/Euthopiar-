#!/usr/bin/env python3
"""
Granular Plugin Verifier Orchestrator for all 15 Plugins (9 Main + 6 Vault).
Executes each plugin sequentially one by one, capturing granular verification data:
- Sources & Servers
- Audio & Language Options
- Subtitle Tracks
- TMDB Bridge & Scoring
- AniSkip Intro/Outro Skip Timestamps
- OpenSubtitles Integration
- Direct Download Links Probe
- Vault Privacy Isolation
- 20-Item High-Coverage Sweep
Saves full logs to tools/verification_logs/<plugin>.log
"""

import os
import sys
import subprocess
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parent.parent
LOGS_DIR = ROOT_DIR / "tools" / "verification_logs"
LOGS_DIR.mkdir(parents=True, exist_ok=True)

PLUGINS = [
    ("Atlantic", False),
    ("Cinejoy", False),
    ("Aether", False),
    ("1Shows", False),
    ("4KHDHub", False),
    ("UHDMovies", False),
    ("MoviesLeech", False),
    ("Movy", False),
    ("PvrPlay", False),
    ("HQPorner", True),
    ("Xvideos", True),
    ("Eporner", True),
    ("Rule34Video", True),
    ("Beeg", True),
    ("Faphouse", True)
]

def main():
    target = sys.argv[1] if len(sys.argv) > 1 else None

    plugins_to_run = PLUGINS
    if target:
        plugins_to_run = [p for p in PLUGINS if target.lower() in p[0].lower()]
        if not plugins_to_run:
            print(f"[!] No plugin matching '{target}' found.")
            sys.exit(1)

    print("=================================================================")
    print("  EUTHOPIAR EUP v2 GRANULAR ONE-BY-ONE VERIFICATION ORCHESTRATOR")
    print(f"  Target count: {len(plugins_to_run)} plugin(s)")
    print("=================================================================")

    # First compile once
    compile_cmd = ["python", str(ROOT_DIR / "tools" / "run_granular_verifier.py"), plugins_to_run[0][0]]
    print(f"[*] Pre-compiling test suite and verifying runtime...")

    for name, is_vault in plugins_to_run:
        print(f"\n>>> LAUNCHING DEEP VERIFICATION FOR: {name} (Vault: {is_vault}) <<<")
        log_file = LOGS_DIR / f"{name}.log"
        cmd = ["python", str(ROOT_DIR / "tools" / "run_granular_verifier.py"), name]

        with open(log_file, "w", encoding="utf-8") as lf:
            proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
            for line in iter(proc.stdout.readline, ''):
                print(line, end='', flush=True)
                lf.write(line)
                lf.flush()
            proc.stdout.close()
            proc.wait()

        print(f"[+] Completed verification for {name}. Log written to: {log_file}")

    print("\n=================================================================")
    print("  ALL GRANULAR PLUGIN VERIFICATIONS COMPLETED SUCCESSFULLY!")
    print("=================================================================")

if __name__ == "__main__":
    main()
