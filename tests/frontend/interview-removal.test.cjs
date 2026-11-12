const {test,before,after}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const root=path.resolve('backend/src/main/webapp');
let browser;
before(async()=>{browser=await chromium.launch({headless:true,...(process.env.CHROMIUM_EXECUTABLE?{executablePath:process.env.CHROMIUM_EXECUTABLE}:{})});});
after(async()=>browser?.close());
const application={id:'legacy_app',applicantId:'student',jobId:'job',name:'Alex Student',applicantName:'Alex Student',role:'BIO 93',module:'Biology',moduleCode:'BIO 93',status:'review',avatar:'A',appliedAt:'2026-10-01',date:'2026-10-01',matchScore:null,periodId:'cycle',periodName:'Fall',qualificationMatching:{status:'needs_confirmation',matched:[],missingRequired:[],unmatchedPreferred:[],score:null},sectionMatching:{status:'pending',sections:[]},documents:[],timeline:[]};
for(const filename of ['organiser/dashboard.jsp','organiser/pipeline.jsp','applicant/dashboard.jsp','applicant/applications.jsp']) {
  test(filename+' renders without Interview UI or requests',async t=>{
    const page=await browser.newPage();t.after(()=>page.close());
    const errors=[],retiredRequests=[];
    page.on('pageerror',e=>errors.push(e.message));page.on('console',m=>{if(m.type()==='error') errors.push(m.text());});
    await page.route('**/*',async route=>{
      const req=route.request(),url=new URL(req.url());
      if(url.pathname==='/tamo/'+filename){
        let body=fs.readFileSync(path.join(root,filename),'utf8').replace(/<%= request.getContextPath\(\) %>/g,'/tamo').replace(/<%=[\s\S]*?%>/g,'null').replace(/<%[\s\S]*?%>/g,'').replace(/<jsp:include[^>]*\/>/g,'');
        const shared=fs.readFileSync(path.join(root,'common/layout-bottom.jsp'),'utf8');
        const helpers=shared.slice(shared.indexOf('  function notificationText('),shared.indexOf('  function sanitizeDocumentHtml('));
        return route.fulfill({contentType:'text/html',body:'<!doctype html><html><head><meta name="ctx" content="/tamo"><link rel="stylesheet" href="/tamo/static/css/custom.css"><style>.hidden{display:none}</style><script>window.lucide={createIcons(){}};'+helpers+'</script><script src="/tamo/static/js/api.js"></script></head><body>'+body+'</body></html>'});
      }
      if(url.pathname.includes('/interviews')) retiredRequests.push(req.url());
      if(url.pathname.startsWith('/tamo/static/')){const file=path.join(root,url.pathname.slice(6));return route.fulfill({contentType:file.endsWith('.css')?'text/css':'text/javascript',body:fs.existsSync(file)?fs.readFileSync(file):''});}
      let json=[];
      if(url.pathname.endsWith('/organiser/dashboard'))json={activePostings:0,currentApplicants:1,totalApplications:1,jobs:[],recentApplications:[application]};
      else if(url.pathname.endsWith('/pipeline'))json={applied:[],review:[application],shortlisted:[],offer:[],rejected:[]};
      else if(url.pathname.endsWith('/applicant/dashboard'))json={activeApplications:1,profileCompletion:50,periodId:'cycle',periods:[{id:'cycle',name:'Fall'}],recentApplications:[application],schedule:[],notifications:[],upcomingDeadlines:[],workload:{totalHours:0,assignments:[]},offers:[]};
      else if(url.pathname.endsWith('/applications'))json=[application];
      return route.fulfill({json});
    });
    await page.goto('http://scope.test/tamo/'+filename);
    const target=filename==='organiser/dashboard.jsp'?'#recentActivity':filename==='organiser/pipeline.jsp'?'#kanban':filename==='applicant/applications.jsp'?'#appList':'#recentApps';
    // All four pages must finish their async application rendering.
    await page.waitForFunction(selector=>{const node=document.querySelector(selector);return node&&node.textContent.includes('BIO 93');},target,{timeout:5000});
    if(filename==='organiser/pipeline.jsp') {
      for (const width of [1152, 390]) {
        await page.setViewportSize({width,height:900});
        const fits = await page.locator('#kanban').evaluate(board => {
          const bounds=board.getBoundingClientRect();
          return [...board.querySelectorAll('.kanban-col')].every(col => {
            const rect=col.getBoundingClientRect();
            return rect.left >= bounds.left && rect.right <= bounds.right + 1;
          });
        });
        assert.ok(fits, 'All stages fit or wrap at '+width+'px');
      }
    }
    assert.doesNotMatch(await page.locator('body').innerText(),/interview/i);
    assert.deepEqual(retiredRequests,[]);assert.deepEqual(errors,[]);
  });
}
