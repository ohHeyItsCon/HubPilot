#!/usr/bin/env python3
"""Run the two unchanged historical fixtures on both binary sets with the newly pinned test environment."""
import argparse
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('build',ROOT/'reconstruction/build.py');build=importlib.util.module_from_spec(spec);spec.loader.exec_module(build)
p=argparse.ArgumentParser();p.add_argument('--jdk',type=Path,required=True);p.add_argument('--offline',action='store_true');args=p.parse_args()
base=json.loads((ROOT/'reconstruction/dependencies.lock.json').read_text());fixture=json.loads((ROOT/'reconstruction/fixtures.lock.json').read_text())
lock=fixture+[x for x in base if not x['name'].startswith(('paper-api-','snakeyaml-'))]
paths=[str(build.fetch(x,ROOT/'build/deps',args.offline)) for x in lock]
classes=ROOT/'build/fixture-classes';classes.mkdir(exist_ok=True)
basejars=[str(ROOT/f'build/baseline/HubPilot-{c}-1.0.2.jar') for c in ['Hub','Interact']]
cp=os.pathsep.join([*basejars,*paths])
subprocess.run([str(args.jdk/'bin/javac'),'--release','21','-proc:none','-cp',cp,'-d',str(classes),str(ROOT/'development/1.0.2/InteractValidation.java'),str(ROOT/'development/1.0.2/HubToggleValidation.java')],check=True)
for label in ['baseline','candidate']:
 suffix='1.0.2' if label=='baseline' else '1.0.2-reconstructed'
 for test,components in [('dev.hubpilot.interact.InteractValidation',['Hub','Interact']),('HubToggleValidation',['Hub'])]:
  jars=[str(ROOT/f'build/{label}/HubPilot-{c}-{suffix}.jar') for c in components]
  cp=os.pathsep.join([str(classes),*jars,*paths])
  with tempfile.TemporaryDirectory(prefix='hubpilot-retained-fixture-') as work:
   shared=Path(work)/'shared';shared.mkdir()
   r=subprocess.run([str(args.jdk.resolve()/'bin/java'),'-Xverify:all','-cp',cp,test,str(shared)],cwd=work,capture_output=True,text=True,timeout=90)
  (ROOT/f'build/{label}-{test.rsplit(".",1)[-1]}.log').write_text(r.stdout+r.stderr)
  print(label+' '+test+':\n'+r.stdout+r.stderr,flush=True)
  if r.returncode: raise SystemExit(r.returncode)
