package edu.ucsb.cs156.frontiers.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ucsb.cs156.frontiers.entities.Instructor;
import edu.ucsb.cs156.frontiers.repositories.InstructorRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * This is a REST controller for getting information about the instructors.
 * These endpoints are only accessible to instructors with the role
 * "ROLE_ADMIN".
 */

@Tag(name = "Instructors")
@RequestMapping("/api/admin/instructors")
@RestController
@Slf4j
public class InstructorsController extends ApiController {
    @Autowired
    InstructorRepository instructorRepository;

    @Autowired
    ObjectMapper mapper;

    /**
     * Create a new Instructor.
     * 
     * @param email the email of the instructor
     * @return the created Instructor
     */
    @Operation(summary = "Create a new Instructor")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/post")
    public Instructor postInstructor(
            @RequestParam String email) {
        Instructor instructor = Instructor.builder()
                .email(email)
                .build();
        instructorRepository.save(instructor);
        return instructor;
    }

    /**
     * This method returns a list of all instructors, available only to Admins.
     * 
     * @return a list of all instructors
     */
    @Operation(summary = "List all Instructors")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/get")
    public Iterable<Instructor> allInstructors() {
        Iterable<Instructor> instructors = instructorRepository.findAll();
        return instructors;
    }
}
