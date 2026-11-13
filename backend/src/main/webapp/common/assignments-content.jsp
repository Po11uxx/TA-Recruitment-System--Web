<%@ page pageEncoding="UTF-8" %>
<div class="max-w-6xl mx-auto space-y-6 pb-12">
  <div><h1 class="text-3xl font-bold" id="assignmentsTitle"><%= "applicant".equals(session.getAttribute("role")) ? "My TA Placement" : "TA Assignments" %></h1><p id="assignmentTimeDescription" class="text-zinc-500 mt-2">Review your TA placement, section meetings and weekly hours here. Dates use your device time zone. Weekly meeting times are labeled with their course time zone. Totals and capacity are counted separately for each application period.</p></div>
  <div id="assignmentMessage" role="status" class="text-sm"></div>
  <div class="flex flex-wrap gap-4">
    <label class="text-sm">Application period<select id="assignmentPeriod" class="input mt-1"></select></label>
    <label class="text-sm" id="courseFilterLabel">Course<select id="assignmentCourse" class="input mt-1"></select></label>
    <label class="text-sm flex items-center gap-2"><input type="checkbox" id="assignmentHistory">Show other cancelled assignments</label>
    <label class="text-sm">Result status<select id="assignmentResultFilter" class="input mt-1"><option value="">All results</option><option value="draft">Draft</option><option value="pending">Awaiting reply</option><option value="accepted">Accepted</option><option value="declined">Declined</option><option value="cancelled">Cancelled</option></select></label>
    <button type="button" id="refreshAssignments" class="btn btn-outline self-end">Refresh</button>
  </div>
  <p id="assignmentResultSummary" class="text-sm font-medium" role="status"></p>
  <div id="assignmentStaff" class="space-y-5 hidden">
    <p id="assignmentAccess" class="text-sm hidden" role="status">Read-only access for this course.</p>
    <section id="assignmentDraftPanel" class="card p-5 space-y-4 hidden" aria-label="Assign a TA">
      <h2 class="text-lg font-bold">Prepare an assignment draft</h2>
      <p class="text-sm text-zinc-500">Choose an approved application or assign an active Applicant directly.</p>
      <form id="assignTaForm" class="space-y-4"><fieldset id="assignTaFields" class="space-y-4">
        <label class="block text-sm">Assignment source<select id="assignmentSource" class="input mt-1"><option value="application">Approved application</option><option value="direct">Direct assignment</option></select></label>
        <label class="block text-sm">Applicant<select id="assignmentCandidate" class="input mt-1" required></select></label>
        <label class="block text-sm">Section<select id="assignmentSection" class="input mt-1" required></select></label>
        <p id="assignmentPreview" class="text-sm font-medium whitespace-pre-line"></p>
        <div id="manualConfirmation" class="hidden border rounded-lg p-4 space-y-3">
          <label class="flex gap-2 text-sm"><input id="confirmPending" type="checkbox">I have manually confirmed this applicant can attend all section meetings.</label>
          <label class="block text-sm">Confirmation note (10–1000 characters)<textarea id="confirmationNote" class="input mt-1" maxlength="1000" rows="3"></textarea></label>
        </div>
        <label id="assignmentDraftReasonLabel" class="block text-sm hidden">Reason for direct assignment<textarea id="assignmentDraftReason" class="input mt-1" maxlength="1000" rows="2"></textarea></label>
        <button class="btn btn-primary" id="assignTaButton" type="submit">Save draft</button>
      </fieldset></form>
    </section>
    <section class="card p-5 space-y-3" aria-label="Section capacity"><h2 class="text-lg font-bold">Section capacity · reserved / requested</h2><div id="assignmentCapacity" class="space-y-2 text-sm"></div></section>
  </div>
  <section class="space-y-3" aria-label="Weekly workload"><h2 class="text-lg font-bold"><%= "applicant".equals(session.getAttribute("role")) ? "My weekly hours · selected period" : "Weekly hours by applicant · selected period" %></h2><p id="assignmentWorkloadDescription" class="text-sm text-zinc-500">Reserved hours include drafts and pending replies; accepted hours count only active assignments accepted by the student. Totals cover all courses in the selected period.</p><div id="assignmentWorkload" class="space-y-2"></div></section>
  <section class="space-y-3" aria-label="Assignments"><h2 class="text-lg font-bold">Assignments</h2><div id="assignmentRows" class="space-y-4"></div></section>
  <dialog id="assignmentActionDialog" class="rounded-xl p-6 max-w-lg w-full backdrop:bg-black/40">
    <form id="assignmentActionForm" class="space-y-4"><h2 id="assignmentActionTitle" class="text-lg font-bold"></h2><p id="assignmentActionDetails" class="text-sm whitespace-pre-line"></p>
      <label id="assignmentDeclineReasonLabel" class="block text-sm hidden">Reason for declining<textarea id="assignmentDeclineReason" class="input mt-1" maxlength="1000" rows="3"></textarea></label>
      <p id="assignmentActionError" role="alert" class="text-sm text-red-700"></p>
      <div class="flex gap-3"><button id="submitAssignmentAction" type="submit" class="btn btn-primary">Confirm</button><button type="button" id="closeAssignmentAction" class="btn btn-outline">Back</button></div>
    </form>
  </dialog>
  <dialog id="cancelAssignmentDialog" class="rounded-xl p-6 max-w-lg w-full backdrop:bg-black/40">
    <form id="cancelAssignmentForm" class="space-y-4"><h2 class="text-lg font-bold">Cancel assignment</h2><p id="cancelAssignmentName" class="text-sm"></p>
      <label class="block text-sm">Reason<textarea id="cancelAssignmentReason" class="input mt-1" required maxlength="1000" rows="3"></textarea></label>
      <p id="cancelAssignmentError" role="alert" class="text-sm text-red-700"></p>
      <div class="flex gap-3"><button id="submitCancelAssignment" type="submit" class="btn btn-primary">Confirm cancellation</button><button type="button" id="closeCancelAssignment" class="btn btn-outline">Keep assignment</button></div>
    </form>
  </dialog>
</div>
