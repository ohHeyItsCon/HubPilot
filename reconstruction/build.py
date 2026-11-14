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
VERSION = (ROOT / 'VERSION').read_text().strip()


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
    generated = out / 'generated'
    generated.mkdir(exist_ok=True)
    version_sources = []
    for component in COMPONENTS:
        source = generated / component / 'BuildVersion.java'
        source.parent.mkdir(exist_ok=True)
        source.write_text(f'package dev.hubpilot.{component};\npublic final class BuildVersion {{ private BuildVersion() {{}} public static final String VERSION = "{VERSION}"; }}\n')
        version_sources.append(source)
    command = [javac, '--release', '21', '-proc:none', '-g', '-encoding', 'UTF-8', '-Xmaxerrs', '500',
               '-cp', os.pathsep.join(map(str, jars)), '-d', str(classes), *map(str, sources), *map(str, version_sources)]
    subprocess.run(command, check=True)
    # Relocate with ASM's class-aware remapper, including descriptors and stack maps.
    relocator = ROOT / 'reconstruction/Relocate.java'
    subprocess.run([javac, '-proc:none', '-cp', os.pathsep.join(map(str, jars)), '-d', str(out), str(relocator)], check=True)
    java = str(Path(javac).with_name('java'))
    subprocess.run([java, '-cp', os.pathsep.join([str(out), *map(str, jars)]),
                    'Relocate', str(classes), str(next(p for p in jars if p.name == 'snakeyaml-2.7.jar'))], check=True)
    reports = []
    for c in COMPONENTS:
        entries = {str(p.relative_to(classes)).replace(os.sep, '/'): p.read_bytes()
                   for p in (classes / 'dev/hubpilot' / c).rglob('*.class')}
        resources = ROOT / c / 'src/main/resources'
        for p in resources.rglob('*'):
            if p.is_file():
                entries[p.relative_to(resources).as_posix()] = p.read_bytes()
                if p.name in ('plugin.yml', 'velocity-plugin.json'):
                    entries[p.relative_to(resources).as_posix()] = p.read_text().replace('1.1.0-beta.1', VERSION).encode()
        if c == 'core':
            entries.update({p.relative_to(classes).as_posix(): p.read_bytes()
                            for p in (classes / 'dev/hubpilot/libs/snakeyaml').rglob('*.class')})
            with zipfile.ZipFile(next(p for p in jars if p.name == 'snakeyaml-2.7.jar')) as dependency:
                for name in dependency.namelist():
                    if name.startswith('META-INF/maven/org.yaml/snakeyaml/') and not name.endswith('/'):
                        entries[name] = dependency.read(name)
                for source, target in [('META-INF/LICENSE.txt', 'META-INF/licenses/snakeyaml/LICENSE.txt'),
                                       ('META-INF/NOTICE.txt', 'META-INF/licenses/snakeyaml/NOTICE.txt')]:
                    if source in dependency.namelist():
                        entries[target] = dependency.read(source)
        artifact = out / f'HubPilot-{c.title()}-{VERSION}.jar'
        temporary = artifact.with_suffix('.jar.tmp')
        with zipfile.ZipFile(temporary, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as z:
            for name, data in sorted(entries.items()):
                info = zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0))
                info.external_attr = 0o100644 << 16
                info.compress_type = zipfile.ZIP_DEFLATED
                z.writestr(info, data)
        temporary.replace(artifact)
        reports.append({'component': c, 'artifact': artifact.name, 'sha256': digest(artifact.read_bytes()),
                        'entries': {n: digest(data) for n, data in sorted(entries.items())}})
    report = {'version': VERSION, 'build_inputs': {p.relative_to(ROOT).as_posix(): digest(p.read_bytes()) for p in [ROOT / 'VERSION', ROOT / 'reconstruction/build.py', relocator]}, 'compiler': version, 'source_count': len(sources), 'dependencies_lock_sha256': digest((ROOT / 'reconstruction/dependencies.lock.json').read_bytes()),
              'sources': {p.relative_to(ROOT).as_posix(): digest(p.read_bytes()) for p in sources}, 'artifacts': reports}
    (out / 'build-manifest.json').write_text(json.dumps(report, indent=2) + '\n')
    print(f'Built {len(reports)} source-only candidates in {out}; NOT approved for deployment')


if __name__ == '__main__':
    try:
        main()
    except subprocess.CalledProcessError as error:
        raise SystemExit(error.returncode)
