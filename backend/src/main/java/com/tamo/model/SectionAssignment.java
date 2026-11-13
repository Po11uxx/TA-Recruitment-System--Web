package com.tamo.model;

/** An auditable assignment; cancellation retains the original section snapshot. */
public class SectionAssignment {
    private String id;
    public String getId() { return id; }
    public void setId(String value) { id = value; }
    private String applicationId;
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String value) { applicationId = value; }
    private String directReason;
    public String getDirectReason() { return directReason; }
    public void setDirectReason(String value) { directReason = value; }
    private String sectionId;
    public String getSectionId() { return sectionId; }
    public void setSectionId(String value) { sectionId = value; }
    private String applicantId;
    public String getApplicantId() { return applicantId; }
    public void setApplicantId(String value) { applicantId = value; }
    private String periodId;
    public String getPeriodId() { return periodId; }
    public void setPeriodId(String value) { periodId = value; }
    private String status;
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    private String assignedBy;
    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String value) { assignedBy = value; }
    private String assignedAt;
    public String getAssignedAt() { return assignedAt; }
    public void setAssignedAt(String value) { assignedAt = value; }
    private String cancelledBy;
    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String value) { cancelledBy = value; }
    private String cancelledAt;
    public String getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(String value) { cancelledAt = value; }
    private String cancellationReason;
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String value) { cancellationReason = value; }
    private String manualConfirmationNote;
    public String getManualConfirmationNote() { return manualConfirmationNote; }
    public void setManualConfirmationNote(String value) { manualConfirmationNote = value; }
    private CourseSection sectionSnapshot;
    public CourseSection getSectionSnapshot() { return sectionSnapshot; }
    public void setSectionSnapshot(CourseSection value) { sectionSnapshot = value; }
    private String publicationStatus = "draft";
    public String getPublicationStatus() { return publicationStatus; }
    public void setPublicationStatus(String value) { publicationStatus = value; }
    private String publishedBy;
    public String getPublishedBy() { return publishedBy; }
    public void setPublishedBy(String value) { publishedBy = value; }
    private String publishedAt;
    public String getPublishedAt() { return publishedAt; }
    public void setPublishedAt(String value) { publishedAt = value; }
    private String responseStatus = "pending";
    public String getResponseStatus() { return responseStatus; }
    public void setResponseStatus(String value) { responseStatus = value; }
    private String respondedAt;
    public String getRespondedAt() { return respondedAt; }
    public void setRespondedAt(String value) { respondedAt = value; }
    private String responseReason;
    public String getResponseReason() { return responseReason; }
    public void setResponseReason(String value) { responseReason = value; }
    private String responseDeadline;
    public String getResponseDeadline() { return responseDeadline; }
    public void setResponseDeadline(String value) { responseDeadline = value; }
    private String expiredAt;
    public String getExpiredAt() { return expiredAt; }
    public void setExpiredAt(String value) { expiredAt = value; }
}
