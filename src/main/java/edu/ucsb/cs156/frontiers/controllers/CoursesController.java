package edu.ucsb.cs156.frontiers.controllers;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.errors.InvalidInstallationTypeException;
import edu.ucsb.cs156.frontiers.models.CurrentUser;
import edu.ucsb.cs156.frontiers.models.RosterStudentDTO;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.OrganizationLinkerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Course")
@RequestMapping("/api/courses")
@RestController
@Slf4j
public class CoursesController extends ApiController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RosterStudentRepository rosterStudentRepository;

    @Autowired
    private OrganizationLinkerService linkerService;

    /**
     * This method creates a new Course.
     * 
     * @param orgName    the name of the organization
     * @param courseName the name of the course
     * @param term       the term of the course
     * @param school     the school of the course
     * @return the created course
     */

    @Operation(summary = "Create a new course")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/post")
    public Course postCourse(
            @Parameter(name = "orgName") @RequestParam String orgName,
            @Parameter(name = "courseName") @RequestParam String courseName,
            @Parameter(name = "term") @RequestParam String term,
            @Parameter(name = "school") @RequestParam String school) {
        // get current date right now and set status to pending
        CurrentUser currentUser = getCurrentUser();
        Course course = Course.builder()
                .orgName(orgName)
                .courseName(courseName)
                .term(term)
                .school(school)
                .creator(currentUser.getUser())
                .build();
        Course savedCourse = courseRepository.save(course);

        return savedCourse;
    }

    /**
     * This method returns a list of courses.
     * 
     * @return a list of all courses.
     */
    @Operation(summary = "List all courses")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/all")
    public Iterable<Course> allCourses() {
        Iterable<Course> courses = courseRepository.findAll();
        return courses;
    }

    /**
     * <p>
     * This is the outgoing method, redirecting from Frontiers to GitHub to allow a
     * Course to be linked to a GitHub Organization.
     * It redirects from Frontiers to the GitHub app installation process, and will
     * return with the {@link #addInstallation(Optional, String, String, Long)
     * addInstallation()} endpoint
     * </p>
     * 
     * @param courseId id of the course to be linked to
     * @return dynamically loaded url to install Frontiers to a Github Organization,
     *         with the courseId marked as the state parameter, which GitHub will
     *         return.
     *
     */
    @Operation(summary = "Authorize Frontiers to a Github Course")
    @PreAuthorize("hasRole('ROLE_PROFESSOR')")
    @GetMapping("/redirect")
    public ResponseEntity<Void> linkCourse(@Parameter Long courseId)
            throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeySpecException {
        String newUrl = linkerService.getRedirectUrl();
        newUrl += "/installations/new?state=" + courseId;
        // found this convenient solution here:
        // https://stackoverflow.com/questions/29085295/spring-mvc-restcontroller-and-redirect
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).header(HttpHeaders.LOCATION, newUrl).build();
    }

    /**
     *
     * @param installation_id id of the incoming GitHub Organization installation
     * @param setup_action    whether the permissions are installed or updated.
     *                        Required RequestParam but not used by the method.
     * @param code            token to be exchanged with GitHub to ensure the
     *                        request is legitimate and not spoofed.
     * @param state           id of the Course to be linked with the GitHub
     *                        installation.
     * @return ResponseEntity, returning /success if the course was successfully
     *         linked or /noperms if the user does not have the permission to
     *         install the application on GitHub. Alternately returns 403 Forbidden
     *         if the user is not the creator.
     */
    @Operation(summary = "Link a Course to a Github Course")
    @PreAuthorize("hasRole('ROLE_PROFESSOR')")
    @GetMapping("link")
    public ResponseEntity<Void> addInstallation(
            @Parameter(name = "installationId") @RequestParam Optional<String> installation_id,
            @Parameter(name = "setupAction") @RequestParam String setup_action,
            @Parameter(name = "code") @RequestParam String code,
            @Parameter(name = "state") @RequestParam Long state)
            throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
        if (installation_id.isEmpty()) {
            return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                    .header(HttpHeaders.LOCATION, "/courses/nopermissions").build();
        } else {
            Course course = courseRepository.findById(state)
                    .orElseThrow(() -> new EntityNotFoundException(Course.class, state));
            if (!(course.getCreator().getId() == getCurrentUser().getUser().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            } else {
                String orgName = linkerService.getOrgName(installation_id.get());
                course.setInstallationId(installation_id.get());
                course.setOrgName(orgName);
                courseRepository.save(course);
                return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                        .header(HttpHeaders.LOCATION, "/admin/courses?success=True&course=" + state).build();
            }
        }
    }

    /**
     * This method handles the InvalidInstallationTypeException.
     * 
     * @param e the exception
     * @return a map with the type and message of the exception
     */
    @ExceptionHandler({ InvalidInstallationTypeException.class })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Object handleInvalidInstallationType(Throwable e) {
        return Map.of(
                "type", e.getClass().getSimpleName(),
                "message", e.getMessage());
    }

    /**
     * This method returns a list of courses that the current user is enrolled.
     * 
     * @return a list of courses in the DTO form along with the student status in the organization.
     */
    @Operation(summary = "List all courses for the current student, including their org status")
    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/list")
    public List<Map<String, Object>> listCoursesForCurrentUser() {
        String email = getCurrentUser().getUser().getEmail();
        Iterable<RosterStudent> rosterStudentsIterable = rosterStudentRepository.findAllByEmail(email);
        List<RosterStudent> rosterStudents = new ArrayList<>();
        rosterStudentsIterable.forEach(rosterStudents::add);
        return rosterStudents.stream()
                .map(rs -> {
                    Course course = rs.getCourse();
                    RosterStudentDTO rsDto = RosterStudentDTO.from(rs);
                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("id", course.getId());
                    response.put("installationId", course.getInstallationId());
                    response.put("orgName", course.getOrgName());
                    response.put("courseName", course.getCourseName());
                    response.put("term", course.getTerm());
                    response.put("school", course.getSchool());
                    response.put("studentStatus", rsDto.getOrgStatus());
                    return response;
                })
                .collect(Collectors.toList());
    }

}
