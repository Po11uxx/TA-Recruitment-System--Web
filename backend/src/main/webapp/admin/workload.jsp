<%@ page pageEncoding="UTF-8" %>
<%@ include file="/common/auth-check.jsp" %>
<jsp:include page="/common/layout-top.jsp" />
<div class="space-y-5 max-w-6xl mx-auto"><h1 class="text-3xl font-bold">Workload Intel</h1><p>Accepted hours and reserved hours use formal Section assignments, grouped by application period.</p>
<label>Application period<select id="workloadPeriod" class="input w-auto"></select></label><label>Search applicant<input id="workloadSearch" class="input"></label><a class="btn btn-outline" href="assignments.jsp">View assignments</a><div id="workloadRows" class="space-y-3" role="status"></div></div>
<jsp:include page="/common/layout-bottom.jsp" />
<script>
(async()=>{
 const period=document.getElementById('workloadPeriod'),search=document.getElementById('workloadSearch'),rows=document.getElementById('workloadRows');let sequence=0;
 async function load(){const n=++sequence;try{const result=await request('/admin/workload?'+new URLSearchParams({periodId:period.value,keyword:search.value}));if(n!==sequence)return;rows.replaceChildren();result.forEach(r=>{const c=document.createElement('article');c.className='card p-5';c.textContent=r.taName+' · '+r.totalHours+' accepted / '+r.maxHours+' limit hours per week · '+r.reservedHours+' reserved · '+r.status;rows.appendChild(c);});if(!result.length)rows.textContent='No applicants in this view.';}catch(e){rows.textContent=e.message;}}
 (await adminApi.getApplicationPeriods()).forEach(p=>period.add(new Option(p.name,p.id)));if(period.options.length)period.selectedIndex=period.options.length-1;period.onchange=load;search.oninput=load;load();
})();
</script>
