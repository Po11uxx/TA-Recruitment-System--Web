const {test,before,after}=require('node:test');const assert=require('node:assert/strict');const fs=require('node:fs');const path=require('node:path');const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright');
let browser;const root=path.resolve('backend/src/main/webapp');
before(async()=>{browser=await chromium.launch({headless:true,...(process.env.CHROMIUM_EXECUTABLE?{executablePath:process.env.CHROMIUM_EXECUTABLE}:{})});});after(async()=>browser?.close());
for(const [zone,expected] of [['America/Los_Angeles','Nov 10, 2026, 15:30'],['Asia/Tokyo','Nov 11, 2026, 08:30']])test('assignment page uses '+zone+' for instants and keeps native selects usable',async t=>{
 const context=await browser.newContext({timezoneId:zone});t.after(()=>context.close());const page=await context.newPage();const errors=[];page.on('pageerror',e=>errors.push(e.message));
 const body=fs.readFileSync(path.join(root,'common/assignments-content.jsp'),'utf8').replace(/<%[\s\S]*?%>/g,'');
 await page.route('**/*',r=>r.fulfill({contentType:'text/html',body:'<!doctype html><html><body>'+body+'</body></html>'}));await page.goto('http://fixture.test/applicant/assignments.jsp');
 await page.addStyleTag({path:path.join(root,'static/css/custom.css')});await page.addStyleTag({content:'.hidden{display:none}'});
 const data={serverNow:'2026-11-10T00:00:00Z',periods:[{id:'fall',name:'Fall'}],modules:[],sections:[],candidates:[],assignments:[{id:'a1',periodId:'fall',applicantId:'s1',applicantName:'Student',moduleCode:'BIO 93',sectionSnapshot:{name:'Lab A',timeZone:'America/Los_Angeles',meetingTimes:[{dayOfWeek:1,startTime:'10:00',endTime:'12:00'}],hoursPerWeek:8},status:'active',publicationStatus:'published',responseStatus:'pending',assignedAt:'2026-11-10T23:30:00Z',publishedAt:'2026-11-10T23:30:00Z',responseDeadline:'2026-11-10T23:30:00Z',weeklyHoursTotal:8,acceptedWeeklyHoursTotal:0}]};
 await page.addScriptTag({content:'const CTX=""; window.request=async()=>('+JSON.stringify(data)+');'});
 for(const file of ['static/js/assignment-time.js','static/js/assignments.js'])await page.addScriptTag({path:path.join(root,file)});
 await page.waitForSelector('#assignment-a1');assert.match(await page.locator('#assignmentTimeDescription').innerText(),new RegExp(zone));const content=await page.locator('#assignment-a1').innerText();assert.ok(content.includes('Prepared: '+expected),content);assert.doesNotMatch(await page.locator('body').innerText(),/deadline|remaining|expired/i);assert.equal(await page.locator('#assignmentReplyDeadline').count(),0);await page.getByRole('button',{name:'Accept assignment',exact:true}).click();assert.equal(await page.locator('#submitAssignmentAction').isEnabled(),true);await page.locator('#closeAssignmentAction').click();assert.equal(await page.getByRole('button',{name:'Decline assignment',exact:true}).isEnabled(),true);assert.match(content,/Monday 10:00–12:00 · America\/Los_Angeles/);
 await page.waitForFunction(()=>parseFloat(getComputedStyle(document.getElementById('assignmentPeriod')).paddingRight)>=43.99);
 const style=await page.locator('#assignmentPeriod').evaluate(n=>{const s=getComputedStyle(n);return {appearance:s.appearance,paddingRight:s.paddingRight,image:s.backgroundImage,position:s.backgroundPosition};});assert.equal(style.appearance,'none');assert.equal(style.paddingRight,'44px');assert.match(style.image,/svg/);assert.match(style.position,/50%/);
 await page.locator('#assignmentResultFilter').selectOption('accepted');await page.waitForSelector('#assignment-a1',{state:'detached'});await page.locator('#assignmentResultFilter').selectOption('');await page.waitForSelector('#assignment-a1');assert.deepEqual(errors,[]);
});
async function actionFixture(t,role,responseStatus='pending',publicationStatus='published',manageableCourseIds=role==='organiser'?['bio']:[]) {
 const context=await browser.newContext();t.after(()=>context.close());const page=await context.newPage();
 const body=fs.readFileSync(path.join(root,'common/assignments-content.jsp'),'utf8').replace(/<%[\s\S]*?%>/g,'');
 await page.route('**/*',r=>r.fulfill({contentType:'text/html',body:'<!doctype html><body>'+body+'</body>'}));await page.goto('http://fixture.test/'+role+'/assignments.jsp');await page.addStyleTag({content:'.hidden{display:none}'});
 const data={periods:[{id:'fall',name:'Fall'}],modules:[{id:'bio',code:'BIO 93',name:'Biology'}],manageableCourseIds,sections:[],candidates:[],assignments:[{id:'a1',periodId:'fall',applicantId:'s1',applicantName:'Student',moduleCode:'BIO 93',sectionSnapshot:{moduleId:'bio',name:'Lab A',hoursPerWeek:8,meetingTimes:[]},status:'active',publicationStatus,responseStatus,assignedAt:'2026-11-10T23:30:00Z',responseDeadline:'2000-01-01T00:00:00Z',weeklyHoursTotal:8}]};
 await page.addScriptTag({content:'const CTX="";const fixture='+JSON.stringify(data)+';window.sent=[];window.request=async(url,options)=>{if(options)window.sent.push({url,...options});return fixture;};'});
 for(const file of ['static/js/assignment-time.js','static/js/assignments.js'])await page.addScriptTag({path:path.join(root,file)});
 await page.waitForFunction(()=>document.getElementById('assignmentResultSummary').textContent.length>0);return page;
}
for(const decision of ['accept','decline'])test('student can '+decision+' despite legacy passed reply date',async t=>{
 const page=await actionFixture(t,'applicant');await page.getByRole('button',{name:decision==='accept'?'Accept assignment':'Decline assignment',exact:true}).click();
 if(decision==='decline')await page.locator('#assignmentDeclineReason').fill('Schedule changed');
 await page.locator('#submitAssignmentAction').click();await page.waitForFunction(()=>window.sent.length===1);
 const sent=await page.evaluate(()=>window.sent[0]);assert.equal(sent.url,'/applicant/assignments/a1/respond');assert.deepEqual(JSON.parse(sent.body),{decision,reason:decision==='decline'?'Schedule changed':''});
});
test('staff publish uses an empty payload without a date picker',async t=>{
 const page=await actionFixture(t,'organiser','pending','draft');await page.getByRole('button',{name:'Publish result',exact:true}).click();assert.equal(await page.locator('#assignmentActionDialog input').count(),0);assert.doesNotMatch(await page.locator('#assignmentActionDialog').innerText(),/deadline|expire/i);await page.locator('#submitAssignmentAction').click();await page.waitForFunction(()=>window.sent.length===1);const sent=await page.evaluate(()=>window.sent[0]);assert.equal(sent.url,'/organiser/assignments/a1/publish');assert.deepEqual(JSON.parse(sent.body),{});
});
test('legacy expired placement is cancelled history and cannot be accepted',async t=>{
 const page=await actionFixture(t,'applicant','expired');assert.equal(await page.locator('#assignment-a1').count(),0);assert.equal(await page.locator('option[value="expired"]').count(),0);await page.locator('#assignmentHistory').check();assert.match(await page.locator('#assignment-a1').innerText(),/Cancelled[\s\S]*Historical cancellation/);assert.equal(await page.getByRole('button',{name:'Accept assignment',exact:true}).count(),0);
});

test('admin sees a draft without editing controls by default',async t=>{
 const page=await actionFixture(t,'admin','pending','draft');
 assert.equal(await page.locator('#assignment-a1').count(),1);
 assert.equal(await page.locator('#assignmentAccess').isVisible(),true);
 assert.equal(await page.locator('#assignmentDraftPanel').isVisible(),false);
 assert.equal(await page.getByRole('button',{name:'Publish result',exact:true}).count(),0);
 assert.equal(await page.getByRole('button',{name:'Cancel assignment',exact:true}).count(),0);
});
test('delegated admin can publish with a reason',async t=>{
 const page=await actionFixture(t,'admin','pending','draft',['bio']);
 await page.getByRole('button',{name:'Publish result',exact:true}).click();
 await page.locator('#assignmentPublishReason').fill('Covering for the Teacher');
 await page.locator('#submitAssignmentAction').click();
 await page.waitForFunction(()=>window.sent.length===1);
 const sent=await page.evaluate(()=>window.sent[0]);
 assert.deepEqual(JSON.parse(sent.body),{reason:'Covering for the Teacher'});
});
