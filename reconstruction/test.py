#!/usr/bin/env python3
"""Differential isolated lifecycle characterization: same test class, two separate JVMs."""
import argparse
import json
import os
from pathlib import Path
import subprocess

ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('--jdk',type=Path,required=True);a=p.parse_args()
lock=json.loads((ROOT/'reconstruction/dependencies.lock.json').read_text())
deps=[str(ROOT/'build/deps'/e['name']) for e in lock]
classes=ROOT/'build/test-classes';classes.mkdir(exist_ok=True)
candidate=ROOT/'build/candidate/HubPilot-Core-1.0.2-reconstructed.jar'
cp=os.pathsep.join([str(candidate),*deps])
sources=sorted((ROOT/'tests/src').rglob('*.java'))
subprocess.run([str(a.jdk/'bin/javac'),'--release','21','-proc:none','-cp',cp,'-d',str(classes),*map(str,sources)],check=True)
outputs=[]
for label,jar in [('baseline',ROOT/'build/baseline/HubPilot-Core-1.0.2.jar'),('reconstructed',candidate)]:
 cp=os.pathsep.join([str(classes),str(jar),*deps])
 r=subprocess.run([str(a.jdk/'bin/java'),'-Xverify:all','-cp',cp,'dev.hubpilot.core.LifecycleCharacterization'],capture_output=True,text=True,timeout=90)
 (ROOT/f'build/{label}-lifecycle.log').write_text(r.stdout+r.stderr)
 print(label+':\n'+r.stdout+r.stderr,flush=True)
 if r.returncode: raise SystemExit(r.returncode)
 outputs.append(r.stdout)
assert outputs[0]==outputs[1], 'Differential lifecycle results differ'
print('PASS identical baseline/reconstruction lifecycle observations (isolated platform doubles and real loopback provider HTTP)')
