(function () {
  if (window.__accountSyncStarted) return;
  var app = document.querySelector('#app');
  var vm = app && app.__vue__;
  var acc = localStorage.getItem('AccNum');
  if (!vm || !vm.$http || !acc || !window.sysConfig) {
    window.__accountResult={state:'waiting',hasVM:!!vm,hasHTTP:!!(vm&&vm.$http),hasAcc:!!acc,hasConfig:!!window.sysConfig};return;
  }
  window.__accountSyncStarted = true;
  window.__accountResult = {state: 'loading'};
  (async function () {
    try {
      var base = window.sysConfig.baseUrl;
      var ep = localStorage.getItem('CardEpid') || localStorage.getItem('Epid');
      var wallet = await vm.$http.post(base + '/GetWalletMoney', {AccNum: acc, EPID: ep});
      if (Number(wallet.code) !== 1 || !Array.isArray(wallet.list)) throw new Error(wallet.msg || '余额查询失败');
      window.__accountResult={state:'loading',stage:'账单',wallets:wallet.list};
      var records = [], start = 1, total = null;
      for (var page = 0; page < 200; page++) {
        var result = await vm.$http.post(base + '/GetDealRec', {
          AccNum: acc, EPID: 0, CardAccNum: '-1', WalletNum: '0',
          TypeNum: '-7', BeginRecNum: start, Count: 100
        });
        if (Number(result.code) !== 1) throw new Error(result.msg || '账单查询失败');
        var groups = result.list || [], count = 0;
        groups.forEach(function (group) {
          (group.dealDetail || []).forEach(function (item) {
            count++;
            var fee = String(item.feeNumStr || ''), location = String(item.locationNumStr || '');
            var month = String(group.month || ''), day = String(item.dealDate || '').replace(/日/g, '');
            var date = /^\d{6}$/.test(month) && /^\d{1,2}$/.test(day)
              ? month.substring(0,4)+'-'+month.substring(4,6)+'-'+('0'+day).slice(-2) : String(item.dealDate || '');
            var time = String(item.dealTime || '');
            var money = Number(item.money);
            if (!Number.isFinite(money)) return;
            records.push({id: String(item.payRecNum || ''), date: date, time: time,
              amount: Math.abs(money).toFixed(2), signedAmount: money,
              fee: fee, location: location, water: /用水|饮水|水费|热水|冷水|洗浴|淋浴|浴室/.test(fee+' '+location)});
          });
        });
        total = Number(result.count);
        start += count;
        window.__accountResult={state:'loading',stage:'账单',wallets:wallet.list,loaded:records.length};
        if (!count || (Number.isFinite(total) && start > total) || count < 100) break;
      }
      var known=window.__knownBillDetails||{}, detailIndex=0;
      async function enrich(){
        while(detailIndex<records.length){
          var row=records[detailIndex++];
          if(known[row.id]&&known[row.id].detailChecked){Object.assign(row,known[row.id]);continue;}
          if(!row.id){row.detailChecked=false;continue;}
          try{
            var detail=await vm.$http.post(base+'/GetConsumptionDetail',{PayRecNum:row.id});
            if(Number(detail.code)===1){
              row.location=[detail.businessName,detail.deviceName,detail.remark].filter(Boolean).join(' · ');
              row.water=/用水|饮水|水费|热水|冷水|洗浴|淋浴|浴室|水控/.test(row.fee+' '+row.location);
              row.detailChecked=true;
            }
          }catch(ignored){row.detailChecked=false;}
          window.__accountResult={state:'loading',stage:'交易详情',wallets:wallet.list,loaded:detailIndex,total:records.length};
        }
      }
      await Promise.all([enrich(),enrich(),enrich(),enrich()]);
      var session = {};
      ['AccNum','CardEpid','Epid','AccName','CardAccNum','IsScp'].forEach(function(k){
        var value=localStorage.getItem(k); if(value!==null)session[k]=value;
      });
      window.__accountResult = {state:'ok', account:acc, name:localStorage.getItem('AccName') || '学校一卡通',
        session:session, wallets:wallet.list, records:records, complete:!count || count<100 || start>total};
    } catch (e) {
      window.__accountResult={state:'error', message:e.message || '学校服务暂时不可用'};
    }
  })();
})();
