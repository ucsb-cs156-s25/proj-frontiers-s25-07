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
import java.util.stream.StreamSupport;

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
            if(action.equals("member_added")) {
                // Original check for member_added payload
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
                    // First try to find by GitHub login
                    Optional<RosterStudent> student = rosterStudentRepository.findByCourseAndGithubLogin(course.get(), githubLogin);
                    
                    if(!student.isPresent()) {
                        // If not found by GitHub login, try to find a student with INVITED status in this course
                        log.info("Student not found by GitHub login, trying to find previously invited student in course");
                        
                        Iterable<RosterStudent> allStudentsInCourse = rosterStudentRepository.findByCourseId(course.get().getId());
                        Optional<RosterStudent> invitedStudent = StreamSupport.stream(allStudentsInCourse.spliterator(), false)
                            .filter(s -> s.getOrgStatus() == OrgStatus.INVITED)
                            .findFirst();
                            
                        if(invitedStudent.isPresent()) {
                            RosterStudent updatedStudent = invitedStudent.get();
                            // Update the GitHub login and status
                            updatedStudent.setGithubLogin(githubLogin);
                            updatedStudent.setOrgStatus(OrgStatus.MEMBER);
                            
                            rosterStudentRepository.save(updatedStudent);
                            return ResponseEntity.ok(updatedStudent.toString());
                        } else {
                            // Try the email matching approach as a fallback
                            List<RosterStudent> studentsInCourse = rosterStudentRepository.findAllByEmail(githubLogin + "@ucsb.edu");
                            Optional<RosterStudent> studentInCourse = studentsInCourse.stream()
                                .filter(s -> s.getCourse().getId().equals(course.get().getId()))
                                .findFirst();
                                
                            if(studentInCourse.isPresent()) {
                                RosterStudent updatedStudent = studentInCourse.get();
                                updatedStudent.setGithubLogin(githubLogin);
                                updatedStudent.setOrgStatus(OrgStatus.MEMBER);
                                
                                rosterStudentRepository.save(updatedStudent);
                                return ResponseEntity.ok(updatedStudent.toString());
                            } else {
                                log.info("GitHub user {} was added to course {}, but no matching roster student was found", 
                                         githubLogin, course.get().getCourseName());
                            }
                        }
                    } else {
                        // Existing code for when student is found by GitHub login
                        RosterStudent updatedStudent = student.get();
                        updatedStudent.setOrgStatus(OrgStatus.MEMBER);
                        
                        rosterStudentRepository.save(updatedStudent);
                        return ResponseEntity.ok(updatedStudent.toString());
                    }
                } else {
                    log.warn("Received webhook for installation ID {} but no matching course was found", installationId);
                }
            } 
            // Handle member_invited event
            else if(action.equals("member_invited")) {
                // Check if all required fields exist for member_invited payload
                if ((!jsonBody.has("user") || !jsonBody.get("user").has("login")) && 
                    (!jsonBody.has("invitation") || !jsonBody.get("invitation").has("email")) ||
                    !jsonBody.has("installation") ||
                    !jsonBody.get("installation").has("id")) {
                    
                    log.warn("Webhook payload missing required fields: {}", jsonBody);
                    return ResponseEntity.ok().body("success");
                }
                
                // Handle both GitHub username invites and email invites
                if (jsonBody.has("user") && jsonBody.get("user").has("login")) {
                    String githubLogin = jsonBody.get("user").get("login").asText();
                    String installationId = jsonBody.get("installation").get("id").asText();
                    
                    Optional<Course> course = courseRepository.findByInstallationId(installationId);
                    if(course.isPresent()) {
                        // Find student by email directly
                        List<RosterStudent> studentsInCourse = rosterStudentRepository.findAllByEmail(jsonBody.get("invitation").get("email").asText());
                        Optional<RosterStudent> studentInCourse = studentsInCourse.stream()
                            .filter(s -> s.getCourse().getId().equals(course.get().getId()))
                            .findFirst();
                            
                            if(studentInCourse.isPresent()) {
                                RosterStudent updatedStudent = studentInCourse.get();
                                updatedStudent.setOrgStatus(OrgStatus.INVITED);
                                rosterStudentRepository.save(updatedStudent);
                                return ResponseEntity.ok(updatedStudent.toString());
                            } else {
                                log.info("Email {} was invited to course {}, but no matching roster student was found", 
                                         jsonBody.get("invitation").get("email").asText(), course.get().getCourseName());
                                return ResponseEntity.ok().body("success");
                            }
                    }
                    return ResponseEntity.ok().body("success");
                } else {
                    // For email invites, we don't have a GitHub login yet
                    // We'll use the email to find the student
                    String email = jsonBody.get("invitation").get("email").asText();
                    log.info("Processing invitation by email: {}", email);
                    String installationId = jsonBody.get("installation").get("id").asText();
                    
                    Optional<Course> course = courseRepository.findByInstallationId(installationId);
                    if(course.isPresent()) {
                        // Find student by email directly
                        List<RosterStudent> studentsInCourse = rosterStudentRepository.findAllByEmail(email);
                        Optional<RosterStudent> studentInCourse = studentsInCourse.stream()
                            .filter(s -> s.getCourse().getId().equals(course.get().getId()))
                            .findFirst();
                            
                            if(studentInCourse.isPresent()) {
                                RosterStudent updatedStudent = studentInCourse.get();
                                updatedStudent.setOrgStatus(OrgStatus.INVITED);
                                rosterStudentRepository.save(updatedStudent);
                                return ResponseEntity.ok(updatedStudent.toString());
                            } else {
                                log.info("Email {} was invited to course {}, but no matching roster student was found", 
                                         email, course.get().getCourseName());
                                return ResponseEntity.ok().body("success");
                            }
                    }
                    return ResponseEntity.ok().body("success");
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
