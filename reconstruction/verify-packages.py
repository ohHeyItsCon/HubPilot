#!/usr/bin/env python3
"""Compare candidate packaging with the public baseline; never accepts class overlays as a build."""
import argparse
import hashlib
import json
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]

def sha(data): return hashlib.sha256(data).hexdigest()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--candidate', type=Path, default=ROOT/'build/candidate')
    parser.add_argument('--baseline', type=Path, default=ROOT/'build/baseline')
    args = parser.parse_args()
    reports = []
    for component in ['Core','Hub','Link','Interact']:
        with zipfile.ZipFile(args.baseline/f'HubPilot-{component}-1.0.2.jar') as base, zipfile.ZipFile(args.candidate/f'HubPilot-{component}-1.0.2-reconstructed.jar') as candidate:
            names = candidate.namelist()
            assert len(names) == len(set(names)) and candidate.testzip() is None, component
            before = {n for n in base.namelist() if not n.endswith('/')}
            after = set(names)
            missing, extra = sorted(before-after), sorted(after-before)
            assert not missing and not extra, (component, missing, extra)
            changed_resources = [n for n in sorted(before) if not n.endswith('.class') and base.read(n)!=candidate.read(n)]
            assert not changed_resources, (component, changed_resources)
            for name in names:
                if name.endswith('.class'):
                    assert name.startswith('dev/hubpilot/'+component.lower()+'/') or (component=='Core' and name.startswith('org/yaml/snakeyaml/')), name
                    if name.startswith('org/yaml/'):
                        with zipfile.ZipFile(ROOT/'build/deps/snakeyaml-2.1.jar') as dependency:
                            assert dependency.read(name)==candidate.read(name), name
            descriptor = 'velocity-plugin.json' if component=='Core' else 'plugin.yml'
            for required in [descriptor,'META-INF/licenses/hubpilot/LICENSE.txt','META-INF/licenses/autoserver/LICENSE.txt']:
                assert required in after, (component,required)
            reports.append({'component':component,'entries':len(after),'resources_identical':True,'class_entry_set_identical':True,'dependency_bytes':'pinned Maven SnakeYAML 2.1; normalized bytecode comparison is a separate required check',
                            'changed_classes':[n for n in sorted(before) if n.endswith('.class') and base.read(n)!=candidate.read(n)]})
    (args.candidate/'packaging-verification.json').write_text(json.dumps(reports,indent=2)+'\n')
    print('PASS four candidate archives: exact entry sets, resources/notices and dependency class bytes; no platform/test classes')

if __name__=='__main__': main()
