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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@Tag(name = "Webhooks Controller")
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final CourseRepository courseRepository;
    private final RosterStudentRepository rosterStudentRepository;
    private final UserRepository userRepository;

    public WebhookController(CourseRepository courseRepository, RosterStudentRepository rosterStudentRepository, UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.rosterStudentRepository = rosterStudentRepository;
        this.userRepository = userRepository;
    }

    /**
    * Accepts webhooks from GitHub, currently to update the membership status of a RosterStudent.
    * @param jsonBody body of the webhook. The description of the currently used webhook is available in docs/webhooks.md
    *
    * @return either the word success so GitHub will not flag the webhook as a failure, or the updated RosterStudent
    */
    @PostMapping("/github")
    public ResponseEntity<String> createGitHubWebhook(@RequestBody JsonNode jsonBody) throws JsonProcessingException {

        log.info("Received GitHub webhook: {}", jsonBody);
        
        if(jsonBody.has("action")){
            String action = jsonBody.get("action").asText();
            
            // Handle member_added event
            if(action.equals("member_added") || action.equals("member_invited")){
                // For member_invited, the structure is different - user login is directly in the payload
                String githubLogin;
                String installationId;
                
                if(action.equals("member_invited")) {
                    // Check if all required fields exist for member_invited payload
                    if (!jsonBody.has("user") || 
                        !jsonBody.get("user").has("login") ||
                        !jsonBody.has("installation") ||
                        !jsonBody.get("installation").has("id")) {
                        
                        log.warn("Webhook payload missing required fields: {}", jsonBody);
                        return ResponseEntity.ok().body("success");
                    }
                    
                    githubLogin = jsonBody.get("user").get("login").asText();
                    installationId = jsonBody.get("installation").get("id").asText();
                } else {
                    // Original check for member_added payload
                    if (!jsonBody.has("membership") || 
                        !jsonBody.get("membership").has("user") || 
                        !jsonBody.get("membership").get("user").has("login") ||
                        !jsonBody.has("installation") ||
                        !jsonBody.get("installation").has("id")) {
                        
                        log.warn("Webhook payload missing required fields: {}", jsonBody);
                        return ResponseEntity.ok().body("success");
                    }
                    
                    githubLogin = jsonBody.get("membership").get("user").get("login").asText();
                    installationId = jsonBody.get("installation").get("id").asText();
                }
                
                Optional<Course> course = courseRepository.findByInstallationId(installationId);
                
                if(course.isPresent()){
                    // First try to find by GitHub login
                    Optional<RosterStudent> student = rosterStudentRepository.findByCourseAndGithubLogin(course.get(), githubLogin);
                    
                    if(!student.isPresent()) {
                        // If not found by GitHub login, try to find by email using the GitHub login
                        // This approach doesn't require UserRepository
                        log.info("Student not found by GitHub login, trying to find by email for GitHub user: {}", githubLogin);
                        
                        // Find all students with matching email in the course
                        List<RosterStudent> studentsInCourse = rosterStudentRepository.findAllByEmail(githubLogin + "@ucsb.edu");
                        Optional<RosterStudent> studentInCourse = studentsInCourse.stream()
                            .filter(s -> s.getCourse().getId().equals(course.get().getId()))
                            .findFirst();
                            
                        if(studentInCourse.isPresent()) {
                            RosterStudent updatedStudent = studentInCourse.get();
                            // Update the GitHub login
                            updatedStudent.setGithubLogin(githubLogin);
                            
                            // Set appropriate status based on action
                            if(action.equals("member_added")) {
                                updatedStudent.setOrgStatus(OrgStatus.MEMBER);
                            } else if(action.equals("member_invited")) {
                                updatedStudent.setOrgStatus(OrgStatus.INVITED);
                            }
                            
                            rosterStudentRepository.save(updatedStudent);
                            return ResponseEntity.ok(updatedStudent.toString());
                        } else {
                            log.info("GitHub user {} was added to course {}, but no matching roster student was found", 
                                     githubLogin, course.get().getCourseName());
                        }
                    } else {
                        // Existing code for when student is found by GitHub login
                        RosterStudent updatedStudent = student.get();
                        
                        // Set appropriate status based on action
                        if(action.equals("member_added")) {
                            updatedStudent.setOrgStatus(OrgStatus.MEMBER);
                        } else if(action.equals("member_invited")) {
                            updatedStudent.setOrgStatus(OrgStatus.INVITED);
                        }
                        
                        rosterStudentRepository.save(updatedStudent);
                        return ResponseEntity.ok(updatedStudent.toString());
                    }
                } else {
                    log.warn("Received webhook for installation ID {} but no matching course was found", installationId);
                }
            } 
            // Handle member_removed event
            else if(action.equals("member_removed")) {
                // Check if all required fields exist in the payload
                if (!jsonBody.has("membership") || 
                    !jsonBody.get("membership").has("user") || 
                    !jsonBody.get("membership").get("user").has("login") ||
                    !jsonBody.has("installation") ||
                    !jsonBody.get("installation").has("id")) {
                    
                    log.warn("Webhook payload missing required fields: {}", jsonBody);
                    return ResponseEntity.ok().body("success");
                }
                
                String githubLogin = jsonBody.get("membership").get("user").get("login").asText();
                String installationId = jsonBody.get("installation").get("id").asText();
                Optional<Course> course = courseRepository.findByInstallationId(installationId);
                
                if(course.isPresent()){
                    Optional<RosterStudent> student = rosterStudentRepository.findByCourseAndGithubLogin(course.get(), githubLogin);
                    if(student.isPresent()){
                        RosterStudent updatedStudent = student.get();
                        updatedStudent.setOrgStatus(OrgStatus.NONE);
                        rosterStudentRepository.save(updatedStudent);
                        return ResponseEntity.ok(updatedStudent.toString());
                    }
                }
            }
        }
        return ResponseEntity.ok().body("success");
    }
}
