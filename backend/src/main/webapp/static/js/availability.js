(() => {
  const el = id => document.getElementById(id);
  const days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
  const minutesPerDay = 1440, firstHour = 8, lastHour = 20;
  const tick = 10, weekdays = days.slice(0, 5);
  const starts = Array.from({length:(lastHour - firstHour) * 60 / tick + 1}, (_, i) => firstHour * 60 + i * tick);
  const busy = days.map(() => new Uint8Array(minutesPerDay));
  const cells = [];
  let editable = false, loading = 0, anchor = null, focused = 0;
  const confirm = el('confirmUnmarkedAvailable');
  const time = minute => String(Math.floor(minute / 60)).padStart(2, '0') + ':' + String(minute % 60).padStart(2, '0');
  const minute = value => Number(value.slice(0, 2)) * 60 + Number(value.slice(3, 5));
  function status(message, error = false) {
    el('availabilityStatus').textContent = message;
    el('availabilityStatus').className = 'text-sm ' + (error ? 'text-red-700' : 'text-zinc-600');
  }
  function render() {
    el('availabilityRangeHint').textContent = anchor
      ? 'Start: ' + days[anchor.day] + ' ' + time(anchor.start) + '. Choose a later end time on the same day.'
      : 'Choose a start time, then an end time on the same day.';
    cells.forEach(cell => {
      cell.button.setAttribute('aria-pressed', String(cell === anchor));
      cell.container.querySelector('.availability-block')?.remove();
    });
    const list = el('availabilityRanges'); list.replaceChildren();
    for (const slot of intervals()) {
      const day = slot.dayOfWeek - 1;
      if (day >= weekdays.length) continue;
      const from = Math.max(firstHour * 60, minute(slot.startTime));
      const to = Math.min(lastHour * 60, minute(slot.endTime));
      if (from >= to) continue;
      const offset = (from - firstHour * 60) / tick;
      const row = Math.floor(offset);
      const block = document.createElement('span');
      block.className = 'availability-block';
      block.style.top = 'calc(var(--availability-tick) * ' + (offset - row) + ')';
      block.style.height = 'calc(var(--availability-tick) * ' + ((to - from) / tick) + ')';
      block.textContent = to - from >= 20 ? time(from) + '–' + time(to) : '';
      block.setAttribute('aria-hidden', 'true');
      cells[row * weekdays.length + day].container.append(block);
      const item = document.createElement('li');
      const label = days[day] + ' ' + time(from) + '–' + time(to);
      item.append(document.createTextNode(label + ' '));
      if (editable) {
        const remove = document.createElement('button'); remove.type = 'button';
        remove.className = 'btn btn-ghost'; remove.textContent = 'Remove';
        remove.setAttribute('aria-label', 'Remove ' + label);
        remove.addEventListener('click', () => {
          busy[day].fill(0, from, to); anchor = null; confirm.checked = false; render();
          cells[row * weekdays.length + day].button.focus();
        });
        item.append(remove);
      }
      list.append(item);
    }
  }
  function choose(cell) {
    if (!editable || el('availabilityFields').disabled) return;
    if (anchor?.day === cell.day && cell.start > anchor.start) {
      busy[cell.day].fill(1, anchor.start, cell.start);
      confirm.checked = false;
      anchor = null;
    } else if (cell === anchor) {
      anchor = null;
    } else if (cell.start < lastHour * 60) {
      anchor = cell;
    }
    render();
  }
  function buildGrid() {
    const table = el('availabilityGrid');
    const header = table.createTHead().insertRow();
    for (const label of ['Time', ...weekdays]) {
      const th = document.createElement('th'); th.scope = 'col'; th.textContent = label; header.append(th);
    }
    const tbody = table.createTBody();
    starts.forEach(start => {
      const row = tbody.insertRow();
      row.className = start % 60 === 0 ? 'availability-hour' : start % 30 === 0 ? 'availability-half-hour' : '';
      const label = document.createElement('th'); label.scope = 'row';
      label.textContent = start % 30 === 0 ? time(start) : ''; row.append(label);
      weekdays.forEach((name, day) => {
        const button = document.createElement('button'); button.type = 'button';
        button.setAttribute('aria-label', name + ' ' + time(start));
        const index = cells.length;
        const container = row.insertCell();
        const cell = {day, start, button, container}; cells.push(cell);
        button.tabIndex = index === focused ? 0 : -1;
        button.addEventListener('focus', () => {
          cells[focused].button.tabIndex = -1; focused = index; button.tabIndex = 0;
        });
        button.addEventListener('keydown', event => {
          if (event.key === 'Escape') { anchor = null; render(); return; }
          const offsets = {ArrowLeft:-1, ArrowRight:1, ArrowUp:-weekdays.length, ArrowDown:weekdays.length};
          if (!(event.key in offsets)) return;
          event.preventDefault();
          if ((event.key === 'ArrowLeft' && day === 0) || (event.key === 'ArrowRight' && day === weekdays.length - 1)) return;
          cells[index + offsets[event.key]]?.button.focus();
        });
        button.addEventListener('click', () => choose(cell));
        container.append(button);
      });
    });
  }
  function fill(schedule) {
    // Use minute precision so an existing 09:15 boundary is not silently rounded.
    const hasFullSchedule = !!schedule && !schedule.conflictsOnly;
    busy.forEach(day => day.fill(hasFullSchedule ? 1 : 0));
    function apply(intervals, value) {
      for (const slot of intervals || []) {
        const day = busy[slot.dayOfWeek - 1];
        if (day && slot.startTime && slot.endTime) day.fill(value, minute(slot.startTime), minute(slot.endTime));
      }
    }
    if (hasFullSchedule && !schedule.noAvailability) apply(schedule.availableSlots, 0);
    apply(schedule?.conflictSlots, 1);
    anchor = null; render();
    confirm.checked = false;
    el('availabilityNotes').value = schedule?.notes || '';
    const previous = el('availabilityPrevious');
    previous.replaceChildren();
    previous.classList.toggle('hidden', !schedule);
    if (schedule) {
      const detail = document.createElement('details');
      const title = document.createElement('summary');
      title.textContent = schedule.conflictsOnly ? 'Previously imported conflicts · review before confirming' : 'Previously saved schedule';
      const content = document.createElement('div'); detail.append(title, content); previous.append(detail);
      AvailabilityView.render(content, schedule);
    }
  }
  function intervals() {
    const result = [];
    busy.forEach((day, index) => {
      let start = 0;
      while (start < minutesPerDay) {
        if (!day[start]) { start++; continue; }
        let end = start + 1;
        while (end < minutesPerDay && day[end]) end++;
        result.push({dayOfWeek:index + 1, startTime:time(start), endTime:time(end)});
        start = end;
      }
    });
    return result;
  }
  async function loadPeriod() {
    const sequence = ++loading; editable = false; el('availabilityFields').disabled = true;
    const id = el('availabilityPeriod').value;
    if (!id) { el('availabilityForm').classList.add('hidden'); status('No application period selected.'); return; }
    status('Loading availability…');
    try {
      const result = await applicantApi.getAvailability(id);
      if (sequence !== loading) return;
      editable = result.editable; fill(result.availability); el('availabilityFields').disabled = !editable;
      el('availabilityForm').classList.remove('hidden');
      el('availabilityGridScroll').scrollTop = 0;
      status(!editable ? 'This period is not accepting updates. Your saved schedule is read-only.' :
        result.availability?.conflictsOnly ? 'Imported conflicts do not confirm unmarked time. Review the full week before saving.' :
        result.availability ? 'Saved schedule loaded. Changes apply to future submissions only.' : 'No schedule saved for this period yet.');
    } catch (error) {
      if (sequence === loading) { el('availabilityForm').classList.add('hidden'); status(error.message || 'Could not load availability.', true); }
    }
  }
  buildGrid();
  el('clearBusy').addEventListener('click', () => { busy.slice(0, weekdays.length).forEach(day => day.fill(0, firstHour * 60, lastHour * 60)); anchor = null; render(); confirm.checked = false; });
  el('availabilityPeriod').addEventListener('change', loadPeriod);
  el('availabilityForm').addEventListener('submit', async event => {
    event.preventDefault();
    if (!editable || el('availabilityFields').disabled || !confirm.checked) return;
    if (anchor) { status('Choose an end time or cancel the selection before saving.', true); return; }
    const id = el('availabilityPeriod').value;
    const payload = {timeZone:'America/Los_Angeles', conflictSlots:intervals(), confirmUnmarkedAvailable:true, notes:el('availabilityNotes').value.trim()};
    el('availabilityFields').disabled = true; el('availabilityPeriod').disabled = true;
    try {
      const saved = await applicantApi.saveAvailability(id, payload);
      fill(saved); status('Availability saved. Existing submitted applications keep their original snapshot.');
    } catch (error) { status(error.message || 'Could not save availability.', true); }
    finally { el('availabilityFields').disabled = !editable; el('availabilityPeriod').disabled = false; }
  });
  applicantApi.getAvailabilityPeriods().then(async periods => {
    el('availabilityPeriod').replaceChildren(new Option('Choose an application period', ''));
    periods.forEach(p => el('availabilityPeriod').add(new Option(p.name + ' · ' + p.effectiveStatus, p.id)));
    const requested = new URLSearchParams(location.search).get('periodId');
    el('availabilityPeriod').value = periods.some(p => p.id === requested) ? requested : (periods.find(p => p.editable) || periods[periods.length - 1])?.id || '';
    el('availabilityPeriod').disabled = false; await loadPeriod();
  }).catch(error => {
    el('availabilityPeriod').replaceChildren(new Option('Could not load application periods', ''));
    status(error.message || 'Could not load periods. Refresh the page to retry.', true);
  });
})();
