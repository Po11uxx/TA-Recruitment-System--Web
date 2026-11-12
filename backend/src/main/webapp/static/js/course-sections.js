(() => {
  const el = id => document.getElementById(id);
  const prefix = location.pathname.startsWith(CTX + '/admin/') ? '/admin/course-sections' : '/organiser/course-sections';
  const days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
  const number = value => new Intl.NumberFormat('en-US', {maximumFractionDigits:2}).format(value);
  let sections = [], editing = null, sequence = 0, copying = false, canConfigure = false;
  function message(text, error = false) { el('sectionMessage').textContent = text; el('sectionMessage').className = 'text-sm ' + (error ? 'text-red-700' : 'text-emerald-700'); }
  function text(parent, value, className = '') { const node = document.createElement('p'); node.className = className; node.textContent = value; parent.appendChild(node); }
  function addMeeting(value = {}) {
    const row = document.createElement('div'); row.className = 'grid grid-cols-1 sm:grid-cols-4 gap-3 border border-zinc-200 rounded-lg p-3'; row.setAttribute('role', 'group'); row.setAttribute('aria-label', 'Weekly meeting');
    function field(label, input) { const wrapper = document.createElement('label'); wrapper.className = 'text-sm font-medium'; wrapper.append(document.createTextNode(label), input); row.appendChild(wrapper); }
    const day = document.createElement('select'); day.className = 'input'; day.dataset.field = 'dayOfWeek'; days.forEach((name, i) => day.add(new Option(name, i + 1))); day.value = value.dayOfWeek || 1; field('Day', day);
    for (const [name, label, placeholder] of [['startTime', 'Start time (24h)', '14:00'], ['endTime', 'End time (24h)', '17:00']]) {
      const input = document.createElement('input'); input.type = 'text'; input.className = 'input'; input.required = true; input.maxLength = 5; input.placeholder = placeholder; input.value = value[name] || ''; input.dataset.field = name;
      input.pattern = name === 'endTime' ? '([01][0-9]|2[0-3]):[0-5][0-9]|24:00' : '([01][0-9]|2[0-3]):[0-5][0-9]'; input.title = 'Use 24-hour HH:mm format.'; field(label, input);
    }
    const remove = document.createElement('button'); remove.type = 'button'; remove.className = 'btn btn-outline self-end'; remove.textContent = 'Remove meeting'; remove.addEventListener('click', () => row.remove()); row.appendChild(remove); el('sectionMeetings').appendChild(row);
  }
  function criteriaEnabled(){for(const id of ['sectionRequiredSkills','sectionPreferredSkills','sectionSkillOptions','sectionTrainingRequired','sectionSexualTrainingRequired'])el(id).disabled=!el('sectionOwnCriteria').checked;}
  el('sectionOwnCriteria').addEventListener('change',criteriaEnabled);
  function form(section = null) {
    copying=false;el('sectionCopyTargetLabel').classList.add('hidden');editing = section?.id || null; el('sectionForm').reset(); el('sectionMeetings').replaceChildren();
    const criteria=section?.matchingCriteria||{};el('sectionOwnCriteria').checked=!!section?.matchingCriteria;for(const [key,id]of [['requiredSkills','sectionRequiredSkills'],['preferredSkills','sectionPreferredSkills'],['skillOptions','sectionSkillOptions']])el(id).value=(criteria[key]||[]).join('\n');el('sectionTrainingRequired').checked=!!criteria.trainingRequired;el('sectionSexualTrainingRequired').checked=!!criteria.sexualHarassmentTrainingRequired;criteriaEnabled();
    el('sectionFormTitle').textContent = editing ? 'Edit section' : 'Add section';
    if (section) {
      el('sectionName').value = section.name; el('sectionType').value = section.type;
      el('sectionPositions').value = section.taPositions; el('sectionHours').value = section.hoursPerWeek;
      el('sectionFaculty').value=section.facultyName||''; el('sectionLocation').value = section.location || ''; el('sectionNotes').value = section.notes || '';
      section.meetingTimes.forEach(addMeeting);
    } else addMeeting();
    el('sectionFormPanel').classList.remove('hidden'); el('sectionName').focus();
  }
  function render() {
    const visible = sections.filter(s => el('showArchivedSections').checked || s.status === 'active');
    const body = el('sectionRows'); body.replaceChildren();
    if (!visible.length) { const row = body.insertRow(); const cell = row.insertCell(); cell.colSpan = 5; cell.className = 'p-6 text-zinc-500'; cell.textContent = 'No sections to show. Add one or enable Show archived sections.'; }
    for (const section of visible) {
      const row = body.insertRow(); row.className = 'border-t border-zinc-100 align-top';
      const cells = Array.from({length:5}, () => { const cell = row.insertCell(); cell.className = 'p-4'; return cell; });
      text(cells[0], section.name, 'font-semibold'); text(cells[0], section.type.charAt(0).toUpperCase() + section.type.slice(1), 'text-zinc-500');
      if(section.facultyName)text(cells[0],'Faculty: '+section.facultyName,'text-zinc-500');
      if (section.status === 'archived') text(cells[0], 'Archived', 'text-amber-700 mt-1');
      section.meetingTimes.forEach(meeting => text(cells[1], days[meeting.dayOfWeek - 1] + ' · ' + meeting.startTime + '–' + meeting.endTime, 'whitespace-nowrap'));
      text(cells[2], section.taPositions + ' TA position' + (section.taPositions === 1 ? '' : 's'));
      text(cells[2], number(section.hoursPerWeek) + ' hours / TA / week', 'text-zinc-500');
      text(cells[3], section.location || 'No location recorded', 'text-zinc-600');
      if (section.notes) text(cells[3], section.notes, 'mt-2 whitespace-pre-wrap break-words');
      function button(label, action) { const btn = document.createElement('button'); btn.type = 'button'; btn.className = 'btn btn-outline text-xs mr-2 mb-2'; btn.textContent = label; btn.addEventListener('click', action); cells[4].appendChild(btn); }
      if(!canConfigure)continue;
      if(section.periodId&&section.status==='active')button('Edit',()=>form(section));
      button('Copy to cycle',()=>{form(section);editing=null;copying=true;el('sectionCopyTargetLabel').classList.remove('hidden');el('sectionFormTitle').textContent='Copy section into a cycle';const choices=[...el('sectionCopyTarget').options];if(choices.length)el('sectionCopyTarget').value=choices[choices.length-1].value;message('Choose the target cycle in the copy form, then save a separate section record.');});
      button(section.status === 'active' ? 'Archive' : 'Restore', async event => {
        const button = event.currentTarget; button.disabled = true; el('sectionCourse').disabled = true;
        try {
          await request(prefix + '/sections/' + section.id + (section.status === 'active' ? '' : '/restore'), {method:section.status === 'active' ? 'DELETE' : 'POST'});
          await load(); message(section.status === 'active' ? 'Section archived. Enable Show archived sections to restore it.' : 'Section restored.');
        } catch (error) { message(error.message || 'Could not update section.', true); button.disabled = false; }
        finally { el('sectionCourse').disabled = false; }
      });
    }
  }
  async function load() {
    const current = ++sequence; const course = el('sectionCourse').value;
    editing = null; sections = []; canConfigure = false; el('newSection').classList.add('hidden'); el('sectionFormPanel').classList.add('hidden'); el('newSection').disabled = true;
    el('sectionRows').replaceChildren(); el('sectionSummary').textContent = ''; el('sectionCourseInfo').textContent = '';
    if (!course) { message('Choose a course to manage its sections.'); return; }
    message('Loading sections…');
    const data = await request(prefix + '/modules/' + encodeURIComponent(course) + '/sections?periodId='+encodeURIComponent(el('sectionPeriod').value));
    if (current !== sequence) return;
    sections = data.sections; canConfigure = data.canConfigure === true; el('newSection').classList.toggle('hidden',!canConfigure); el('newSection').disabled = el('sectionPeriod').value==='legacy_unscoped';
    el('sectionCourseInfo').textContent = [data.module.code, data.module.name, data.module.term, data.module.organiserName].filter(Boolean).join(' · ');
    el('sectionSummary').textContent = 'Active sections: ' + sections.filter(s => s.status === 'active').length + ' · TA positions requested: ' + data.taPositionsRequested + ' · Combined TA hours / week: ' + number(data.weeklyHoursRequested);
    render(); message(canConfigure?'':'Read-only access for this course.');
  }
  el('sectionPeriod').addEventListener('change',()=>load().catch(e=>message(e.message,true)));
  el('newSection').addEventListener('click', () => form());
  el('cancelSection').addEventListener('click', () => el('sectionFormPanel').classList.add('hidden'));
  el('addSectionMeeting').addEventListener('click', () => addMeeting());
  el('showArchivedSections').addEventListener('change', render);
  el('sectionCourse').addEventListener('change', () => load().catch(error => message(error.message || 'Could not load sections.', true)));
  el('sectionForm').addEventListener('submit', async event => {
    event.preventDefault(); const course = el('sectionCourse').value;
    const meetingTimes = [...el('sectionMeetings').children].map(row => {
      const meeting = {}; row.querySelectorAll('[data-field]').forEach(input => meeting[input.dataset.field] = input.dataset.field === 'dayOfWeek' ? Number(input.value) : input.value.trim()); return meeting;
    });
    if (!meetingTimes.length) { message('Add at least one weekly meeting time.', true); return; }
    const body = {periodId:copying?el('sectionCopyTarget').value:el('sectionPeriod').value,facultyName:el('sectionFaculty').value.trim(),name:el('sectionName').value.trim(), type:el('sectionType').value, timeZone:'America/Los_Angeles', meetingTimes,
      taPositions:Number(el('sectionPositions').value), hoursPerWeek:Number(el('sectionHours').value), location:el('sectionLocation').value.trim(), notes:el('sectionNotes').value.trim()};
    body.matchingCriteria=el('sectionOwnCriteria').checked?{requiredSkills:el('sectionRequiredSkills').value.split('\n').map(s=>s.trim()).filter(Boolean),preferredSkills:el('sectionPreferredSkills').value.split('\n').map(s=>s.trim()).filter(Boolean),skillOptions:el('sectionSkillOptions').value.split('\n').map(s=>s.trim()).filter(Boolean),studentLevels:[],subjectInterests:[],trainingRequired:el('sectionTrainingRequired').checked,sexualHarassmentTrainingRequired:el('sectionSexualTrainingRequired').checked,teachingExperiencePreferred:false}:null;
    el('sectionFields').disabled = true; el('sectionCourse').disabled = true;
    try {
      await request(prefix + (editing ? '/sections/' + editing : '/modules/' + course + '/sections'), {method:editing ? 'PUT' : 'POST', body:JSON.stringify(body)});
      el('sectionPeriod').value=body.periodId;await load(); message('Section saved.');
    } catch (error) { message(error.message || 'Could not save section.', true); }
    finally { el('sectionFields').disabled = false; el('sectionCourse').disabled = false; }
  });
  request(prefix + '/modules').then(async modules => {
    const periods=await request(prefix.startsWith('/admin')?'/admin/application-periods':'/organiser/application-periods');
    periods.forEach(p=>{el('sectionPeriod').add(new Option(p.name,p.id));el('sectionCopyTarget').add(new Option(p.name,p.id));});if(periods.length)el('sectionPeriod').value=periods[periods.length-1].id;
    el('sectionCourse').replaceChildren(new Option('Choose a course', ''));
    modules.forEach(module => el('sectionCourse').add(new Option([module.code, module.name, module.term].filter(Boolean).join(' · '), module.id)));
    const requested = new URLSearchParams(location.search).get('moduleId');
    el('sectionCourse').value = modules.some(m => m.id === requested) ? requested : modules[0]?.id || '';
    el('sectionCourse').disabled = false;
    if (!modules.length) { message('No courses are assigned to you. Ask an administrator to create or assign a course.'); return; }
    await load();
  }).catch(error => message(error.message || 'Could not load courses.', true));
})();
