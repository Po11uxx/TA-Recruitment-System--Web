const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const root='backend/src/main/webapp/';
function page(filename,fetch){
 const elements={};for(const id of ['email','password','name','errorMsg','submitBtn'])elements[id]={value:id==='email'?'student@uci.edu':id==='name'?'Student':'password',innerHTML:'Submit',disabled:false,classList:{add(){},remove(){}}};
 const window={location:{},addEventListener(){}};
 const context={window,URLSearchParams,document:{getElementById:id=>elements[id],querySelector:()=>({content:'/tamo'}),querySelectorAll:()=>[]},fetch,lucide:{createIcons(){}},setTimeout:fn=>fn()};
 vm.createContext(context);vm.runInContext(fs.readFileSync(root+'static/js/api.js','utf8'),context);
 const jsp=fs.readFileSync(root+filename,'utf8').replace(/<%[\s\S]*?%>/g,'');
 for(const match of jsp.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script>/g))if(match[1].trim())vm.runInContext(match[1],context);
 return {context,window,elements,jsp};
}
test('login has no public role selectors or client role state',()=>{
 const html=fs.readFileSync(root+'auth/login.jsp','utf8');
 assert.doesNotMatch(html,/Select Role|Advanced review role|selectedRole|selectRole\(/);
 assert.doesNotMatch(html,/name="role"|id="role-/);
});
for(const role of ['applicant','organiser','admin','reviewer','decision_maker','viewer'])test('login routes using stored '+role+' account without sending role',async()=>{
 let payload;const ui=page('auth/login.jsp',async(_url,options)=>{payload=JSON.parse(options.body);return {ok:true,json:async()=>({user:{role}})};});
 await ui.context.handleLogin({preventDefault(){}});
 assert.deepEqual(payload,{email:'student@uci.edu',password:'password'});
 assert.equal(ui.window.location.href,'/tamo/'+(['organiser','reviewer','decision_maker','viewer'].includes(role)?'review':role)+'/dashboard.jsp');
});
test('signup has no staff entry or role state and always creates an applicant',async()=>{
 let payload;const ui=page('auth/signup.jsp',async(_url,options)=>{payload=JSON.parse(options.body);return {ok:true,json:async()=>({user:{role:'applicant'}})};});
 assert.doesNotMatch(ui.jsp,/selectedRole|Teacher|admin|name="role"|id="role-/);
 await ui.context.handleSignup({preventDefault(){}});assert.equal(payload.role,'applicant');assert.equal(ui.window.location.href,'/tamo/applicant/dashboard.jsp');
});
test('invalid or absent server role never falls back to an applicant dashboard',async()=>{
 const ui=page('auth/login.jsp',async()=>({ok:true,json:async()=>({user:{role:'invented'}})}));
 await ui.context.handleLogin({preventDefault(){}});assert.equal(ui.window.location.href,undefined);assert.match(ui.elements.errorMsg.textContent,/role|account/i);
});

test('Applicant login resumes a placement link from an offer email',async()=>{
 const ui=page('auth/login.jsp',async()=>({ok:true,json:async()=>({user:{role:'applicant'}})}));
 ui.window.location.search='?placementId=assignment_email';
 await ui.context.handleLogin({preventDefault(){}});
 assert.equal(ui.window.location.href,'/tamo/applicant/assignments.jsp?assignmentId=assignment_email');
});
