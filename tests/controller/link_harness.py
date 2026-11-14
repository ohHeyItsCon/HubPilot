"""Runs a real HubPilot Controller API (its own code, in-process, over TLS) with the freshly built Core
JAR installed on a fake proxy, plays the owner, and checks what the Controller recorded.
Usage: python3 link_harness.py <controller-src> <core-jar> <workdir>"""
import json, ssl, subprocess, sys, threading, time, urllib.request, tempfile
from pathlib import Path
CTRL, JAR, W = Path(sys.argv[1]), Path(sys.argv[2]), Path(sys.argv[3])
sys.path.insert(0, str(CTRL))
from controller.server import Server, Handler
from controller.store import Store
from controller.auth import Auth
from controller.engine import Engine
from controller.files import Files
from controller.addons import Addons
from controller.hubpilot import HubPilot
from controller.proxy import Proxy
from tests.test_engine import FakeRuntime, SID

tmp = Path(tempfile.mkdtemp()); store = Store(tmp); runtime = FakeRuntime()
runtime.registry['servers'][SID].update(kind='proxy', port=25590)
runtime.registry['servers'][SID]['software'] = {'loader': 'Velocity', 'proxyVersion': '3.4.0'}
runtime.inspect = lambda sid: {'State': {'Running': True}}
engine = Engine(store, runtime)
srv = Server(('127.0.0.1', 0), Handler); port = srv.server_port; base = f'https://127.0.0.1:{port}'
srv.origin = base; srv.allowed_hosts = {f'127.0.0.1:{port}'}
games = tmp / 'games'; plugins = games / SID / 'plugins'; (plugins / 'hubpilot-core').mkdir(parents=True)
(plugins / JAR.name).write_bytes(JAR.read_bytes())
(plugins / 'hubpilot-core' / 'setup.yml').write_text('setup-version: 1\ncomplete: true\nprovider-skipped: false\n')
(plugins / 'hubpilot-core' / 'staff.yml').write_text('owners:\n  - uuid: "fd518eff-0000-4000-8000-000000000002"\n    username: "Owner"\nstaff:\n  []\n')
srv.store, srv.auth, srv.engine, srv.runtime = store, Auth(store), engine, runtime
srv.files = Files(engine, games); srv.addons = Addons(srv); srv.hubpilot = HubPilot(srv); srv.proxy = Proxy(srv)
subprocess.run(['openssl', 'req', '-x509', '-newkey', 'rsa:2048', '-nodes', '-keyout', tmp / 'k.pem', '-out', tmp / 'c.pem', '-days', '1', '-subj', '/CN=hpc-test'], check=True, capture_output=True)
ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER); ctx.load_cert_chain(tmp / 'c.pem', tmp / 'k.pem')
srv.socket = ctx.wrap_socket(srv.socket, server_side=True)
threading.Thread(target=srv.serve_forever, kwargs={'poll_interval': 0.01}, daemon=True).start()
client = ssl.create_default_context(); client.check_hostname = False; client.verify_mode = ssl.CERT_NONE  # harness only
cookie = None
def call(path, method='GET', body=None):
    global cookie
    h = {'Content-Type': 'application/json', 'Origin': base}
    if cookie: h['Cookie'] = cookie
    req = urllib.request.Request(base + '/api/v1' + path, headers=h, method=method, data=json.dumps(body).encode() if body is not None else None)
    try: r = urllib.request.urlopen(req, timeout=5, context=client)
    except urllib.error.HTTPError as e: r = e
    data = json.load(r)
    if r.headers.get('Set-Cookie'): cookie = r.headers['Set-Cookie'].split(';')[0]
    return r.status, data
call('/auth/setup', 'POST', {'code': (tmp / 'bootstrap-code').read_text().strip(), 'name': 'owner', 'password': 'a secure password 123'})
SID_JSON = json.dumps(SID)
(W / 'url').write_text(base); (W / 'sid').write_text(SID)
st, link = call('/hubpilot'); print('controller sees Core:', link.get('link', {}).get('state'), '|', link.get('link', {}).get('reason', ''))
for _ in range(150):
    if (W / 'challenge').exists(): break
    time.sleep(0.2)
st, pk = call('/pairing', 'POST', {'name': 'HubPilot link test', 'challenge': (W / 'challenge').read_text()}); (W / 'code').write_text(pk['code'])
st, lp = call('/hubpilot/pairing', 'POST', {}); print('link pairing code issued:', st, (lp.get('code') or lp)[:8] if isinstance(lp.get('code'), str) else lp)
(W / 'linkcode').write_text(lp.get('code', ''))
for _ in range(300):
    if (W / 'reported').exists(): break
    time.sleep(0.2)
time.sleep(1.5)
st, state = call('/hubpilot'); L = state['link']
print('link state after Core claimed:', L['state'], '| core', (L.get('session') or {}).get('coreVersion'), '| last contact', (L.get('session') or {}).get('lastCommunication') is not None)
tr = state.get('transfers')
print('transfers:', json.dumps(tr)[:900])
(W / 'done').write_text('1')
