#!/usr/bin/env python3
"""Check named own-class binary members; report compiler metadata differences separately."""
import json
from pathlib import Path
import zipfile
from classfile import parse
ROOT=Path(__file__).resolve().parents[1]
report=[];fail=[]
for c in ['Core','Hub','Link','Interact']:
 with zipfile.ZipFile(ROOT/f'build/baseline/HubPilot-{c}-1.0.2.jar') as b,zipfile.ZipFile(ROOT/f'build/candidate/HubPilot-{c}-1.0.2-reconstructed.jar') as a:
  for n in b.namelist():
   if not n.startswith('dev/hubpilot/') or not n.endswith('.class'):continue
   old,new=parse(b.read(n)),parse(a.read(n));diff={'class':n}
   for kind in ['fields','methods']:
    def keys(data):return {(m['name'],m['descriptor']) for m in data[kind] if not m['synthetic'] and not m['name'].startswith('lambda$')}
    missing=sorted(keys(old)-keys(new));extra=sorted(keys(new)-keys(old));diff[kind]={'missing':missing,'extra':extra}
    if missing:fail.append((n,kind,missing))
    oldmembers={(m['name'],m['descriptor']):m for m in old[kind]};newmembers={(m['name'],m['descriptor']):m for m in new[kind]}
    for key in oldmembers.keys()&newmembers.keys():
     if (oldmembers[key]['access']&0x00df)!=(newmembers[key]['access']&0x00df):fail.append((n,kind,key,'access flags'))
   if old['parent']!=new['parent'] or old['interfaces']!=new['interfaces']:fail.append((n,'inheritance'))
   oldrefs=set(map(tuple,old['refs']));newrefs=set(map(tuple,new['refs']))
   diff['added_member_references']=sorted(newrefs-oldrefs);diff['removed_member_references']=sorted(oldrefs-newrefs)
   report.append(diff)
(ROOT/'build/candidate/abi-comparison.json').write_text(json.dumps(report,indent=2)+'\n')
print('Missing named binary members/inheritance changes:',json.dumps(fail));print('Class count:',len(report))
raise SystemExit(bool(fail))
