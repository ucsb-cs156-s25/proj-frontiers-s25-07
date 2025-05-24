package edu.ucsb.cs156.frontiers.controllers;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Tag(name = "Webhooks Controller")
@RestController
@RequestMapping("/api/webhooks")
@Slf4j
public class WebhookController {


    private final CourseRepository courseRepository;
    private final RosterStudentRepository rosterStudentRepository;

    public WebhookController(CourseRepository courseRepository, RosterStudentRepository rosterStudentRepository) {
        this.courseRepository = courseRepository;
        this.rosterStudentRepository = rosterStudentRepository;
    }

    /**
    * Accepts webhooks from GitHub, currently to update the membership status of a RosterStudent.
    * @param jsonBody body of the webhook. The description of the currently used webhook is available in docs/webhooks.md
    *
    * @return either the word success so GitHub will not flag the webhook as a failure, or the updated RosterStudent
    */
    @PostMapping("/github")
    public ResponseEntity<String> createGitHubWebhook(@RequestBody JsonNode jsonBody) throws JsonProcessingException {
        log.info("Received GitHub webhook: {}", jsonBody.toString());

        if(!jsonBody.has("action")){
            return ResponseEntity.ok().body("success");
        }
        
        String action = jsonBody.get("action").asText();
        log.info("Webhook action: {}", action);
        
        // Early return if not an action we care about
        if(!action.equals("member_added") && !action.equals("member_invited")) {
            return ResponseEntity.ok().body("success");
        }
        
        // Extract GitHub login based on payload structure
        String githubLogin = null;
        String installationId = null;
        
        // For member_added events, the structure is different
        if (action.equals("member_added")) {
            if (!jsonBody.has("membership") || 
                !jsonBody.get("membership").has("user") || 
                !jsonBody.get("membership").get("user").has("login") ||
                !jsonBody.has("installation") || 
                !jsonBody.get("installation").has("id")) {
                return ResponseEntity.ok().body("success");
            }
            
            githubLogin = jsonBody.get("membership").get("user").get("login").asText();
            installationId = jsonBody.get("installation").get("id").asText();
        } 
        // For member_invited events, use the original structure
        else { // must be "member_invited" based on earlier check
            if (!jsonBody.has("user") || 
                !jsonBody.get("user").has("login") || 
                !jsonBody.has("installation") || 
                !jsonBody.get("installation").has("id")) {
                return ResponseEntity.ok().body("success");
            }
            
            githubLogin = jsonBody.get("user").get("login").asText();
            installationId = jsonBody.get("installation").get("id").asText();
        }
        
        log.info("GitHub login: {}, Installation ID: {}", githubLogin, installationId);
        
        Optional<Course> course = courseRepository.findByInstallationId(installationId);
        log.info("Course found: {}", course.isPresent());
        
        if(!course.isPresent()){
            log.warn("No course found with installation ID: {}", installationId);
            return ResponseEntity.ok().body("success");
        }
        
        Optional<RosterStudent> student = rosterStudentRepository.findByCourseAndGithubLogin(course.get(), githubLogin);
        log.info("Student found: {}", student.isPresent());
        
        if(!student.isPresent()){
            log.warn("No student found with GitHub login: {} in course: {}", githubLogin, course.get().getCourseName());
            return ResponseEntity.ok().body("success");
        }
        
        RosterStudent updatedStudent = student.get();
        log.info("Current student org status: {}", updatedStudent.getOrgStatus());
        
        // Update status based on action
        if(action.equals("member_added")) {
            updatedStudent.setOrgStatus(OrgStatus.MEMBER);
            log.info("Setting status to MEMBER");
        } else { // must be "member_invited" based on earlier check
            updatedStudent.setOrgStatus(OrgStatus.INVITED);
            log.info("Setting status to INVITED");
        }
        
        rosterStudentRepository.save(updatedStudent);
        log.info("Student saved with new org status: {}", updatedStudent.getOrgStatus());
        return ResponseEntity.ok(updatedStudent.toString());
    }
}
