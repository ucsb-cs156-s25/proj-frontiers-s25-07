package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.time.LocalDateTime;

/**
 * This is a REST controller for Admin
 */

 @Tag(name = "Admin")
 @RequestMapping("/api/admin")
 @RestController
 @Slf4j

 public class AdminsController extends ApiController {

    @Autowired
    AdminRepository adminRepository;

    /**
     * List all admins
     * 
     * @return an iterable of Admin
     */
    @Operation(summary= "List all admins")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/all")
    public Iterable<Admin> allAdmins() {
        Iterable<Admin> admins = adminRepository.findAll();
        return admins;
    }

    /**
     * Create a new admin

     * @param adminEmail       the email in typical email format
     * @return the saved admin
     */
    @Operation(summary= "Create a new admin")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/post")
    public Admin postAdmin(
            @Parameter(name="adminEmail") @RequestParam String adminEmail)
        {

        Admin admin = new Admin();

        Admin savedAdmin = adminRepository.save(admin);

        return savedAdmin;
    }

    /**
     * Delete an Admin
     * 
     * @param id the id of the admin to delete
     * @return a message indicating the admin was deleted
     */
    @Operation(summary= "Delete an Admin")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("")
    public Object deleteAdmin(
            @Parameter(name="id") @RequestParam Long id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Admin.class, id));
        // if (".env".contains(admin.adminEmail)) {
        //     throw java.lang.UnsupportedOperationException;
        // } else {
        //     adminRepository.delete(admin);
        // }
        adminRepository.delete(admin);
        return genericMessage("Admin with id %s deleted".formatted(id));
    }
}
