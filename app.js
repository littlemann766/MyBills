
'use strict';
(() => {
const VERSION='21.0.1';
const K={
 bills:'myBills_v1', billOverrides:'myBills_bill_overrides_v1', paid:'myBills_bill_paid_amounts_v1',
 paychecks:'myBills_paychecks_v1', paycheckOverrides:'myBills_paycheck_overrides_v1',
 hours:'myBills_work_hours_v1', wage:'myBills_hourly_wage_v1', incomeType:'myBills_income_type_v1',
 salary:'myBills_annual_salary_v1', spending:'myBills_budget_spending_v1', budgets:'myBills_monthly_budgets_v1',
 paymentMeta:'myBills_payment_meta_v1', reminders:'myBills_reminders_v1', settings:'myBills_settings_v2',
 setupDone:'myBills_setup_complete_v1', setupProgress:'myBills_setup_progress_v1'
};
const $=id=>document.getElementById(id), $$=s=>[...document.querySelectorAll(s)];
const load=(k,f)=>{try{const v=JSON.parse(localStorage.getItem(k));return v==null?f:v}catch{return f}};
const save=(k,v)=>localStorage.setItem(k,JSON.stringify(v));
const money=n=>new Intl.NumberFormat('en-US',{style:'currency',currency:'USD'}).format(Number(n)||0);
const iso=d=>{const x=new Date(d),o=x.getTimezoneOffset();return new Date(x.getTime()-o*60000).toISOString().slice(0,10)};
const today=()=>iso(new Date());
const uid=p=>(crypto.randomUUID?crypto.randomUUID():p+'-'+Date.now()+'-'+Math.random().toString(16).slice(2));
const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const parseDate=s=>new Date(s+'T00:00:00');
const fmt=s=>parseDate(s).toLocaleDateString(undefined,{month:'short',day:'numeric',year:'numeric'});
const monthKey=(y,m)=>`${y}-${String(m+1).padStart(2,'0')}`;

const state={
 bills:load(K.bills,[]), billOverrides:load(K.billOverrides,{}), paid:load(K.paid,{}),
 paychecks:load(K.paychecks,[]), paycheckOverrides:load(K.paycheckOverrides,{}),
 hours:load(K.hours,[]), spending:load(K.spending,[]), budgets:load(K.budgets,{}),
 paymentMeta:load(K.paymentMeta,{}), reminders:load(K.reminders,[]), settings:load(K.settings,{notificationDays:3,notificationTime:'09:00'}),
 view:'home', calendarDate:new Date(), spendRange:'week', editingBill:null, editingSpend:null
};
if(!Array.isArray(state.bills))state.bills=[];
if(!Array.isArray(state.paychecks))state.paychecks=[];
if(!Array.isArray(state.hours))state.hours=[];
if(!Array.isArray(state.spending))state.spending=[];

function persist(){
 save(K.bills,state.bills);save(K.billOverrides,state.billOverrides);save(K.paid,state.paid);
 save(K.paychecks,state.paychecks);save(K.paycheckOverrides,state.paycheckOverrides);save(K.hours,state.hours);
 save(K.spending,state.spending);save(K.budgets,state.budgets);save(K.paymentMeta,state.paymentMeta);
 save(K.reminders,state.reminders);save(K.settings,state.settings);
}
function occurrenceKey(id,date){return `${id}|${date}`}
function occurs(b,date){
 const d=parseDate(date), start=parseDate(b.due); if(d<start)return false;
 if(!b.recurring)return date===b.due;
 const days=Math.round((d-start)/86400000);
 if((b.recurrence||'monthly')==='weekly')return days%7===0;
 if((b.recurrence||'monthly')==='biweekly')return days%14===0;
 return d.getDate()===start.getDate();
}
function billAmountPaid(b,date){
 const k=occurrenceKey(b.id,date);
 if(k in state.paid)return Number(state.paid[k])||0;
 const ov=state.billOverrides[k];
 if(ov && typeof ov==='object' && ov.paid)return Number(b.amount)||0;
 if(ov===true)return Number(b.amount)||0;
 return b.paid&&date===b.due?Number(b.amount)||0:0;
}
function monthBillOccurrences(y,m){
 const out=[],end=new Date(y,m+1,0).getDate();
 for(let day=1;day<=end;day++){
   const date=iso(new Date(y,m,day));
   for(const b of state.bills)if(occurs(b,date)&&!state.billOverrides[occurrenceKey(b.id,date)]?.deleted)out.push({b,date});
 }
 return out;
}
function salaryPerCheck(p){
 const annual=Number(p.annualSalary||localStorage.getItem(K.salary)||0);
 if(!annual)return 0;
 return p.frequency==='biweekly'?annual/26:p.frequency==='weekly'?annual/52:Number(p.amount)||0;
}
function hoursBetween(a,b){
 return state.hours.filter(x=>x.date>=a&&x.date<=b).reduce((s,x)=>s+(Number(x.hours)||0),0);
}
function paycheckAmount(p,date){
 const k=occurrenceKey(p.id,date),ov=state.paycheckOverrides[k];
 if(ov?.deleted)return 0;if(ov && 'amount' in ov)return Number(ov.amount)||0;
 if(p.incomeType==='salary')return Number(p.amount)||salaryPerCheck(p);
 const wage=Number(localStorage.getItem(K.wage)||0);
 if(p.frequency==='one-time')return Number(p.amount)||0;
 const span=p.frequency==='biweekly'?14:7,end=parseDate(date),start=new Date(end);start.setDate(start.getDate()-span+1);
 const h=hoursBetween(iso(start),date);return h>0?h*wage:Number(p.amount)||0;
}
function paycheckDatesForMonth(p,y,m){
 const out=[], first=parseDate(p.date); if(first>new Date(y,m+1,0))return out;
 if(p.frequency==='one-time'){if(first.getFullYear()===y&&first.getMonth()===m)out.push(p.date);return out}
 const step=p.frequency==='biweekly'?14:7;
 for(let d=new Date(first);d<=new Date(y,m+1,0);d.setDate(d.getDate()+step)){
   if(d.getFullYear()===y&&d.getMonth()===m)out.push(iso(d));
 }
 return out;
}
function monthIncome(y,m){return state.paychecks.reduce((s,p)=>s+paycheckDatesForMonth(p,y,m).reduce((a,d)=>a+paycheckAmount(p,d),0),0)}
function monthBills(y,m){return monthBillOccurrences(y,m).reduce((s,x)=>s+Number(x.b.amount||0),0)}
function monthPaid(y,m){return monthBillOccurrences(y,m).reduce((s,x)=>s+Math.min(Number(x.b.amount)||0,billAmountPaid(x.b,x.date)),0)}
function manualSigned(x){return x.type==='refund'||x.kind==='refund'?-Math.abs(Number(x.amount)||0):Math.abs(Number(x.amount)||0)}
function monthManualSpending(y,m){return state.spending.filter(x=>{const d=new Date(x.timestamp||x.date||0);return d.getFullYear()===y&&d.getMonth()===m}).reduce((s,x)=>s+manualSigned(x),0)}
function budgetFor(y,m){return Number(state.budgets[monthKey(y,m)]||0)}
function currentWeek(){
 const n=new Date(),s=new Date(n);s.setDate(n.getDate()-n.getDay());s.setHours(0,0,0,0);const e=new Date(s);e.setDate(e.getDate()+6);e.setHours(23,59,59,999);return{s,e}
}
function weekHours(){const {s,e}=currentWeek();return state.hours.filter(x=>{const d=parseDate(x.date);return d>=s&&d<=e}).reduce((a,x)=>a+(Number(x.hours)||0),0)}

function setView(name){
 state.view=name;$$('.view').forEach(v=>v.classList.toggle('active',v.dataset.view===name));
 $$('.nav button').forEach(b=>b.classList.toggle('active',b.dataset.view===name));
 renderCurrent();window.scrollTo(0,0);
}
function renderCurrent(){({home:renderHome,calendar:renderCalendar,bills:renderBills,spending:renderSpending,budget:renderBudget,settings:renderSettings}[state.view]||renderHome)()}
function renderHome(){
 const d=new Date(),y=d.getFullYear(),m=d.getMonth(),income=monthIncome(y,m),bills=monthBills(y,m),paid=monthPaid(y,m),remaining=Math.max(0,bills-paid);
 const manual=monthManualSpending(y,m),budget=budgetFor(y,m),available=(budget||income)-paid-manual;
 $('homeGrid').innerHTML=[
 ['Monthly Income',money(income),incomeTypeLabel()],['Total Bills',money(bills),'This month'],
 ['Paid',money(paid),'Recorded payments'],['Remaining',money(remaining),'Still due'],
 ['Bills',String(new Set(monthBillOccurrences(y,m).map(x=>x.b.id)).size),'Active this month'],['Available Now',money(available),budget?`Based on ${money(budget)} budget`:'Based on monthly income']
 ].map(([a,b,c])=>`<div class="card"><div class="muted">${a}</div><div class="metric">${b}</div><div class="muted">${c}</div></div>`).join('');
 $('weekHours').textContent=weekHours().toFixed(1);
 const ratio=(budget||income)>0?(paid+Math.max(0,manual))/(budget||income):0;
 $('smartTip').textContent=ratio>1?'Spending is above your current monthly amount. Review bills and spending before adding new expenses.':ratio>.8?'You are above 80% of your current monthly amount.':'Your budget is in a comfortable range right now.';
}
function incomeTypeLabel(){
 const t=localStorage.getItem(K.incomeType)||'hourly';
 return t==='salary'?`Salary: ${money(localStorage.getItem(K.salary)||0)}/yr`:`Hourly wage: ${money(localStorage.getItem(K.wage)||0)}`;
}
function renderCalendar(){
 const d=state.calendarDate,y=d.getFullYear(),m=d.getMonth();
 $('calTitle').textContent=d.toLocaleDateString(undefined,{month:'long',year:'numeric'});
 const first=new Date(y,m,1),start=new Date(first);start.setDate(first.getDate()-first.getDay());
 let html=['Sun','Mon','Tue','Wed','Thu','Fri','Sat'].map(x=>`<div class="dow">${x}</div>`).join('');
 for(let i=0;i<42;i++){const x=new Date(start);x.setDate(start.getDate()+i);const date=iso(x),events=state.bills.filter(b=>occurs(b,date));
   html+=`<button class="day ${x.getMonth()!==m?'other':''} ${date===today()?'today':''}" data-day="${date}"><span class="n">${x.getDate()}</span><span class="dots">${events.slice(0,5).map(()=>'<i class="dot"></i>').join('')}</span></button>`;
 }
 $('calendarGrid').innerHTML=html;
}
function renderBills(){
 const q=($('billSearch')?.value||'').trim().toLowerCase(),cat=$('billCategory')?.value||'';
 let rows=state.bills.filter(b=>(!q||`${b.name} ${b.category||''}`.toLowerCase().includes(q))&&(!cat||(b.category||'')===cat));
 const cats=[...new Set(state.bills.map(b=>b.category).filter(Boolean))].sort();
 const sel=$('billCategory');if(sel){const prev=sel.value;sel.innerHTML='<option value="">All categories</option>'+cats.map(c=>`<option>${esc(c)}</option>`).join('');sel.value=prev}
 rows.sort((a,b)=>a.due.localeCompare(b.due));
 $('billList').innerHTML=rows.length?rows.map(b=>{
   const date=nextOccurrence(b),paid=billAmountPaid(b,date),status=paid>=Number(b.amount)?'Paid':paid>0?'Partial':'Unpaid';
   return `<article class="item" data-bill="${b.id}"><div class="row between"><div><div class="title">${esc(b.name)}</div><div class="muted">${b.recurring?'Recurring '+esc(b.recurrence||'monthly'):'One-time'} • ${status}${b.autopay?' • Autopay':''}</div>${b.category?`<span class="pill">${esc(b.category)}</span>`:''}</div><div class="amount">${money(b.amount)}</div></div><div class="row wrap" style="margin-top:10px"><button class="btn small" data-pay="${b.id}" data-date="${date}">Payment</button><button class="btn small light" data-edit="${b.id}">Edit</button></div></article>`;
 }).join(''):'<div class="empty card">No bills match this view.</div>';
}
function nextOccurrence(b){
 let d=new Date();for(let i=0;i<400;i++){const s=iso(d);if(occurs(b,s))return s;d.setDate(d.getDate()+1)}return b.due;
}
function renderSpending(){
 const now=new Date(),{s,e}=currentWeek(),start=state.spendRange==='week'?s:new Date(now.getFullYear(),now.getMonth(),1),end=state.spendRange==='week'?e:new Date(now.getFullYear(),now.getMonth()+1,0,23,59,59,999);
 const manual=state.spending.filter(x=>{const d=new Date(x.timestamp||x.date||0);return d>=start&&d<=end}).map(x=>({...x,_kind:'manual',_date:new Date(x.timestamp||x.date)}));
 const paid=[];
 for(const [key,amount] of Object.entries(state.paid)){if(!(Number(amount)>0))continue;const [id,date]=key.split('|'),d=parseDate(date);if(d<start||d>end)continue;const b=state.bills.find(x=>x.id===id);if(b)paid.push({_kind:'bill',id,date,name:b.name,amount:Number(amount),_date:d,category:b.category||''})}
 const rows=[...manual,...paid].sort((a,b)=>b._date-a._date);
 const total=rows.reduce((s,x)=>s+(x._kind==='bill'?Math.abs(Number(x.amount)||0):manualSigned(x)),0);
 $('spendTotal').textContent=total<0?`+${money(Math.abs(total))} net returned`:`${money(total)} net spent`;
 $('spendList').innerHTML=rows.length?rows.map(x=>x._kind==='bill'
 ?`<article class="item"><div class="row between"><div><div class="title">${esc(x.name)} <span class="pill">Bill</span></div><div class="muted">Paid ${fmt(x.date)}${x.category?' • '+esc(x.category):''}</div></div><div class="amount">−${money(x.amount)}</div></div></article>`
 :`<article class="item"><div class="row between"><div><div class="title">${esc(x.description||x.name||'Spending')} <span class="pill">${x.type==='refund'||x.kind==='refund'?'Refund / Funds':'Spending'}</span></div><div class="muted">${x._date.toLocaleString()}</div></div><div class="amount ${manualSigned(x)<0?'good':''}">${manualSigned(x)<0?'+':'−'}${money(Math.abs(Number(x.amount)||0))}</div></div><div class="row" style="margin-top:10px"><button class="btn small" data-spend-edit="${x.id}">Edit</button><button class="btn small danger" data-spend-del="${x.id}">Delete</button></div></article>`).join(''):'<div class="empty card">No transactions in this period.</div>';
}
function renderBudget(){
 const d=new Date(),y=d.getFullYear(),m=d.getMonth(),income=monthIncome(y,m),budget=budgetFor(y,m);
 $('budgetAmount').value=budget||'';
 $('incomeType').value=localStorage.getItem(K.incomeType)||'hourly';toggleIncomeFields();
 $('hourlyWage').value=localStorage.getItem(K.wage)||'';
 $('annualSalary').value=localStorage.getItem(K.salary)||'';
 $('budgetSummary').innerHTML=`<div class="grid"><div class="card"><div class="muted">Projected income</div><div class="metric">${money(income)}</div></div><div class="card"><div class="muted">Monthly amount</div><div class="metric">${money(budget||income)}</div></div></div>`;
 renderHours();renderPaychecks();
}
function renderHours(){
 const rows=[...state.hours].sort((a,b)=>b.date.localeCompare(a.date)).slice(0,20);
 $('hoursList').innerHTML=rows.length?rows.map(x=>`<div class="item row between"><div><b>${fmt(x.date)}</b><div class="muted">${Number(x.hours).toFixed(2)} hours</div></div><button class="btn small danger" data-hour-del="${x.id}">Delete</button></div>`).join(''):'<div class="muted">No hours logged yet.</div>';
}
function renderPaychecks(){
 $('paycheckList').innerHTML=state.paychecks.length?state.paychecks.map(p=>`<div class="item"><div class="row between"><div><b>${p.incomeType==='salary'?'Salary':'Check income'}</b><div class="muted">${money(paycheckAmount(p,p.date))} • ${p.frequency||'weekly'} • starts ${fmt(p.date)}</div></div><button class="btn small danger" data-paycheck-del="${p.id}">Delete</button></div></div>`).join(''):'<div class="muted">No paycheck schedule yet.</div>';
}
function renderSettings(){
 const native=window.AndroidBridge&&typeof AndroidBridge.isAppLockEnabled==='function';
 let lock=false;try{lock=native?AndroidBridge.isAppLockEnabled():false}catch{}
 $('pinLock').checked=lock;$('pinLock').disabled=!native;
 $('versionText').textContent=VERSION;
}
function renderDay(date){
 const bills=state.bills.filter(b=>occurs(b,date)),checks=state.paychecks.flatMap(p=>paycheckDatesForMonth(p,parseDate(date).getFullYear(),parseDate(date).getMonth()).includes(date)?[{p,amount:paycheckAmount(p,date)}]:[]);
 $('dayTitle').textContent=fmt(date);$('dayContent').innerHTML=
 (bills.length?'<h3>Bills</h3>'+bills.map(b=>`<div class="item"><div class="row between"><b>${esc(b.name)}</b><span>${money(b.amount)}</span></div></div>`).join(''):'')+
 (checks.length?'<h3>Income</h3>'+checks.map(x=>`<div class="item"><div class="row between"><b>Paycheck</b><span>${money(x.amount)}</span></div></div>`).join(''):'')+
 (!bills.length&&!checks.length?'<div class="empty">Nothing scheduled.</div>':'');
 $('dayDlg').showModal();
}

function openBill(id=null,date=null){
 const b=id?state.bills.find(x=>x.id===id):null;state.editingBill=id;
 $('billFormTitle').textContent=b?'Edit Bill':'Add Bill';$('bfName').value=b?.name||'';$('bfAmount').value=b?.amount||'';$('bfDue').value=b?.due||date||today();
 $('bfCategory').value=b?.category||'';$('bfNotes').value=b?.notes||'';$('bfRecurring').checked=!!b?.recurring;$('bfRecurrence').value=b?.recurrence||'monthly';$('bfAutopay').checked=!!b?.autopay;$('billDelete').hidden=!b;
 $('billDlg').showModal();
}
function saveBillForm(e){
 e.preventDefault();const data={id:state.editingBill||uid('bill'),name:$('bfName').value.trim(),amount:Number($('bfAmount').value),due:$('bfDue').value,category:$('bfCategory').value.trim(),notes:$('bfNotes').value.trim(),recurring:$('bfRecurring').checked,recurrence:$('bfRecurrence').value,autopay:$('bfAutopay').checked,color:'#12e9f4',paid:false};
 if(!data.name||!data.due||!(data.amount>=0))return alert('Add a name, amount, and due date.');
 const i=state.bills.findIndex(x=>x.id===data.id);if(i>=0)state.bills[i]={...state.bills[i],...data};else state.bills.push(data);persist();$('billDlg').close();renderCurrent();scheduleNotifications();
}
function deleteBill(){if(!state.editingBill)return;if(confirm('Delete this bill?')){state.bills=state.bills.filter(x=>x.id!==state.editingBill);persist();$('billDlg').close();renderCurrent()}}
function openPayment(id,date){const b=state.bills.find(x=>x.id===id);if(!b)return;$('paymentBillId').value=id;$('paymentDate').value=date;$('paymentInfo').textContent=`${b.name} • ${money(b.amount)} • due ${fmt(date)}`;$('paymentAmount').value=(billAmountPaid(b,date)||0).toFixed(2);$('paymentDlg').showModal()}
function savePayment(full=false,unpaid=false){const id=$('paymentBillId').value,date=$('paymentDate').value,b=state.bills.find(x=>x.id===id);if(!b)return;let a=unpaid?0:full?Number(b.amount):Number($('paymentAmount').value);if(!(a>=0))return;state.paid[occurrenceKey(id,date)]=a;if(a>0)state.paymentMeta[occurrenceKey(id,date)]={date:today(),amount:a};persist();$('paymentDlg').close();renderCurrent()}
function addSpend(type){
 const desc=$('spendDesc').value.trim(),amount=Number($('spendAmount').value);if(!desc||!(amount>0))return alert('Add a description and amount.');
 if(state.editingSpend){const x=state.spending.find(v=>v.id===state.editingSpend);if(x){x.description=desc;x.amount=amount;x.type=type||x.type}}else state.spending.push({id:uid('spend'),description:desc,amount,type,timestamp:new Date().toISOString()});
 state.editingSpend=null;$('spendDesc').value='';$('spendAmount').value='';persist();renderSpending();
}
function saveBudget(){const d=new Date(),v=Number($('budgetAmount').value)||0;state.budgets[monthKey(d.getFullYear(),d.getMonth())]=v;localStorage.setItem(K.incomeType,$('incomeType').value);localStorage.setItem(K.wage,String(Number($('hourlyWage').value)||0));localStorage.setItem(K.salary,String(Number($('annualSalary').value)||0));persist();renderBudget();renderHome()}
function toggleIncomeFields(){const t=$('incomeType').value;$('hourlyFields').hidden=t!=='hourly';$('salaryFields').hidden=t!=='salary'}
function addHours(){const date=$('hoursDate').value||today(),hours=Number($('hoursAmount').value);if(!(hours>0))return alert('Enter hours worked.');state.hours.push({id:uid('hours'),date,hours});persist();$('hoursAmount').value='';renderHours()}
function addPaycheck(){const t=$('incomeType').value,date=$('payDate').value||today(),freq=$('payFrequency').value,amount=Number($('payAmount').value)||0,annual=Number($('annualSalary').value)||0;state.paychecks.push({id:uid('pay'),date,frequency:freq,amount,incomeType:t,annualSalary:t==='salary'?annual:0});persist();renderPaychecks();renderHome()}
function scheduleNotifications(){
 if(!(window.AndroidBridge&&AndroidBridge.scheduleNotifications))return;
 try{AndroidBridge.scheduleNotifications(JSON.stringify({enabled:true,reminderEnabled:true,daysBefore:Number(state.settings.notificationDays||3),time:state.settings.notificationTime||'09:00',sound:'default',vibrate:true,bills:state.bills,reminders:state.reminders}))}catch{}
}

function backup(){
 const storage={};for(let i=0;i<localStorage.length;i++){const k=localStorage.key(i);if(k&&k.startsWith('myBills_'))storage[k]=localStorage.getItem(k)}
 const payload=JSON.stringify({format:'MyBillsBackup',version:VERSION,createdAt:new Date().toISOString(),storage},null,2);
 if(window.AndroidBridge&&AndroidBridge.saveBackup)AndroidBridge.saveBackup(payload);else{const a=document.createElement('a');a.href=URL.createObjectURL(new Blob([payload],{type:'application/json'}));a.download='my-bills-backup.json';a.click()}
}
async function restoreFile(file){
 const obj=JSON.parse(await file.text());const storage=obj.storage||obj.masterState?.storage||{};
 if(!storage||typeof storage!=='object')throw new Error('Invalid backup');
 Object.entries(storage).forEach(([k,v])=>{if(k.startsWith('myBills_'))localStorage.setItem(k,typeof v==='string'?v:JSON.stringify(v))});
 location.reload();
}
function clearAll(){if(confirm('Delete all My Bills data on this device?')){Object.keys(localStorage).filter(k=>k.startsWith('myBills_')).forEach(k=>localStorage.removeItem(k));location.reload()}}

function calcPress(v){const d=$('calcDisplay');if(v==='C')d.value='';else if(v==='⌫')d.value=d.value.slice(0,-1);else if(v==='='){try{d.value=String(calc(d.value))}catch{d.value='Error'}}else d.value=(d.value==='Error'?'':d.value)+v}
function calc(s){
 let i=0;s=s.replace(/×/g,'*').replace(/÷/g,'/').replace(/\s/g,'');
 const number=()=>{let a=i;while(/[0-9.]/.test(s[i]||''))i++;if(a===i)throw 0;return Number(s.slice(a,i))};
 const factor=()=>{if(s[i]==='-'){i++;return-factor()}if(s[i]==='('){i++;const v=expr();if(s[i++]!==')')throw 0;return v}return number()};
 const term=()=>{let v=factor();while(s[i]==='*'||s[i]==='/'){const o=s[i++],r=factor();if(o==='/'&&r===0)throw 0;v=o==='*'?v*r:v/r}return v};
 const expr=()=>{let v=term();while(s[i]==='+'||s[i]==='-'){const o=s[i++],r=term();v=o==='+'?v+r:v-r}return v};
 const v=expr();if(i!==s.length||!Number.isFinite(v))throw 0;return Math.round((v+Number.EPSILON)*1e8)/1e8;
}

function setupLoad(){
 let p=load(K.setupProgress,{step:0,values:{}});return p&&typeof p==='object'?p:{step:0,values:{}}
}
function setupSave(){
 const values={};$$('#setupDlg input,#setupDlg select').forEach(el=>values[el.id]=el.type==='checkbox'?el.checked:el.value);
 save(K.setupProgress,{step:Number($('setupStep').value)||0,values});
}
function setupShow(step){
 $('setupStep').value=step;$$('.setup-step').forEach((x,i)=>x.classList.toggle('active',i===step));setupSave();
}
function initSetup(){
 if(localStorage.getItem(K.setupDone)==='true'||localStorage.getItem('myBills_onboarding_v1_done')==='1'||state.bills.length||state.paychecks.length||state.spending.length)return;
 const p=setupLoad();Object.entries(p.values||{}).forEach(([id,v])=>{const el=$(id);if(el){if(el.type==='checkbox')el.checked=!!v;else el.value=v}});
 $('setupPin').checked=false; // opt-in only for a truly new setup
 setupShow(Math.max(0,Math.min(3,Number(p.step)||0)));$('setupDlg').showModal();
}
function finishSetup(){
 localStorage.setItem(K.incomeType,$('setupIncomeType').value);localStorage.setItem(K.wage,String(Number($('setupWage').value)||0));localStorage.setItem(K.salary,String(Number($('setupSalary').value)||0));
 const budget=Number($('setupBudget').value)||0,d=new Date();if(budget>0)state.budgets[monthKey(d.getFullYear(),d.getMonth())]=budget;
 const amount=Number($('setupPayAmount').value)||0,date=$('setupPayDate').value||today(),freq=$('setupPayFreq').value;
 if(amount>0||($('setupIncomeType').value==='salary'&&Number($('setupSalary').value)>0))state.paychecks.push({id:uid('pay'),date,frequency:freq,amount,incomeType:$('setupIncomeType').value,annualSalary:Number($('setupSalary').value)||0});
 persist();try{if($('setupPin').checked&&window.AndroidBridge)AndroidBridge.setAppLockEnabled(true)}catch{}
 localStorage.setItem(K.setupDone,'true');localStorage.setItem('myBills_onboarding_v1_done','1');localStorage.removeItem(K.setupProgress);$('setupDlg').close();renderHome();openBill();
}

function bind(){
 $$('.nav button').forEach(b=>b.addEventListener('click',()=>setView(b.dataset.view)));
 $('addBillHome').onclick=()=>openBill();$('addHoursHome').onclick=()=>{setView('budget');$('hoursAmount').focus()};$('addReminderHome').onclick=()=>setView('settings');
 $('billSearch').oninput=renderBills;$('billCategory').onchange=renderBills;$('newBill').onclick=()=>openBill();
 $('billForm').onsubmit=saveBillForm;$('billDelete').onclick=deleteBill;
 $('billList').onclick=e=>{const p=e.target.closest('[data-pay]'),ed=e.target.closest('[data-edit]');if(p)openPayment(p.dataset.pay,p.dataset.date);if(ed)openBill(ed.dataset.edit)};
 $('payFull').onclick=()=>savePayment(true,false);$('payUnpaid').onclick=()=>savePayment(false,true);$('paySave').onclick=()=>savePayment(false,false);
 $('spendAdd').onclick=()=>addSpend('spending');$('refundAdd').onclick=()=>addSpend('refund');
 $$('.spendRange').forEach(b=>b.onclick=()=>{state.spendRange=b.dataset.range;$$('.spendRange').forEach(x=>x.classList.toggle('primary',x===b));renderSpending()});
 $('spendList').onclick=e=>{const ed=e.target.closest('[data-spend-edit]'),del=e.target.closest('[data-spend-del]');if(ed){const x=state.spending.find(v=>v.id===ed.dataset.spendEdit);if(x){state.editingSpend=x.id;$('spendDesc').value=x.description||x.name||'';$('spendAmount').value=x.amount||''}}if(del&&confirm('Delete this entry?')){state.spending=state.spending.filter(v=>v.id!==del.dataset.spendDel);persist();renderSpending()}};
 $('calPrev').onclick=()=>{state.calendarDate.setMonth(state.calendarDate.getMonth()-1);renderCalendar()};$('calNext').onclick=()=>{state.calendarDate.setMonth(state.calendarDate.getMonth()+1);renderCalendar()};$('calToday').onclick=()=>{state.calendarDate=new Date();renderCalendar()};
 $('calendarGrid').onclick=e=>{const d=e.target.closest('[data-day]');if(d)renderDay(d.dataset.day)};
 $('saveBudget').onclick=saveBudget;$('incomeType').onchange=toggleIncomeFields;$('addHours').onclick=addHours;$('addPaycheck').onclick=addPaycheck;
 $('hoursList').onclick=e=>{const b=e.target.closest('[data-hour-del]');if(b&&confirm('Delete these hours?')){state.hours=state.hours.filter(x=>x.id!==b.dataset.hourDel);persist();renderHours()}};
 $('paycheckList').onclick=e=>{const b=e.target.closest('[data-paycheck-del]');if(b&&confirm('Delete this paycheck schedule?')){state.paychecks=state.paychecks.filter(x=>x.id!==b.dataset.paycheckDel);persist();renderPaychecks();renderHome()}};
 $('pinLock').onchange=()=>{try{AndroidBridge.setAppLockEnabled($('pinLock').checked)}catch{}};
 $('lockNow').onclick=()=>{try{AndroidBridge.lockNow()}catch{}};
 $('backupBtn').onclick=backup;$('restoreInput').onchange=e=>restoreFile(e.target.files[0]).catch(()=>alert('Could not restore that backup.'));$('clearAll').onclick=clearAll;
 $$('.calc-grid button').forEach(b=>b.onclick=()=>calcPress(b.dataset.calc));
 $('setupDlg').addEventListener('cancel',e=>{e.preventDefault();setupSave();$('setupDlg').close()});$$('#setupDlg input,#setupDlg select').forEach(el=>{el.addEventListener('input',setupSave);el.addEventListener('change',setupSave)});
 $$('.setupNext').forEach(b=>b.onclick=()=>setupShow(Number($('setupStep').value)+1));$$('.setupBack').forEach(b=>b.onclick=()=>setupShow(Number($('setupStep').value)-1));$('setupFinish').onclick=finishSetup;
 $('setupIncomeType').onchange=()=>{$('setupHourlyWrap').hidden=$('setupIncomeType').value!=='hourly';$('setupSalaryWrap').hidden=$('setupIncomeType').value!=='salary';setupSave()};
 window.myBillsHandleAndroidBack=()=>{const open=$$('dialog[open]');if(open.length){setupSave();open.at(-1).close();return true}return false};
}
function boot(){
 bind();$('hoursDate').value=today();$('payDate').value=today();$('setupPayDate').value=today();renderHome();renderCalendar();renderBills();renderSpending();renderBudget();renderSettings();initSetup();scheduleNotifications();
}
document.addEventListener('DOMContentLoaded',boot);
})();
