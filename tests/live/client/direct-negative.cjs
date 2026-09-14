if (process.env.HUBPILOT_ISOLATED_PROTOCOL_TEST !== 'true') throw new Error('Isolated-lab opt-in required');
const mc=require('minecraft-protocol'); let rejected=false,joined=false;
const client=mc.createClient({host:'127.0.0.1',port:25576,username:'ReadinessDirect',auth:'offline',version:'1.21.10'});
const timer=setTimeout(()=>{console.log('FAIL rejection deadline');process.exit(1);},45000);
client.on('login',()=>{joined=true;console.log('FAIL direct backend entered play');client.end();});
client.on('disconnect',packet=>{const reason=JSON.stringify(packet);rejected=/velocity/i.test(reason);console.log('direct rejection',reason);});
client.on('error',error=>{console.log('ERROR',error.message);process.exitCode=1;});
client.on('end',()=>{clearTimeout(timer);console.log(rejected&&!joined?'PASS direct backend requires Velocity':'FAIL unproven rejection');if(!rejected||joined)process.exitCode=1;});
