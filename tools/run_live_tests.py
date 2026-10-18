#!/usr/bin/env python3
"""
Test Runner for Euthopiar Universal Plugins (EUP v2)
Compiles tools/LivePluginTestSuite.kt and executes comprehensive live testing
across all 15 plugins (9 Main + 6 Vault) on 20 content items each.
"""

import os
import sys
import subprocess
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parent.parent
ANDROID_JAR = Path(r"C:\Users\sehga\AppData\Local\Android\Sdk\platforms\android-35\android.jar")
GRADLE_CACHE = Path(r"C:\Users\sehga\.gradle\caches\modules-2\files-2.1")
COMPILED_EUP_CLASSES = ROOT_DIR / "build" / "test_eup_classes"
CORE_CLASSES_DIR = ROOT_DIR / "core" / "build" / "tmp" / "kotlin-classes" / "debug"
EUP_API_CLASSES_DIR = ROOT_DIR / "eup-api" / "build" / "classes" / "kotlin" / "main"
SUITE_OUT_DIR = ROOT_DIR / "build" / "test_suite_classes"

def main():
    print("[*] Setting up classpath for LivePluginTestSuite...")
    all_cache_jars = list(GRADLE_CACHE.glob("**/*.jar"))

    comp_jars = [
        str(j) for j in all_cache_jars
        if any(k in j.name for k in [
            'kotlin-compiler-embeddable-2.1.0',
            'kotlin-stdlib-2.1.0',
            'kotlin-reflect-2.1.0',
            'trove4j',
            'kotlinx-coroutines-core-jvm-1.9.0',
            'annotations-13.0.jar'
        ])
    ]
    comp_cp = ";".join(comp_jars)

    ser_plugin = next(j for j in all_cache_jars if 'kotlin-serialization-compiler-plugin-embeddable-2.1.0' in j.name)

    lib_jars = [
        str(ANDROID_JAR),
        str(CORE_CLASSES_DIR),
        str(EUP_API_CLASSES_DIR),
        str(COMPILED_EUP_CLASSES)
    ]

    for j in all_cache_jars:
        s = str(j)
        if any(k in s for k in [
            'kotlin-stdlib',
            'kotlin-reflect',
            'okhttp',
            'okio',
            'jsoup',
            'kotlinx-serialization',
            'kotlinx-coroutines',
            'dylibso',
            'material-1.12.0',
            'annotations-13.0.jar',
            'jackson'
        ]):
            lib_jars.append(s)

    compile_cp = ";".join(lib_jars)

    if not SUITE_OUT_DIR.exists():
        SUITE_OUT_DIR.mkdir(parents=True, exist_ok=True)

    suite_source = ROOT_DIR / "tools" / "LivePluginTestSuite.kt"

    print("[*] Compiling LivePluginTestSuite.kt...")
    cmd = [
        "java", "-cp", comp_cp,
        "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
        "-jvm-target", "17",
        "-cp", compile_cp,
        "-Xskip-metadata-version-check",
        "-Xplugin=" + str(ser_plugin),
        "-d", str(SUITE_OUT_DIR),
        str(suite_source)
    ]

    res = subprocess.run(cmd, capture_output=True, text=True)
    if res.returncode != 0:
        print(f"[!] Compilation failed:\n{res.stderr}")
        sys.exit(1)

    print("[+] LivePluginTestSuite compiled successfully.")

    # Build runtime classpath
    runtime_jars = [
        str(ANDROID_JAR),
        str(CORE_CLASSES_DIR),
        str(EUP_API_CLASSES_DIR),
        str(COMPILED_EUP_CLASSES),
        str(SUITE_OUT_DIR)
    ]
    for j in all_cache_jars:
        s = str(j)
        if any(k in s for k in [
            'kotlin-stdlib',
            'kotlin-reflect',
            'okhttp',
            'okio',
            'jsoup',
            'kotlinx-serialization',
            'kotlinx-coroutines',
            'dylibso',
            'material-1.12.0',
            'annotations-13.0.jar',
            'jackson'
        ]):
            runtime_jars.append(s)

    runtime_cp = ";".join(runtime_jars)

    print("\n[*] Launching LivePluginTestSuite execution across 15 plugins...")
    run_cmd = [
        "java",
        "-cp", runtime_cp,
        "tools.LivePluginTestSuite"
    ]

    proc = subprocess.Popen(run_cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
    for line in iter(proc.stdout.readline, ''):
        print(line, end='', flush=True)

    proc.stdout.close()
    proc.wait()
    sys.exit(proc.returncode)

if __name__ == "__main__":
    main()
