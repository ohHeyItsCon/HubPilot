#!/usr/bin/env python3
"""Materialize the hash-pinned Linux x64 JDK under ignored build/, without host installation."""
import argparse
import hashlib
import json
from pathlib import Path
import tarfile
import urllib.request
ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--cache',type=Path,default=ROOT/'build/toolchain');a=p.parse_args()
entry=json.loads((ROOT/'reconstruction/toolchain.lock.json').read_text())['java']
a.cache.mkdir(parents=True,exist_ok=True);root=a.cache.resolve();archive=root/entry['name']
if not archive.exists():
 data=urllib.request.urlopen(entry['url'],timeout=60).read()
 if hashlib.sha256(data).hexdigest()!=entry['sha256']:raise RuntimeError('JDK archive checksum mismatch')
 archive.write_bytes(data)
if hashlib.sha256(archive.read_bytes()).hexdigest()!=entry['sha256']:raise RuntimeError('Existing JDK archive checksum mismatch')
target=root/entry['directory']
if target.exists():raise RuntimeError(f'Refusing to overwrite existing JDK: {target}; use --jdk with the existing path or a fresh --cache')
with tarfile.open(archive) as tar:
 for member in tar.getmembers():
  path=(root/member.name).resolve()
  if not path.is_relative_to(root) or member.name.startswith('/') or member.isdev():raise RuntimeError('Unsafe toolchain archive entry')
  if member.issym() or member.islnk():
   link=((path.parent if member.issym() else root)/member.linkname).resolve()
   if not link.is_relative_to(root):raise RuntimeError('Unsafe toolchain link')
 tar.extractall(root)
print(target)
