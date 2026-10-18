import sys
import subprocess
from pathlib import Path

ROOT_DIR = Path("C:/Euthopiar")
BUILD_CLASSES = ROOT_DIR / "build" / "test_eup_classes"
BUILD_CLASSES.mkdir(parents=True, exist_ok=True)
GRADLE_CACHE = Path(r"C:\Users\sehga\.gradle\caches\modules-2\files-2.1")
ANDROID_JAR = Path(r"C:\Users\sehga\AppData\Local\Android\Sdk\platforms\android-35\android.jar")

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

core_classes_dir = ROOT_DIR / "core" / "build" / "tmp" / "kotlin-classes" / "debug"
eup_api_classes_dir = ROOT_DIR / "eup-api" / "build" / "classes" / "kotlin" / "main"

lib_jars = [
    str(BUILD_CLASSES),
    str(ANDROID_JAR),
    str(eup_api_classes_dir),
    str(core_classes_dir)
]

for j in all_cache_jars:
    s = str(j)
    if any(k in s for k in [
        'kotlin-stdlib',
        'okhttp',
        'okio',
        'jsoup',
        'kotlinx-serialization',
        'kotlinx-coroutines',
        'dylibso',
        'jackson',
        'annotations-13.0.jar'
    ]):
        lib_jars.append(s)

cp = ";".join(lib_jars)

sources_to_compile = [
    str(ROOT_DIR / "test_eups_repo" / "sources" / "OneShowsWasmEngine.kt"),
    str(ROOT_DIR / "test_eups_repo" / "sources" / "OneShowsPlugin.kt"),
    str(ROOT_DIR / "tools" / "DebugStreamFlow.kt")
]

cmd = [
    "java", "-cp", comp_cp,
    "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
    "-jvm-target", "17",
    "-Xskip-metadata-version-check",
    "-cp", cp,
    "-d", str(BUILD_CLASSES),
] + sources_to_compile

print("[*] Compiling OneShowsPlugin.kt, OneShowsWasmEngine.kt, and DebugStreamFlow.kt...")
res = subprocess.run(cmd, capture_output=True, text=True)
if res.returncode != 0:
    print("Compilation failed:")
    print(res.stderr)
    print(res.stdout)
    sys.exit(1)

print("[*] Running tools.DebugStreamFlowKt...")
run_cmd = ["java", "-cp", cp, "tools.DebugStreamFlowKt"]
res2 = subprocess.run(run_cmd)
sys.exit(res2.returncode)
