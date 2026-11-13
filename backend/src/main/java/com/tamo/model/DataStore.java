package com.tamo.model;

import java.util.List;

/**
 * Represents a DataStore entity in the system.
 */
public class DataStore {
    /**
     * The users of the entity.
     */
    private List<User> users;
    private List<ReviewRecord> reviewRecords=new java.util.ArrayList<>();
    public List<ReviewRecord> getReviewRecords(){return reviewRecords;}
    public void setReviewRecords(List<ReviewRecord> value){reviewRecords=value;}
    private List<OfferEmail> offerEmails = new java.util.ArrayList<>();
    public List<OfferEmail> getOfferEmails() { return offerEmails; }
    private List<SectionAssignment> sectionAssignments = new java.util.ArrayList<>();
    public List<SectionAssignment> getSectionAssignments() { return sectionAssignments; }
    public void setSectionAssignments(List<SectionAssignment> value) { sectionAssignments = value; }
    private List<CourseSection> courseSections = new java.util.ArrayList<>();
    public List<CourseSection> getCourseSections() { return courseSections; }
    public void setCourseSections(List<CourseSection> value) { courseSections = value; }
    private List<WeeklyAvailability> weeklyAvailabilities = new java.util.ArrayList<>();
    public List<WeeklyAvailability> getWeeklyAvailabilities() { return weeklyAvailabilities; }
    public void setWeeklyAvailabilities(List<WeeklyAvailability> value) { weeklyAvailabilities = value; }
    private List<ApplicationPeriod> applicationPeriods = new java.util.ArrayList<>();
    public List<ApplicationPeriod> getApplicationPeriods() { return applicationPeriods; }
    public void setApplicationPeriods(List<ApplicationPeriod> value) { applicationPeriods = value; }
    /**
     * The profiles of the entity.
     */
    private List<Profile> profiles;
    /**
     * The modules of the entity.
     */
    private List<Module> modules;
    /**
     * The jobs of the entity.
     */
    private List<Job> jobs;
    /**
     * The applications of the entity.
     */
    private List<Application> applications;
    /**
     * The interviews of the entity.
     */
    private List<Interview> interviews;
    /**
     * The notifications of the entity.
     */
    private List<Notification> notifications;
    /**
     * The terms of the entity.
     */
    private List<Term> terms;
    /**
     * The departments of the entity.
     */
    private List<Department> departments;
    /**
     * The logs of the entity.
     */
    private List<LogEntry> logs;
    /**
     * The workloadAssignments of the entity.
     */
    private List<WorkloadAssignment> workloadAssignments;
    /**
     * The messages of the entity.
     */
    private List<Message> messages;


    /**
     * Gets the users.
     * @return the users
     */
    public List<User> getUsers() { return users; }
    /**
     * Sets the users.
     * @param users the users to set
     */
    public void setUsers(List<User> users) { this.users = users; }
    /**
     * Gets the profiles.
     * @return the profiles
     */
    public List<Profile> getProfiles() { return profiles; }
    /**
     * Sets the profiles.
     * @param profiles the profiles to set
     */
    public void setProfiles(List<Profile> profiles) { this.profiles = profiles; }
    /**
     * Gets the modules.
     * @return the modules
     */
    public List<Module> getModules() { return modules; }
    /**
     * Sets the modules.
     * @param modules the modules to set
     */
    public void setModules(List<Module> modules) { this.modules = modules; }
    /**
     * Gets the jobs.
     * @return the jobs
     */
    public List<Job> getJobs() { return jobs; }
    /**
     * Sets the jobs.
     * @param jobs the jobs to set
     */
    public void setJobs(List<Job> jobs) { this.jobs = jobs; }
    /**
     * Gets the applications.
     * @return the applications
     */
    public List<Application> getApplications() { return applications; }
    /**
     * Sets the applications.
     * @param applications the applications to set
     */
    public void setApplications(List<Application> applications) { this.applications = applications; }
    /**
     * Gets the interviews.
     * @return the interviews
     */
    public List<Interview> getInterviews() { return interviews; }
    /**
     * Sets the interviews.
     * @param interviews the interviews to set
     */
    public void setInterviews(List<Interview> interviews) { this.interviews = interviews; }
    /**
     * Gets the notifications.
     * @return the notifications
     */
    public List<Notification> getNotifications() { return notifications; }
    /**
     * Sets the notifications.
     * @param notifications the notifications to set
     */
    public void setNotifications(List<Notification> notifications) { this.notifications = notifications; }
    /**
     * Gets the terms.
     * @return the terms
     */
    public List<Term> getTerms() { return terms; }
    /**
     * Sets the terms.
     * @param terms the terms to set
     */
    public void setTerms(List<Term> terms) { this.terms = terms; }
    /**
     * Gets the departments.
     * @return the departments
     */
    public List<Department> getDepartments() { return departments; }
    /**
     * Sets the departments.
     * @param departments the departments to set
     */
    public void setDepartments(List<Department> departments) { this.departments = departments; }
    /**
     * Gets the logs.
     * @return the logs
     */
    public List<LogEntry> getLogs() { return logs; }
    /**
     * Sets the logs.
     * @param logs the logs to set
     */
    public void setLogs(List<LogEntry> logs) { this.logs = logs; }
    /**
     * Gets the workloadassignments.
     * @return the workloadassignments
     */
    public List<WorkloadAssignment> getWorkloadAssignments() { return workloadAssignments; }
    /**
     * Sets the workloadassignments.
     * @param workloadAssignments the workloadassignments to set
     */
    public void setWorkloadAssignments(List<WorkloadAssignment> workloadAssignments) { this.workloadAssignments = workloadAssignments; }

    /**
     * Gets the messages.
     * @return the messages
     */
    public List<Message> getMessages() { return messages; }
    /**
     * Sets the messages.
     * @param messages the messages to set
     */
    public void setMessages(List<Message> messages) { this.messages = messages; }

}
