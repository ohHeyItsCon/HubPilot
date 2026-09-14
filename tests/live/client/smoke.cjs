if (process.env.HUBPILOT_ISOLATED_PROTOCOL_TEST !== 'true') throw new Error('Isolated-lab opt-in required');
const mc = require('minecraft-protocol');
let joined = false;
const client = mc.createClient({host:'127.0.0.1',port:25578,username:'ReadinessBot',auth:'offline',version:'1.21.10'});
const deadline = setTimeout(() => { console.log('FAIL login deadline');client.end();process.exitCode=1; },45000);
client.on('login', data => { joined=true;console.log('PASS offline protocol client entered play via Velocity');clearTimeout(deadline);setTimeout(()=>client.end(),18000); });
client.on('error', error => {console.log('ERROR',error.message);process.exitCode=1;});
client.on('disconnect', packet=>console.log('DISCONNECT',JSON.stringify(packet)));
client.on('end', reason => {clearTimeout(deadline);console.log('END',joined,reason);if(!joined)process.exitCode=1;});
