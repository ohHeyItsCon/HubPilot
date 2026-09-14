#!/usr/bin/env python3
"""Build isolated acceptance probes only; never package them in suite artifacts."""
import argparse,json,os,pathlib,subprocess,zipfile
r=pathlib.Path(__file__).resolve().parents[1];p=argparse.ArgumentParser();p.add_argument('--jdk',type=pathlib.Path,required=True);a=p.parse_args()
out=r/'build/live-probe';out.mkdir(exist_ok=True);deps=[r/'build/deps'/e['name'] for e in json.loads((r/'reconstruction/dependencies.lock.json').read_text())]
subprocess.run([str(a.jdk.resolve()/'bin/javac'),'-proc:none','--release','21','-cp',os.pathsep.join(map(str,deps)),'-d',str(out),*map(str,sorted((r/'tests/live').glob('*.java')))],check=True)
for name in ['ReadinessProbe','VelocityReadinessProbe']:
 with zipfile.ZipFile(out/(name+'.jar'),'w') as z:
  path='dev/hubpilot/readiness/'+name+'.class';z.write(out/path,path)
  if name=='ReadinessProbe':z.writestr('plugin.yml','name: ReadinessProbe\nversion: 1\nmain: dev.hubpilot.readiness.ReadinessProbe\napi-version: "1.21"\ndepend: [HubPilot, HubPilotLink, HubPilotInteract]\n')
  else:z.writestr('velocity-plugin.json',json.dumps({'id':'readiness-probe','name':'Readiness Probe','version':'1','main':'dev.hubpilot.readiness.VelocityReadinessProbe','dependencies':[{'id':'hubpilot-core','optional':False}]}))
print('Built disposable probes; do not install in production')
