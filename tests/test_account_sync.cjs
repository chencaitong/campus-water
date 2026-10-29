const fs=require('fs'),vm=require('vm'),assert=require('assert');
const source=fs.readFileSync(require('path').join(__dirname,'../res/raw/account_sync.js'),'utf8');
async function test(fail=false){let calls=0;const context={window:{sysConfig:{baseUrl:'https://school.invalid/api'}},localStorage:{getItem:k=>({AccNum:'TEST_ACCOUNT',CardEpid:'2',AccName:'Test User'})[k]||null},document:{querySelector:()=>({__vue__:{$http:{post:async(url,args)=>{
assert.equal(args.AccNum,'TEST_ACCOUNT');
if(url.endsWith('GetWalletMoney'))return{code:1,list:[{ewalletName:'卡1',ewalletMoney:'12.34'}]};
if(url.endsWith('GetConsumptionDetail'))return{code:1,businessName:'生活用水',deviceName:'示例饮水机'};
calls++;if(fail)return{code:0,msg:'测试服务错误'};
assert.equal(args.BeginRecNum,calls===1?1:101);
const length=calls===1?100:1;
return{code:1,count:101,list:[{month:'202610',dealDetail:Array.from({length},(_,i)=>({payRecNum:String(args.BeginRecNum+i),dealDate:'28日',dealTime:'10:12:30',money:i===1?0.2:-0.1,feeNumStr:i===2?'食堂消费':'生活用水',locationNumStr:'示例楼饮水机'}))}]};
}}}})} };
vm.runInNewContext(source,context);
for(let i=0;i<20&&!['ok','error'].includes(context.window.__accountResult?.state);i++)await new Promise(r=>setImmediate(r));
const result=context.window.__accountResult;if(fail){assert.equal(result.state,'error');return;}
assert.equal(result.state,'ok');assert.equal(calls,2);assert.equal(result.records.length,101);assert.equal(result.records[0].date,'2026-10-28');assert.equal(result.records[0].time,'10:12:30');assert.equal(result.records[0].amount,'0.10');assert.equal(result.records[1].signedAmount,0.2);assert.equal(result.wallets[0].ewalletMoney,'12.34');assert.equal(result.complete,true);
}
(async()=>{await test();await test(true);console.log('PASS: pagination, amounts, timestamps, refund signs, wallet values, API errors');})();
