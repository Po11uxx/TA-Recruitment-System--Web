(() => {
  const el = id => document.getElementById(id);
  const student = location.pathname.startsWith(CTX + '/applicant/');
  const role = student ? 'applicant' : location.pathname.startsWith(CTX + '/admin/') ? 'admin' : 'organiser';
  const prefix = '/' + role + '/assignments/';
  const days = ['', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
  const labels = {available:'Available',conflict:'Time conflict',pending:'Needs confirmation'};
  const number = value => new Intl.NumberFormat('en-US',{maximumFractionDigits:2}).format(value);
  let data = {assignments:[],periods:[],modules:[],sections:[],candidates:[]}, cancelling = null, busy = false, pendingAction = null, initialLoad = true;
  const linkedId = new URLSearchParams(location.search).get('assignmentId');
  function resultState(a) { return a.responseStatus==='expired'?'cancelled':a.responseStatus==='declined'?'declined':a.status==='cancelled'?'cancelled':a.publicationStatus!=='published'?'draft':a.responseStatus==='accepted'?'accepted':'pending'; }
  const resultLabels={draft:'Draft · not published',pending:'Awaiting student reply',accepted:'Accepted',declined:'Declined',cancelled:'Cancelled'};
  const localTime = AssignmentTime.format;
  el('assignmentTimeDescription').textContent='Review your TA placement, section meetings and weekly hours here. Dates use your device time zone: '+AssignmentTime.timeZone+'. Weekly meeting times use their course time zone. Totals and capacity are counted separately for each application period.';
  function text(parent, value, className = '') { const p=document.createElement('p'); p.className=className; p.textContent=value; parent.appendChild(p); return p; }
  function message(value, error=false) { el('assignmentMessage').textContent=value; el('assignmentMessage').className='text-sm '+(error?'text-red-700':'text-emerald-700'); }
  function options(select, items, placeholder) {
    const previous=select.value; select.replaceChildren(new Option(placeholder,''));
    items.forEach(([id,name])=>select.add(new Option(name,id)));
    if(items.some(([id])=>id===previous)) select.value=previous;
  }
  function period() { return el('assignmentPeriod').value; }
  function course() { return el('assignmentCourse').value; }
  function canManage(courseId) { return !student && (data.manageableCourseIds||[]).includes(courseId); }
  function meetings(s) { return (s.meetingTimes || []).map(m=>days[m.dayOfWeek]+' '+m.startTime+'–'+m.endTime).join(' · ')+(s.timeZone?' · '+s.timeZone:' · Course time zone not recorded'); }
  function candidates() { return (data.candidates||[]).filter(a=>a.periodId===period()&&a.moduleId===course()); }
  function sections() { return (data.sections||[]).filter(s=>s.moduleId===course()&&s.status==='active'&&(s.periodId===period()||(!s.periodId&&period()==='period_legacy'))); }
  function occupied(id) { return data.assignments.filter(a=>a.periodId===period()&&a.sectionId===id&&a.status==='active'&&a.responseStatus!=='expired').length; }
  function candidate() { return candidates().find(a=>a.applicationId===el('assignmentCandidate').value); }
  function selectedSection() { return sections().find(s=>s.id===el('assignmentSection').value); }
  function matching(c,s) { return c&&s&&((c.matching||{}).sections||[]).find(m=>m.sectionId===s.id); }
  function updatePreview() {
    const c=candidate(),s=selectedSection(),match=matching(c,s);
    const state=match?match.status:'pending';
    const alreadyAssigned=!!(c&&s&&data.assignments.some(a=>a.status==='active'&&a.responseStatus!=='expired'&&a.periodId===period()&&a.applicantId===c.applicantId&&a.sectionId===s.id));
    el('manualConfirmation').classList.toggle('hidden',!c||!s||state!=='pending');
    el('confirmPending').checked=false; el('confirmationNote').value='';
    el('assignmentPreview').textContent=c&&s ? (alreadyAssigned?'Already assigned':labels[state])+' · '+meetings(s)+'\n'+number(occupied(s.id))+' / '+number(s.taPositions)+' assigned. '+number(s.hoursPerWeek)+' hours per week.\nApplicant total: '+number(c.weeklyHoursTotal)+' → '+number(c.weeklyHoursTotal+(alreadyAssigned?0:s.hoursPerWeek))+' hours per week in this period. Other assignments are checked again on submit.' : 'Choose a period, course, applicant and section.';
    el('assignTaButton').disabled=busy||!c||!s||alreadyAssigned||state==='conflict'||occupied(s.id)>=s.taPositions;
  }
  function choose() {
    options(el('assignmentCandidate'),candidates().map(c=>[c.applicationId,(c.name||c.applicantId)+' · '+c.applicationId]),'Choose offered applicant');
    options(el('assignmentSection'),sections().map(s=>[s.id,s.name+' · '+occupied(s.id)+' / '+s.taPositions+' assigned']),'Choose section');
    el('assignmentCapacity').replaceChildren();
    if(!period()||!course()) text(el('assignmentCapacity'),'Choose a period and course.');
    else if(!sections().length) text(el('assignmentCapacity'),'No active sections for this course.');
    else sections().forEach(s=>text(el('assignmentCapacity'),s.name+': '+occupied(s.id)+' / '+s.taPositions+' reserved · '+data.assignments.filter(a=>a.periodId===period()&&a.sectionId===s.id&&a.status==='active'&&a.responseStatus!=='expired'&&a.responseStatus==='accepted').length+' accepted · '+number(s.hoursPerWeek)+' hours per TA per week'));
    updatePreview(); render();
  }
  function openAction(a,action) {
    pendingAction={id:a.id,action};
    el('assignmentActionTitle').textContent={publish:'Publish assignment',accept:'Accept assignment',decline:'Decline assignment'}[action];
    el('assignmentActionDetails').textContent=a.moduleCode+' · '+a.sectionSnapshot.name+'\n'+meetings(a.sectionSnapshot)+'\n'+number(a.sectionSnapshot.hoursPerWeek)+' hours / week · '+(a.sectionSnapshot.location||'Location not specified')+'\n'+(action==='publish'?'Publishing makes this assignment visible to the student and sends an in-app notification.':action==='accept'?'Confirm that you accept these meetings and weekly hours.':'Declining releases this position. Your reply and reason will be retained.');
    el('assignmentDeclineReasonLabel').classList.toggle('hidden',action!=='decline');el('assignmentDeclineReason').required=action==='decline';
    const adminPublish=role==='admin'&&action==='publish';
    el('assignmentPublishReasonLabel').classList.toggle('hidden',!adminPublish);
    el('assignmentPublishReason').required=adminPublish;el('assignmentPublishReason').value='';
    el('assignmentDeclineReason').value='';el('assignmentActionError').textContent='';el('assignmentActionDialog').showModal();
  }
  function render() {
    el('assignmentDraftPanel').classList.toggle('hidden',!canManage(course()));
    el('assignmentAccess').classList.toggle('hidden',role!=='admin'||!course()||canManage(course()));
    const rows=data.assignments.filter(a=>a.periodId===period()&&(student||!course()||a.sectionSnapshot.moduleId===course()));
    const workload=el('assignmentWorkload');workload.replaceChildren();
    const totals=new Map();
    rows.filter(a=>a.status==='active'&&a.responseStatus!=='expired').forEach(a=>totals.set(a.applicantId,{name:a.applicantName||a.applicantId,hours:a.weeklyHoursTotal,accepted:a.acceptedWeeklyHoursTotal||0}));
    if(!student) candidates().forEach(c=>totals.set(c.applicantId,{name:c.name||c.applicantId,hours:c.weeklyHoursTotal,accepted:c.acceptedWeeklyHoursTotal||0}));
    if(!totals.size) text(workload,period()?'No active assignments in this view.':'Choose an application period.','text-sm text-zinc-500');
    totals.forEach(v=>text(workload,v.name+': '+number(v.hours)+(student?' published hours / week':' reserved hours / week')+' · '+number(v.accepted)+' accepted hours / week','text-sm font-medium'));
    const container=el('assignmentRows');container.replaceChildren();
    const counts={draft:0,pending:0,accepted:0,declined:0,cancelled:0};rows.forEach(a=>counts[resultState(a)]++);
    el('assignmentResultSummary').textContent=Object.keys(counts).filter(k=>!student||k!=='draft').map(k=>resultLabels[k]+': '+counts[k]).join(' · ');
    const filter=el('assignmentResultFilter').value;
    const shown=rows.filter(a=>(resultState(a)!=='cancelled'||el('assignmentHistory').checked||filter==='cancelled')&&(!filter||resultState(a)===filter));
    if(!shown.length) text(container,period()?'No assignments in this view.':'Choose an application period.','text-zinc-500');
    shown.forEach(a=>{
      const card=document.createElement('article');card.className='card p-5 space-y-2';card.id='assignment-'+a.id;
      if(a.id===linkedId) card.className+=' ring-2 ring-blue-400';
      const heading=document.createElement('h3');heading.className='font-bold';heading.textContent=(student?'':(a.applicantName||a.applicantId)+' · ')+a.moduleCode+' · '+a.sectionSnapshot.name;card.appendChild(heading);
      text(card,resultLabels[resultState(a)],a.status==='active'&&a.responseStatus!=='expired'?'text-emerald-700 text-sm font-medium':'text-zinc-500 text-sm');
      text(card,meetings(a.sectionSnapshot),'text-sm');
      text(card,number(a.sectionSnapshot.hoursPerWeek)+' hours per week · '+(a.sectionSnapshot.location||'Location not specified'),'text-sm');
      text(card,'Prepared: '+localTime(a.assignedAt),'text-xs text-zinc-500');
      if(a.publishedAt) text(card,'Published: '+localTime(a.publishedAt),'text-xs text-zinc-500');
      if(a.respondedAt&&a.responseStatus!=='expired') text(card,'Student reply: '+a.responseStatus+' · '+localTime(a.respondedAt)+(a.responseReason?' · '+a.responseReason:''),'text-sm');
      function actionButton(label,action) {
        const button=document.createElement('button');button.type='button';button.className='btn btn-outline mr-2';button.textContent=label;button.disabled=busy;button.dataset.assignmentAction='true';if(action==='accept'||action==='decline')button.dataset.replyAssignment=a.id;
        button.addEventListener('click',()=>{if(!busy)openAction(a,action);});card.appendChild(button);
      }
      if(canManage(a.sectionSnapshot.moduleId)&&resultState(a)==='draft') actionButton('Publish result','publish');
      if(student&&resultState(a)==='pending') {actionButton('Accept assignment','accept');actionButton('Decline assignment','decline');}
      if(!student&&a.manualConfirmationNote) text(card,'Manual confirmation: '+a.manualConfirmationNote,'text-sm');
      if(resultState(a)==='cancelled') text(card,a.responseStatus==='expired'?'Historical cancellation':('Cancelled: '+localTime(a.cancelledAt)+' · '+a.cancellationReason),'text-sm');
      if(canManage(a.sectionSnapshot.moduleId)&&a.status==='active'&&a.responseStatus!=='expired'&&resultState(a)!=='cancelled') {
        const button=document.createElement('button');button.type='button';button.dataset.cancelAssignment='true';button.className='btn btn-outline';button.textContent='Cancel assignment';button.disabled=busy;
        button.addEventListener('click',()=>{
          cancelling=a.id;el('cancelAssignmentName').textContent=(a.applicantName||a.applicantId)+' · '+a.sectionSnapshot.name;
          el('cancelAssignmentReason').value='';el('cancelAssignmentError').textContent='';el('cancelAssignmentDialog').showModal();
        });card.appendChild(button);
      }
      container.appendChild(card);
    });
  }
  function setBusy(value) {
    busy=value;el('assignTaFields').disabled=value;
    document.querySelectorAll('[data-cancel-assignment], [data-assignment-action]').forEach(button=>button.disabled=value);
    for(const id of ['assignmentPeriod','assignmentCourse','refreshAssignments','submitCancelAssignment','closeCancelAssignment','submitAssignmentAction','closeAssignmentAction','assignmentResultFilter']) el(id).disabled=value;
  }
  async function load() {
    setBusy(true);
    try {
      data=await request(prefix);
      options(el('assignmentPeriod'),data.periods.map(p=>[p.id,p.name]),'Choose application period');
      if(!period()&&data.periods.length) el('assignmentPeriod').value=data.periods[0].id;
      if(!student) {
        options(el('assignmentCourse'),data.modules.map(m=>[m.id,m.code+' · '+m.name]),'Choose course');
        if(!course()&&data.modules.length) el('assignmentCourse').value=data.modules[0].id;
      }
      if(initialLoad&&linkedId) {
        const linked=data.assignments.find(a=>a.id===linkedId);
        if(linked) {el('assignmentPeriod').value=linked.periodId;if(!student)el('assignmentCourse').value=linked.sectionSnapshot.moduleId;if(resultState(linked)==='cancelled')el('assignmentHistory').checked=true;}
      }
      choose();
      if(initialLoad&&linkedId) document.getElementById('assignment-'+linkedId)?.scrollIntoView({block:'center'});
      initialLoad=false;
    } catch(e) { message(e.message||'Could not load assignments.',true); }
    finally { setBusy(false);updatePreview(); }
  }
  el('assignmentPeriod').addEventListener('change',choose);el('assignmentCourse').addEventListener('change',choose);
  el('assignmentResultFilter').addEventListener('change',render);
  el('assignmentHistory').addEventListener('change',render);el('assignmentCandidate').addEventListener('change',updatePreview);el('assignmentSection').addEventListener('change',updatePreview);
  el('refreshAssignments').addEventListener('click',load);
  el('assignTaForm').addEventListener('submit',async event=>{
    event.preventDefault();if(busy||!canManage(course()))return;
    const c=candidate(),s=selectedSection();if(!c||!s)return;
    const payload={applicationId:c.applicationId,sectionId:s.id,confirmPending:el('confirmPending').checked,manualConfirmationNote:el('confirmationNote').value.trim()};
    if(role==='admin') payload.reason=el('assignmentDraftReason').value.trim();
    if((matching(c,s)||{}).status!=='available'&&(!payload.confirmPending||payload.manualConfirmationNote.length<10)) {message('Confirm availability manually and enter a note of at least 10 characters.',true);return;}
    setBusy(true);
    try {await request(prefix,{method:'POST',body:JSON.stringify(payload)});el('assignmentDraftReason').value='';message('Assignment draft saved. Publish it when ready for the student to reply.');await load();}
    catch(e) {message(e.message||'Assignment failed.',true);}
    finally {setBusy(false);updatePreview();}
  });
  el('closeCancelAssignment').addEventListener('click',()=>el('cancelAssignmentDialog').close());
  el('cancelAssignmentForm').addEventListener('submit',async event=>{
    event.preventDefault();if(busy||!cancelling)return;
    const reason=el('cancelAssignmentReason').value.trim();if(!reason) {el('cancelAssignmentError').textContent='Enter a cancellation reason.';return;}
    setBusy(true);
    try {await request(prefix+cancelling+'/cancel',{method:'POST',body:JSON.stringify({reason})});el('cancelAssignmentDialog').close();message('Assignment cancelled. Capacity released; history retained.');await load();}
    catch(e) {el('cancelAssignmentError').textContent=e.message||'Cancellation failed.';}
    finally {setBusy(false);updatePreview();}
  });
  el('closeAssignmentAction').addEventListener('click',()=>el('assignmentActionDialog').close());
  el('assignmentActionForm').addEventListener('submit',async event=>{
    event.preventDefault();if(busy||!pendingAction)return;
    const {id,action}=pendingAction;const reason=el('assignmentDeclineReason').value.trim();
    if(action==='decline'&&!reason) {el('assignmentActionError').textContent='Please enter a reason for declining.';return;}
    setBusy(true);
    try {
      const payload=action==='publish'?(role==='admin'?{reason:el('assignmentPublishReason').value.trim()}:{}):{decision:action,reason};
      await request(prefix+id+(action==='publish'?'/publish':'/respond'),{method:'POST',body:JSON.stringify(payload)});
      el('assignmentActionDialog').close();message(action==='publish'?'Result published. Student notified.':action==='accept'?'Assignment accepted.':'Assignment declined. Position released.');
      await load();if(typeof loadHeaderNotifications==='function')loadHeaderNotifications();
    } catch(e) {el('assignmentActionError').textContent=e.message||'Action failed. Refresh to check the current result.';}
    finally {setBusy(false);updatePreview();}
  });
  if(student) {el('assignmentsTitle').textContent='My TA Placement';el('assignmentWorkloadDescription').textContent='Published hours include arrangements awaiting your reply. Accepted hours count only your accepted, active arrangements in this period.';el('courseFilterLabel').classList.add('hidden');el('assignmentResultFilter').querySelector('option[value="draft"]').remove();}
  else el('assignmentStaff').classList.remove('hidden');
  el('assignmentDraftReasonLabel').classList.toggle('hidden',role!=='admin');
  el('assignmentDraftReason').required=role==='admin';
  load();
  setInterval(()=>{
    if(document.hidden||busy||el('assignmentActionDialog').open||el('cancelAssignmentDialog').open) return;
    if(!student&&(el('assignmentCandidate').value||el('assignmentSection').value||el('confirmationNote').value||el('assignmentDraftReason').value)) return;
    load();if(typeof loadHeaderNotifications==='function')loadHeaderNotifications();
  },15000);
})();
