#!/usr/bin/env python3
import argparse
import json
import os
from pathlib import Path
import subprocess
ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('--jdk',type=Path,required=True);a=p.parse_args()
lock=json.loads((ROOT/'reconstruction/dependencies.lock.json').read_text())
deps=[str(ROOT/'build/deps'/e['name']) for e in lock]
classes=ROOT/'build/inspection-classes';classes.mkdir(exist_ok=True)
cp=os.pathsep.join(deps)
subprocess.run([str(a.jdk/'bin/javac'),'--release','21','-proc:none','-cp',cp,'-d',str(classes),*map(str,sorted((ROOT/'tests/inspection').glob('*.java')))],check=True)
cp=os.pathsep.join([str(classes),*deps])
for label,jar in [('baseline',ROOT/'build/baseline/HubPilot-Core-1.0.2.jar'),('upstream',ROOT/'build/deps/snakeyaml-2.1.jar')]:
 subprocess.run([str(a.jdk/'bin/java'),'-cp',cp,'BytecodeInventory',str(jar),'org/yaml/',str(ROOT/f'build/{label}-snakeyaml-code.json')],check=True)
x=json.loads((ROOT/'build/baseline-snakeyaml-code.json').read_text());y=json.loads((ROOT/'build/upstream-snakeyaml-code.json').read_text());assert x==y,'SnakeYAML executable bytecode differs'
print(f'PASS normalized executable code and exception handlers: {len(x)} SnakeYAML classes')
for label in ['baseline','candidate']:
 suffix='1.0.2' if label=='baseline' else '1.0.2-reconstructed'
 jars=[str(ROOT/f'build/{label}/HubPilot-{c}-{suffix}.jar') for c in ['Core','Hub','Link','Interact']]
 r=subprocess.run([str(a.jdk/'bin/java'),'-Xverify:all','-cp',os.pathsep.join([str(classes),*jars,*deps]),'LinkageInventory',*jars],text=True,capture_output=True)
 (ROOT/f'build/{label}-linkage.log').write_text(r.stdout+r.stderr)
 print(label+': '+r.stdout+r.stderr,flush=True)
 if r.returncode:raise SystemExit(r.returncode)
(ROOT/'build/candidate/dependency-bytecode-verification.json').write_text(json.dumps({'snakeyaml_classes':len(x),'normalized_code_equal':True,'scope':'instructions, constants, control-flow targets, ordered exception handlers and member flags; excludes debug/stack-map encoding'},indent=2)+'\n')
