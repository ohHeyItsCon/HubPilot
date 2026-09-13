#!/usr/bin/env python3
"""Check baseline failure and candidate Link icon on both pinned Paper API generations."""
import argparse,json,os,pathlib,subprocess
r=pathlib.Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('--jdk',type=pathlib.Path,required=True);p.add_argument('--fixed',action='store_true');a=p.parse_args();j=a.jdk.resolve()/'bin'
deps=[r/'build/deps'/x['name'] for x in json.loads((r/'reconstruction/dependencies.lock.json').read_text())]
c=r/'build/compatibility-classes';c.mkdir(exist_ok=True)
subprocess.run([str(j/'javac'),'-proc:none','-cp',os.pathsep.join(map(str,deps)),'-d',str(c),str(r/'tests/compatibility/ChainCompatibility.java')],check=True)
for label in ['baseline','candidate']:
 for api in ['1.21.8','1.21.10']:
  jars=list((r/'build/deps').glob('paper-api-'+api+'*.jar'));assert len(jars)==1
  d=[x for x in deps if not x.name.startswith('paper-api')]+jars
  suffix='1.0.2' if label=='baseline' else '1.0.2-reconstructed'
  fixed=a.fixed and label=='candidate';missing=api=='1.21.10' and not fixed
  cp=os.pathsep.join(map(str,[c,r/f'build/{label}/HubPilot-Hub-{suffix}.jar',*d]))
  print(label,api,flush=True)
  subprocess.run([str(j/'java'),'-cp',cp,'ChainCompatibility',str(missing).lower(),'IRON_CHAIN' if api=='1.21.10' else 'CHAIN'],check=True)
