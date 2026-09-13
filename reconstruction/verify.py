#!/usr/bin/env python3
"""Source build, packaging/ABI/linkage/behavior checks and a second independent output build."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import sys
ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--jdk',type=Path,required=True);p.add_argument('--offline',action='store_true');p.add_argument('--fixtures',action='store_true');args=p.parse_args()
def run(script,*options): subprocess.run([sys.executable,str(ROOT/'reconstruction'/script),*map(str,options)],cwd=ROOT,check=True)
opts=['--jdk',args.jdk]+(['--offline'] if args.offline else [])
baseline=ROOT/'build/baseline'
if args.offline:
    import importlib.util
    spec=importlib.util.spec_from_file_location('build',ROOT/'reconstruction/build.py');b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)
    releases=json.loads((ROOT/'development/handoff-2026-09-13/public-artifacts.json').read_text())['releases']
    for asset in next(r for r in releases if r['tag_name']=='v1.0.2')['assets']:
        b.fetch({'name':asset['name'],'url':asset['browser_download_url'],'sha256':asset['sha256']},baseline,True)
subprocess.run([sys.executable,str(ROOT/'development/handoff-2026-09-13/recover_public_release.py'),'--output',str(baseline)],check=True)
run('build.py',*opts)
run('verify-packages.py');run('verify-abi.py');run('verify-bytecode.py','--jdk',args.jdk);run('test.py','--jdk',args.jdk)
if args.fixtures:run('fixtures.py',*opts)
for component,folder in [('hub','hub-src'),('interact','src')]:
 for original in (ROOT/'development/1.0.2'/folder).rglob('*.java'):
  copy=ROOT/component/'src/main/java'/original.relative_to(ROOT/'development/1.0.2'/folder)
  assert copy.read_bytes()==original.read_bytes(),f'Replacement source drift: {copy}'
run('build.py',*opts,'--output',ROOT/'build/reproducibility-check')
hashes={}
for first in sorted((ROOT/'build/candidate').glob('*.jar')):
 second=ROOT/'build/reproducibility-check'/first.name
 assert first.read_bytes()==second.read_bytes(),f'Repeat build differs: {first.name}'
 hashes[first.name]=hashlib.sha256(first.read_bytes()).hexdigest()
(ROOT/'build/candidate/reproducibility.json').write_text(json.dumps({'same_pinned_environment_fresh_output_builds_identical':True,'hashes':hashes},indent=2)+'\n')
print('PASS all gates and two byte-identical fresh-output builds; NOT a deployment or whole-network acceptance test')
