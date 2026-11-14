#!/usr/bin/env python3
"""Beta package gate, separate from historical reconstruction byte-equivalence gates."""
from pathlib import Path
import json, zipfile
ROOT = Path(__file__).resolve().parents[1]
version = (ROOT / 'VERSION').read_text().strip()
for component in ('Core', 'Hub', 'Interact', 'Link'):
    artifact = ROOT / f'build/candidate/HubPilot-{component}-{version}.jar'
    with zipfile.ZipFile(artifact) as jar:
        assert jar.testzip() is None
        names = jar.namelist()
        for name in names:
            assert not name.startswith(('org/', 'com/', 'net/', 'tests/')), name
            if name.endswith('.class'):
                assert b'org/yaml/snakeyaml' not in jar.read(name), name
                assert name.startswith(f'dev/hubpilot/{component.lower()}/') or (component == 'Core' and name.startswith('dev/hubpilot/libs/snakeyaml/')), name
        descriptor = 'velocity-plugin.json' if component == 'Core' else 'plugin.yml'
        text = jar.read(descriptor).decode()
        if component == 'Core':
            assert json.loads(text)['version'] == version
            assert any(n.startswith('dev/hubpilot/libs/snakeyaml/') for n in names)
            assert b'version=2.7' in jar.read('META-INF/maven/org.yaml/snakeyaml/pom.properties')
            assert b'SnakeYAML 2.7' in jar.read('META-INF/licenses/snakeyaml/NOTICE.txt')
            assert 'META-INF/licenses/snakeyaml/LICENSE.txt' in names
        else:
            assert version in text
        assert all(jar.getinfo(n).compress_type == zipfile.ZIP_DEFLATED for n in names)
    print(f'PASS {artifact.name}: {artifact.stat().st_size:,} bytes; namespace, descriptor, compression and integrity')
