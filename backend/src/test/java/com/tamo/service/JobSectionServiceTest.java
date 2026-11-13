package com.tamo.service;

import com.google.gson.*;
import com.tamo.model.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class JobSectionServiceTest {
    DataStore data; JsonDataService store; OrganiserService organiser;
    @BeforeEach void setup() {
        data=TestDataFactory.createTestDataStore(); store=JsonDataService.detached(data); organiser=new OrganiserService(store);
        section("lecture", "m001", "period_test", 1); section("lab", "m001", "period_test", 3);
        section("other", "m002", "period_test", 2); section("next", "m001", "next_cycle", 2);
    }
    void section(String id,String course,String period,int day) {
        CourseSection s=new CourseSection(); s.setId(id);s.setModuleId(course);s.setPeriodId(period);s.setStatus("active");s.setName(id);s.setType("lecture");s.setTimeZone("America/Los_Angeles");
        CourseSection.MeetingTime time=new CourseSection.MeetingTime();time.setDayOfWeek(day);time.setStartTime("09:00");time.setEndTime("10:00");s.setMeetingTimes(List.of(time));data.getCourseSections().add(s);
    }
    JsonObject posting() { return JsonParser.parseString("{\"title\":\"Lecture and lab TA\",\"moduleId\":\"m001\",\"moduleCode\":\"EBU6304\",\"sectionIds\":[\"lecture\",\"lab\"]}").getAsJsonObject(); }
    @Test void savesLectureAndLabInsteadOfDates() {
        JsonObject body=posting();body.add("workDates",JsonParser.parseString("[\"2026-10-19\"]"));
        Job job=(Job)organiser.createJob("u005",body).get("job");assertNotNull(job);assertEquals(List.of("lecture","lab"),job.getSectionIds());assertTrue(job.getWorkDates().isEmpty());
        assertTrue(JobSectionService.acceptsPeriod(data,job,"period_test"));assertFalse(JobSectionService.acceptsPeriod(data,job,"next_cycle"));
    }
    @Test void rejectsForeignArchivedAndMixedCycleSectionsWithoutCreatingPosting() {
        int before=data.getJobs().size();
        for(String ids:List.of("[\"other\"]","[\"lecture\",\"next\"]","[]","[\"lecture\",\"lecture\"]","[\"missing\"]")) {
            JsonObject body=posting();body.add("sectionIds",JsonParser.parseString(ids));assertEquals(400,organiser.createJob("u005",body).get("code"));assertEquals(before,data.getJobs().size());
        }
        data.getCourseSections().get(0).setStatus("archived");assertEquals(400,organiser.createJob("u005",posting()).get("code"));
    }
    @Test void editRetainsSelectionAndProtectsExistingApplicants() {
        Job job=(Job)organiser.createJob("u005",posting()).get("job");
        Application application=new Application();application.setId("new_app");application.setJobId(job.getId());data.getApplications().add(application);
        JsonObject body=posting();body.add("sectionIds",JsonParser.parseString("[\"lecture\"]"));
        assertEquals(409,organiser.updateJob("u005",job.getId(),body).get("code"));assertEquals(List.of("lecture","lab"),job.getSectionIds());
        body=posting();body.add("sectionIds",JsonParser.parseString("[\"lab\",\"lecture\"]"));assertNotNull(organiser.updateJob("u005",job.getId(),body).get("job"));
    }
    @Test void adminPostsForCourseWithoutBecomingItsTeacher() {
        JsonObject body=posting();
        body.add("sectionIds",JsonParser.parseString("[\"lecture\"]"));
        Job job=(Job)organiser.createJob("u010",body).get("job");
        assertNotNull(job);
        assertEquals("u005",job.getOrganiserId());
        assertTrue(ReviewAccess.organiserOwns(data,"u005",job));
    }
    @Test void matchingOnlyShowsSelectedSections() {
        Job job=data.getJobs().get(0);job.setSectionIds(List.of("lecture"));
        Application app=data.getApplications().get(0);app.setPeriodId("period_test");
        JsonArray rows=new SectionMatchingService().match(data,app,job,"u005").getAsJsonArray("sections");
        assertEquals(1,rows.size());assertEquals("lecture",rows.get(0).getAsJsonObject().get("sectionId").getAsString());
    }
    @Test void assignmentRejectsSectionOutsidePostingBeforeApproval() {
        Application app=data.getApplications().get(0);Job job=data.getJobs().stream().filter(j->j.getId().equals(app.getJobId())).findFirst().orElseThrow();job.setSectionIds(List.of("lecture"));
        JsonObject body=new JsonObject();body.addProperty("applicationId",app.getId());body.addProperty("sectionId","lab");
        var error=assertThrows(SectionAssignmentService.Problem.class,()->new SectionAssignmentService(store).assign("u010","admin",body));
        assertTrue(error.getMessage().contains("not included"));assertTrue(data.getSectionAssignments().isEmpty());
    }
    @Test void applicantCannotApplyToAnotherCyclesPosting() {
        Job job=data.getJobs().get(0);job.setSectionIds(List.of("next"));
        assertEquals(409,new ApplicantService(store).applyForJob("u003",job.getId(),"Synthetic application",List.of()).get("code"));
    }
    @Test void mismatchedCourseCodeDoesNotCreatePosting() {
        JsonObject body=posting();body.addProperty("moduleCode","EBU5476");int before=data.getJobs().size();
        assertEquals(400,organiser.createJob("u005",body).get("code"));assertEquals(before,data.getJobs().size());
    }
    @Test void legacyPostingsKeepCourseScope() {
        Job job=data.getJobs().get(0);assertNull(job.getSectionIds());assertTrue(JobSectionService.includes(job,data.getCourseSections().get(0)));assertTrue(JobSectionService.acceptsPeriod(data,job,"future"));
    }
}
