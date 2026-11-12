const {test,before,after}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const root=path.resolve('backend/src/main/webapp');
let browser;
before(async()=>{browser=await chromium.launch({headless:true,...(process.env.CHROMIUM_EXECUTABLE?{executablePath:process.env.CHROMIUM_EXECUTABLE}:{})});});
after(async()=>browser?.close());
async function open(t,schedule=null,editable=true,viewport={width:1000,height:900}){
 const page=await browser.newPage({viewport});t.after(()=>page.close());
 const html=fs.readFileSync(path.join(root,'applicant/availability.jsp'),'utf8').replace(/<%[\s\S]*?%>/g,'').replace(/<jsp:include[^>]*\/>/g,'').replace(/<script[^>]*><\/script>/g,'');
 await page.setContent(html);
 await page.addStyleTag({path:path.join(root,'static/css/custom.css')});
 await page.addStyleTag({content:'.hidden{display:none}'});
 await page.evaluate(({schedule,editable})=>{
   window.saves=[];
   window.applicantApi={
     getAvailabilityPeriods:async()=>[{id:'fall',name:'Fall',editable,effectiveStatus:editable?'open':'closed'}],
     getAvailability:async()=>({availability:schedule,editable}),
     saveAvailability:async(_,payload)=>{
       saves.push(payload);
       const availableSlots=[];
       for(let day=1;day<=5;day++){
         let cursor='08:00';
         for(const slot of payload.conflictSlots.filter(s=>s.dayOfWeek===day)){
           if(slot.endTime<='08:00'||slot.startTime>='20:00')continue;
           if(cursor<slot.startTime) availableSlots.push({dayOfWeek:day,startTime:cursor,endTime:slot.startTime});
           cursor=slot.endTime<'20:00'?slot.endTime:'20:00';
         }
         if(cursor<'20:00')availableSlots.push({dayOfWeek:day,startTime:cursor,endTime:'20:00'});
       }
       return {...payload,availableSlots,noAvailability:!availableSlots.length};
     }
   };
 },{schedule,editable});
 for(const file of ['availability-view.js','availability.js'])await page.addScriptTag({path:path.join(root,'static/js',file)});
 await page.waitForSelector('#availabilityForm',{state:'visible'});
 return page;
}
test('click and keyboard select exact endpoints; save requires confirmation',async t=>{
 const page=await open(t);
 const start=page.getByRole('button',{name:'Monday 09:00',exact:true});
 const end=page.getByRole('button',{name:'Monday 09:50',exact:true});
 await start.click();
 assert.equal(await start.getAttribute('aria-pressed'),'true');
 await page.getByRole('button',{name:'Monday 09:40',exact:true}).focus();
 await page.keyboard.press('ArrowDown');await page.keyboard.press('Space');
 assert.equal(await end.getAttribute('aria-pressed'),'false');
 assert.equal(await page.locator('.availability-block').innerText(),'09:00–09:50');
 await page.getByRole('button',{name:'Save availability'}).click();
 assert.deepEqual(await page.evaluate(()=>saves),[]);
 await page.locator('#confirmUnmarkedAvailable').check();
 await page.getByRole('button',{name:'Save availability'}).click();
 await page.waitForFunction(()=>saves.length===1);
 assert.deepEqual((await page.evaluate(()=>saves[0])).conflictSlots,[{dayOfWeek:1,startTime:'09:00',endTime:'09:50'}]);
 assert.equal(await page.locator('.availability-block').count(),1);
});
test('existing partial busy boundaries survive save and the week scrolls on mobile',async t=>{
 const page=await open(t,{conflictsOnly:true,conflictSlots:[{dayOfWeek:1,startTime:'09:15',endTime:'09:45'}]},true,{width:390,height:844});
 assert.match(await page.locator('#availabilityStatus').innerText(),/Imported conflicts/);
 assert.equal(await page.locator('.availability-block').innerText(),'09:15–09:45');
 assert.ok(await page.locator('#availabilityGridScroll').evaluate(e=>e.scrollWidth>e.clientWidth));
 await page.locator('#confirmUnmarkedAvailable').check();
 await page.getByRole('button',{name:'Save availability'}).click();
 await page.waitForFunction(()=>saves.length===1);
 assert.deepEqual((await page.evaluate(()=>saves[0])).conflictSlots,[{dayOfWeek:1,startTime:'09:15',endTime:'09:45'}]);
});

test('calendar shows only 08:00–20:00; clearing visible time retains saved outside conflicts',async t=>{
 const outside={dayOfWeek:1,startTime:'07:00',endTime:'07:30'};
 const weekend={dayOfWeek:6,startTime:'09:00',endTime:'09:30'};
 const page=await open(t,{conflictsOnly:true,conflictSlots:[outside,weekend,{dayOfWeek:1,startTime:'08:00',endTime:'08:20'}]});
 const rows=page.locator('#availabilityGrid tbody th');
 assert.equal(await rows.count(),73);
 assert.equal(await rows.first().innerText(),'08:00');
 assert.equal(await rows.last().innerText(),'20:00');
 assert.equal(await page.getByRole('columnheader',{name:'Saturday'}).count(),0);
 await page.getByRole('button',{name:'Clear visible busy times'}).click();
 await page.locator('#confirmUnmarkedAvailable').check();
 await page.getByRole('button',{name:'Save availability'}).click();
 await page.waitForFunction(()=>saves.length===1);
 assert.deepEqual((await page.evaluate(()=>saves[0])).conflictSlots,[outside,weekend]);
});
