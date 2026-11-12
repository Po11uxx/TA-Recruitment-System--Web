<%@ page pageEncoding="UTF-8" %>
<%@ include file="/common/auth-check.jsp" %>
<jsp:include page="/common/layout-top.jsp"/>
<div class="max-w-5xl mx-auto space-y-6">
  <div><h1 class="text-3xl font-bold">Weekly Availability</h1><p class="text-zinc-500 mt-2">Mark the times you cannot work during this application period.</p></div>
  <p class="text-sm text-zinc-600">Monday–Friday, 08:00–20:00 UCI campus time. Choose a start and end on the same day. Thin gray lines mark 10-minute intervals. Use arrow keys and Space to select, or Escape to cancel.</p>
  <div><label for="availabilityPeriod" class="block text-sm font-medium mb-2">Application period</label><select id="availabilityPeriod" class="input" disabled><option value="">Loading periods…</option></select></div>
  <p id="availabilityStatus" role="status" aria-live="polite" class="text-sm text-zinc-600"></p>
  <form id="availabilityForm" class="card p-6 hidden">
    <fieldset id="availabilityFields" class="space-y-6" disabled>
      <legend class="font-semibold text-lg mb-4">Weekly busy times</legend>
      <div id="availabilityPrevious" class="hidden rounded-lg border border-blue-200 p-3 text-sm"></div>
      <div class="flex flex-wrap items-center justify-between gap-3">
        <p class="text-sm"><span class="font-semibold text-blue-800">Busy</span> · Unmarked times in this calendar will be available after confirmation.</p>
        <button id="clearBusy" type="button" class="btn btn-outline">Clear visible busy times</button>
      </div>
      <div id="availabilityGridScroll" class="availability-grid-scroll" tabindex="0" aria-label="Weekly schedule, 08:00–20:00, scroll to view">
        <table id="availabilityGrid" class="availability-grid" aria-label="Weekly busy times"></table>
      </div>
      <p id="availabilityRangeHint" class="text-sm text-zinc-600" role="status"></p>
      <ul id="availabilityRanges" class="text-sm" aria-label="Unavailable time ranges"></ul>
      <label class="flex items-start gap-2 text-sm"><input type="checkbox" id="confirmUnmarkedAvailable" required> I confirm that unmarked times on weekdays from 08:00 to 20:00 are available for TA work.</label>
      <div><label for="availabilityNotes" class="block text-sm font-medium mb-2">Other restrictions or notes</label><textarea id="availabilityNotes" class="input" rows="3" maxlength="2000" placeholder="Other scheduling constraints or comments"></textarea></div>
      <p class="text-sm text-zinc-500">Saved weekend and out-of-hours conflicts are retained. Saving updates your schedule for future submissions. Each submitted application keeps its own copy; it will not change when you edit this schedule.</p>
      <button id="saveAvailability" type="submit" class="btn btn-primary">Save availability</button>
    </fieldset>
  </form>
</div>
<jsp:include page="/common/layout-bottom.jsp"/>
<script src="<%= request.getContextPath() %>/static/js/availability-view.js"></script>
<script src="<%= request.getContextPath() %>/static/js/availability.js?v=weekday-timeline-1"></script>
