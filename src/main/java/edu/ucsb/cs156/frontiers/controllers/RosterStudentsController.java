package edu.ucsb.cs156.frontiers.controllers;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.errors.NoLinkedOrganizationException;
import edu.ucsb.cs156.frontiers.jobs.UpdateOrgMembershipJob;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.*;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.core.JsonProcessingException;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

@Tag(name = "RosterStudents")
@RequestMapping("/api/rosterstudents")
@RestController
@Slf4j
public class RosterStudentsController extends ApiController {

    @Autowired
    private JobService jobService;
    @Autowired
    private OrganizationMemberService organizationMemberService;

    public enum InsertStatus {
        INSERTED, UPDATED
    };

    @Autowired
    private RosterStudentRepository rosterStudentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UpdateUserService updateUserService;

    @Autowired
    private CurrentUserService currentUserService;

    /**
     * This method creates a new RosterStudent.
     * 
     * 
     * @return the created RosterStudent
     */

    @Operation(summary = "Create a new roster student")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/post")
    public RosterStudent postRosterStudent(
            @Parameter(name = "studentId") @RequestParam String studentId,
            @Parameter(name = "firstName") @RequestParam String firstName,
            @Parameter(name = "lastName") @RequestParam String lastName,
            @Parameter(name = "email") @RequestParam String email,
            @Parameter(name = "courseId") @RequestParam Long courseId) throws EntityNotFoundException {

        // Get Course or else throw an error

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

        RosterStudent rosterStudent = RosterStudent.builder()
                .studentId(studentId)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .course(course)
                .rosterStatus(RosterStatus.MANUAL)
                .orgStatus(OrgStatus.NONE)
                .build();
        RosterStudent savedRosterStudent = rosterStudentRepository.save(rosterStudent);

        return savedRosterStudent;
    }

    /**
     * This method returns a list of roster students for a given course.
     * 
     * @return a list of all courses.
     */
    @Operation(summary = "List all roster students for a course")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/course")
    public Iterable<RosterStudent> rosterStudentForCourse(
            @Parameter(name = "courseId") @RequestParam Long courseId) throws EntityNotFoundException {
        courseRepository.findById(courseId).orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
        Iterable<RosterStudent> rosterStudents = rosterStudentRepository.findByCourseId(courseId);
        return rosterStudents;
    }

    @Operation(summary = "Upload Roster students for Course in UCSB Egrades Format")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping(value = "/upload/egrades", consumes = { "multipart/form-data" })
    public Map<String, String> uploadRosterStudents(
            @Parameter(name = "courseId") @RequestParam Long courseId,
            @Parameter(name = "file") @RequestParam("file") MultipartFile file)
            throws JsonProcessingException, IOException, CsvException {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId.toString()));

        int counts[] = { 0, 0 };

        try (InputStream inputStream = new BufferedInputStream(file.getInputStream());
                InputStreamReader reader = new InputStreamReader(inputStream);
                CSVReader csvReader = new CSVReader(reader);) {
            csvReader.skip(2);
            List<String[]> myEntries = csvReader.readAll();
            for (String[] row : myEntries) {
                RosterStudent rosterStudent = fromEgradesCSVRow(row);
                InsertStatus s = upsertStudent(rosterStudent, course);
                counts[s.ordinal()]++;
            }
        }
        return Map.of(
                "filename", file.getOriginalFilename(),
                "message", String.format("Inserted %d new students, Updated %d students",
                        counts[InsertStatus.INSERTED.ordinal()], counts[InsertStatus.UPDATED.ordinal()]));

    }

    public RosterStudent fromEgradesCSVRow(String[] row) {
        return RosterStudent.builder()
                .firstName(row[5])
                .lastName(row[4])
                .studentId(row[1])
                .email(row[10])
                .build();
    }

    public InsertStatus upsertStudent(RosterStudent student, Course course) {
        Optional<RosterStudent> existingStudent = rosterStudentRepository.findByCourseIdAndStudentId(course.getId(),
                student.getStudentId());
        String convertedEmail = student.getEmail().replace("@umail.ucsb.edu","@ucsb.edu");
        if (existingStudent.isPresent()) {
            RosterStudent existingStudentObj = existingStudent.get();
            existingStudentObj.setRosterStatus(RosterStatus.ROSTER);
            existingStudentObj.setFirstName(student.getFirstName());
            existingStudentObj.setLastName(student.getLastName());
            if (!existingStudentObj.getEmail().equals(convertedEmail)) {
                existingStudentObj.setEmail(convertedEmail);
            }
            existingStudentObj = rosterStudentRepository.save(existingStudentObj);
            updateUserService.attachUserToRosterStudent(existingStudentObj);
            return InsertStatus.UPDATED;
        } else {
            student.setCourse(course);
            student.setEmail(convertedEmail);
            student.setRosterStatus(RosterStatus.ROSTER);
            student.setOrgStatus(OrgStatus.NONE);
            student = rosterStudentRepository.save(student);
            updateUserService.attachUserToRosterStudent(student);
            return InsertStatus.INSERTED;
        }
    }

    @PreAuthorize("hasRole('ROLE_PROFESSOR')")
    @PostMapping("/updateCourseMembership")
    public Job updateCourseMembership(@Parameter(name = "courseId", description = "Course ID") @RequestParam Long courseId) throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
        if(course.getInstallationId() == null || course.getOrgName() == null){
            throw new NoLinkedOrganizationException(course.getCourseName());
        }else{
            UpdateOrgMembershipJob job = UpdateOrgMembershipJob.builder()
                    .rosterStudentRepository(rosterStudentRepository)
                    .organizationMemberService(organizationMemberService)
                    .course(course)
                    .build();

            return jobService.runAsJob(job);
        }
    }
    
    @Operation(summary = "Link a Roster Student to a Github Account")
    @PreAuthorize("hasRole('ROLE_USER')")
    @PutMapping("/linkGitHub")
    public ResponseEntity<String> linkGitHub(@Parameter(name = "rosterStudentId", description = "Roster Student to be linked to") @RequestParam Long rosterStudentId){
        User currentUser = currentUserService.getUser();
        RosterStudent rosterStudent = rosterStudentRepository.findById(rosterStudentId)
                .orElseThrow(() -> new EntityNotFoundException(RosterStudent.class, rosterStudentId));

        if (currentUser.getId() != rosterStudent.getUser().getId()) {
            throw new AccessDeniedException("User not authorized to link this roster student");
        }

        if (rosterStudent.getGithubId() != 0 && rosterStudent.getGithubLogin() != null ) {
            return ResponseEntity.badRequest().body("This roster student is already linked to a GitHub account");
        }

        rosterStudent.setGithubId(currentUser.getGithubId());
        rosterStudent.setGithubLogin(currentUser.getGithubLogin());
        rosterStudentRepository.save(rosterStudent);
        return ResponseEntity.ok("Successfully linked GitHub account to roster student");
    }

    @Operation(summary = "Get Associated Roster Students with a User")
    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/associatedRosterStudents")
    public Iterable<RosterStudent> getAssociatedRosterStudents(){
        User currentUser = currentUserService.getUser();
        Iterable<RosterStudent> rosterStudents = rosterStudentRepository.findAllByUser((currentUser));
        return rosterStudents;
    }

    @Operation(summary = "Update a roster student")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/update")
    public RosterStudent updateRosterStudent(
            @Parameter(name = "id") @RequestParam Long id,
            @Parameter(name = "firstName") @RequestParam(required = false) String firstName,
            @Parameter(name = "lastName") @RequestParam(required = false) String lastName,
            @Parameter(name = "studentId") @RequestParam(required = false) String studentId) throws EntityNotFoundException {
        
        if(firstName == null || lastName == null || studentId == null ||
            firstName.trim().isEmpty() || lastName.trim().isEmpty() || studentId.trim().isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required fields cannot be empty");
        }

        RosterStudent rosterStudent = rosterStudentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(RosterStudent.class, id));

        if(!rosterStudent.getStudentId().trim().equals(studentId.trim())){
            Optional<RosterStudent> existingStudent = rosterStudentRepository.findByCourseIdAndStudentId(
                    rosterStudent.getCourse().getId(), studentId.trim());
            if (existingStudent.isPresent()){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student ID already exists in this course");
            }
        }

        rosterStudent.setFirstName(firstName.trim());
        rosterStudent.setLastName(lastName.trim());
        rosterStudent.setStudentId(studentId.trim());

        return rosterStudentRepository.save(rosterStudent);
    }

    @Operation(summary = "Update a student's organization status")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/updateOrgStatus")
    public RosterStudent updateOrgStatus(
            @Parameter(name = "id") @RequestParam Long id,
            @Parameter(name = "orgStatus") @RequestParam OrgStatus orgStatus) throws EntityNotFoundException {
        
        RosterStudent rosterStudent = rosterStudentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(RosterStudent.class, id));
        
        rosterStudent.setOrgStatus(orgStatus);
        return rosterStudentRepository.save(rosterStudent);
    }
}
