#!/usr/bin/env python3
"""Verify public HubPilot releases; optionally recover inspection-only Java.
New handoff tooling, 2026-09-13. Not the original build or test harness.
Python 3.10+, standard library. Java needed only for --decompile.
Never deploys plugins or reads production configuration/credentials.
"""
import argparse
import hashlib
import json
import pathlib
import re
import shutil
import struct
import subprocess
import tempfile
import urllib.request
import zipfile

HERE = pathlib.Path(__file__).resolve().parent


def sha(data):
    return hashlib.sha256(data).hexdigest()


def fetch(url, target, expected):
    if target.exists():
        if sha(target.read_bytes()) != expected:
            raise RuntimeError(f"Existing file has unexpected hash: {target}; move it aside explicitly")
        return
    request = urllib.request.Request(url, headers={"User-Agent": "HubPilot-public-handoff-audit"})
    with urllib.request.urlopen(request, timeout=90) as response:
        data = response.read()
    if sha(data) != expected:
        raise RuntimeError(f"Download hash mismatch for {target.name}; no file written")
    tmp = target.with_suffix(target.suffix + ".part")
    tmp.write_bytes(data)
    tmp.replace(target)


def inspect_jar(path):
    with zipfile.ZipFile(path) as z:
        names = z.namelist()
        if len(names) != len(set(names)):
            raise RuntimeError(f"Duplicate ZIP entries: {path.name}")
        bad = z.testzip()
        if bad:
            raise RuntimeError(f"Bad ZIP CRC: {path.name}:{bad}")
        for required in ("META-INF/licenses/hubpilot/LICENSE.txt", "META-INF/licenses/autoserver/LICENSE.txt"):
            if required not in names:
                raise RuntimeError(f"Missing license: {path.name}:{required}")
        descriptor = "velocity-plugin.json" if "velocity-plugin.json" in names else "plugin.yml"
        text = z.read(descriptor).decode("utf-8")
        if descriptor.endswith("json"):
            main = json.loads(text)["main"]
        else:
            main = re.search(r"^main:\s*(\S+)", text, re.M).group(1)
        if main.replace(".", "/") + ".class" not in names:
            raise RuntimeError(f"Missing declared entrypoint: {path.name}")
        own = [n for n in names if n.startswith("dev/hubpilot/") and n.endswith(".class")]
        return {
            "name": path.name, "sha256": sha(path.read_bytes()), "entrypoint": main,
            "descriptor": text, "own_class_count": len(own),
            "own_class_major_versions": sorted({struct.unpack(">H", z.read(n)[6:8])[0] for n in own}),
            "own_classes_sha256": {n: sha(z.read(n)) for n in sorted(own)},
            "resources": [n for n in names if not n.endswith((".class", "/"))],
            "archive_crc_and_required_notices": "PASS",
        }


def decompile(jars, root, manifest):
    tool = manifest["decompiler"]
    cfr = root / "cfr-0.152.jar"
    fetch(tool["url"], cfr, tool["sha256"])
    if not shutil.which("java"):
        raise RuntimeError("Java is required for --decompile")
    output = root / "reconstructed-inspection-only"
    if output.exists():
        raise RuntimeError(f"Refusing to overwrite {output}; use a new output directory")
    output.mkdir()
    (output / "README.txt").write_text(
        "RECONSTRUCTED FROM PINNED PUBLIC RELEASE JARS, NOT ORIGINAL SOURCE.\n"
        "Not compiled or a reproducible build. CFR can emit incorrect control flow, invalid\n"
        "Java and missing dependency warnings. Compare bytecode before changing behavior.\n"
        "Known example: Core PublicControlGate.dispatch returns true on successful dispatch\n"
        "at bytecode offsets 249-250; CFR 0.152 incorrectly prints a finally return false.\n"
        "Original source files already tracked under development/1.0.2 take precedence\n"
        "where available; exact release bytes remain authoritative for shipped behavior.\n"
        "Retain included HubPilot, AutoServer and dependency license notices.\n",
        encoding="utf-8",
    )
    for jar in jars:
        component = jar.name.split("-")[1]
        target = output / component
        target.mkdir()
        with tempfile.TemporaryDirectory(prefix="hubpilot-cfr-") as tmp:
            result = subprocess.run(["java", "-jar", str(cfr), str(jar), "--outputdir", tmp,
                                     "--silent", "true"], capture_output=True, text=True)
            (target / "decompiler.log").write_text(result.stdout + result.stderr, encoding="utf-8")
            if result.returncode:
                raise RuntimeError(f"CFR failed for {jar.name}; inspect decompiler.log")
            own = pathlib.Path(tmp) / "dev" / "hubpilot"
            if not own.exists():
                raise RuntimeError(f"CFR produced no HubPilot classes for {jar.name}")
            shutil.copytree(own, target / "dev" / "hubpilot")
        with zipfile.ZipFile(jar) as z:
            for n in z.namelist():
                if n.endswith((".class", "/")):
                    continue
                rel = pathlib.PurePosixPath(n)
                if rel.is_absolute() or ".." in rel.parts:
                    raise RuntimeError("Unsafe archive path")
                f = target / "resources" / str(rel)
                f.parent.mkdir(parents=True, exist_ok=True)
                f.write_bytes(z.read(n))
    shutil.copy2(HERE / "public-artifacts.json", output / "public-artifacts.json")
    shutil.make_archive(str(root / "reconstructed-inspection-only"), "zip", output)
    return str(output)


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--release", choices=("v1.0.1", "v1.0.2"), default="v1.0.2")
    ap.add_argument("--output", type=pathlib.Path, required=True)
    ap.add_argument("--decompile", action="store_true")
    args = ap.parse_args()
    manifest = json.loads((HERE / "public-artifacts.json").read_text())
    release = next(r for r in manifest["releases"] if r["tag_name"] == args.release)
    root = args.output.resolve()
    root.mkdir(parents=True, exist_ok=True)
    jars = []
    bundles = []
    for a in release["assets"]:
        path = root / a["name"]
        if pathlib.Path(a["name"]).name != a["name"]:
            raise RuntimeError("Unsafe manifest filename")
        fetch(a["browser_download_url"], path, a["sha256"])
        if path.stat().st_size != a["size"]:
            raise RuntimeError(f"Size mismatch: {path.name}")
        if path.suffix == ".jar":
            jars.append(path)
        elif path.suffix == ".zip":
            bundles.append(path)
    report = {"scope": "Static archive verification only; no plugin execution or live testing",
              "release": args.release, "artifacts": [inspect_jar(j) for j in jars]}
    for bundle in bundles:
        with zipfile.ZipFile(bundle) as z:
            if z.testzip() or len(z.namelist()) != len(set(z.namelist())):
                raise RuntimeError(f"Invalid bundle: {bundle.name}")
            for jar in jars:
                if z.read(jar.name) != jar.read_bytes():
                    raise RuntimeError(f"Bundle differs from standalone JAR: {jar.name}")
    report["bundle_matches_standalone_jars"] = True
    checksum = root / "CHECKSUMS-SHA256.txt"
    if checksum.exists():
        for jar in jars:
            if not any(line.split() and line.split()[0] == sha(jar.read_bytes())
                       and line.split()[-1].lstrip("*") == jar.name
                       for line in checksum.read_text().splitlines()):
                raise RuntimeError(f"Checksum list does not match {jar.name}")
        report["release_checksum_list_matches"] = True
    if args.decompile:
        report["inspection_source_path"] = decompile(jars, root, manifest)
    (root / "verification.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(f"PASS static release verification: {args.release}; {len(jars)} JARs; {root / 'verification.json'}")
    print("This is not a source build, JVM linkage check, security certification, or live test.")


if __name__ == "__main__":
    main()
