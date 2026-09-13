#!/usr/bin/env python3
"""Pinned Java 21 source build. Never compiles against or overlays HubPilot binaries."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
COMPONENTS = ('core', 'hub', 'link', 'interact')


def digest(data):
    return hashlib.sha256(data).hexdigest()


def fetch(entry, directory, offline):
    target = directory / entry['name']
    if not target.exists():
        if offline:
            raise RuntimeError(f'Missing offline dependency: {target.name}')
        data = urllib.request.urlopen(entry['url'], timeout=60).read()
        if digest(data) != entry['sha256']:
            raise RuntimeError(f'Dependency hash mismatch: {target.name}')
        target.write_bytes(data)
    if digest(target.read_bytes()) != entry['sha256']:
        raise RuntimeError(f'Cached dependency hash mismatch: {target.name}')
    return target


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jdk', type=Path, default=os.environ.get('JAVA_HOME'))
    parser.add_argument('--offline', action='store_true')
    parser.add_argument('--output', type=Path, default=ROOT / 'build/candidate')
    args = parser.parse_args()
    javac = str(args.jdk / 'bin/javac') if args.jdk else shutil.which('javac')
    if not javac:
        raise RuntimeError('Set JAVA_HOME or --jdk to a JDK 21 installation')
    version = subprocess.check_output([javac, '-version'], text=True).strip()
    if version != 'javac 21.0.8':
        raise RuntimeError(f'Pinned build requires javac 21.0.8; found {version}')
    out = args.output.resolve()
    if not out.is_relative_to((ROOT / 'build').resolve()):
        raise RuntimeError('Output must stay under this checkout’s ignored build/ directory')
    out.mkdir(parents=True, exist_ok=True)
    deps = ROOT / 'build/deps'
    deps.mkdir(parents=True, exist_ok=True)
    lock = json.loads((ROOT / 'reconstruction/dependencies.lock.json').read_text())
    jars = [fetch(e, deps, args.offline) for e in lock]
    classes = out / 'classes'
    if classes.exists():
        shutil.rmtree(classes)
    classes.mkdir()
    sources = sorted(p for c in COMPONENTS for p in (ROOT / c / 'src/main/java').rglob('*.java'))
    command = [javac, '--release', '21', '-proc:none', '-g', '-encoding', 'UTF-8', '-Xmaxerrs', '500',
               '-cp', os.pathsep.join(map(str, jars)), '-d', str(classes), *map(str, sources)]
    subprocess.run(command, check=True)
    reports = []
    for c in COMPONENTS:
        entries = {str(p.relative_to(classes)).replace(os.sep, '/'): p.read_bytes()
                   for p in (classes / 'dev/hubpilot' / c).rglob('*.class')}
        resources = ROOT / c / 'src/main/resources'
        for p in resources.rglob('*'):
            if p.is_file():
                entries[p.relative_to(resources).as_posix()] = p.read_bytes()
        if c == 'core':
            snake = next(p for p in jars if p.name == 'snakeyaml-2.1.jar')
            with zipfile.ZipFile(snake) as z:
                entries.update({n: z.read(n) for n in z.namelist() if n.startswith('org/yaml/snakeyaml/') and n.endswith('.class')})
        artifact = out / f'HubPilot-{c.title()}-1.0.2-reconstructed.jar'
        temporary = artifact.with_suffix('.jar.tmp')
        with zipfile.ZipFile(temporary, 'w', compression=zipfile.ZIP_STORED) as z:
            for name, data in sorted(entries.items()):
                info = zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0))
                info.external_attr = 0o100644 << 16
                z.writestr(info, data)
        temporary.replace(artifact)
        reports.append({'component': c, 'artifact': artifact.name, 'sha256': digest(artifact.read_bytes()),
                        'entries': {n: digest(data) for n, data in sorted(entries.items())}})
    report = {'compiler': version, 'source_count': len(sources), 'dependencies_lock_sha256': digest((ROOT / 'reconstruction/dependencies.lock.json').read_bytes()),
              'sources': {p.relative_to(ROOT).as_posix(): digest(p.read_bytes()) for p in sources}, 'artifacts': reports}
    (out / 'build-manifest.json').write_text(json.dumps(report, indent=2) + '\n')
    print(f'Built {len(reports)} source-only candidates in {out}; NOT approved for deployment')


if __name__ == '__main__':
    try:
        main()
    except subprocess.CalledProcessError as error:
        raise SystemExit(error.returncode)
