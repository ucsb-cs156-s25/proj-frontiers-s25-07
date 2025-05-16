package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.entities.Instructor;
import edu.ucsb.cs156.frontiers.repositories.InstructorRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = InstructorsController.class)
@Import(TestConfig.class)
public class InstructorsControllerTests extends ControllerTestCase {

    @MockBean
    InstructorRepository instructorRepository;
    @MockBean
    UserRepository userRepository;

    // Tests for the POST endpoint
    @Test
    public void logged_out_users_cannot_post() throws Exception {
        mockMvc.perform(post("/api/admin/instructors/post"))
                .andExpect(status().is(403)); // logged out users cannot post
    }

    @WithMockUser(roles = { "USER" })
    @Test
    public void logged_in_users_cannot_post() throws Exception {
        mockMvc.perform(post("/api/admin/instructors/post"))
                .andExpect(status().is(403)); // logged in users cannot post
    }

    @WithMockUser(roles = { "ADMIN" })
    @Test
    public void logged_in_admins_can_post() throws Exception {
        // arrage
        Instructor instructor = Instructor.builder()
                .email("ins@ucsb.edu")
                .build();
        when(instructorRepository.findAll()).thenReturn(new ArrayList<>(Arrays.asList(instructor)));

        // act
        MvcResult response = mockMvc.perform(post("/api/admin/instructors/post?email=ins@ucsb.edu").with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        // assert
        verify(instructorRepository, times(1)).save(eq(instructor));
        String expectedJson = mapper.writeValueAsString(instructor);
        String responseString = response.getResponse().getContentAsString();
        assertEquals(expectedJson, responseString);
    }

    // Tests for the GET endpoint
    @Test
    public void logged_out_users_cannot_get() throws Exception {
        mockMvc.perform(get("/api/admin/instructors/get"))
                .andExpect(status().is(403)); // logged out users cannot get
    }

    @WithMockUser(roles = { "USER" })
    @Test
    public void logged_in_users_cannot_get() throws Exception {
        mockMvc.perform(get("/api/admin/instructors/get"))
                .andExpect(status().is(403)); // logged in users cannot get
    }

    @WithMockUser(roles = { "ADMIN" })
    @Test
    public void logged_in_admins_can_get() throws Exception {
        // arrage
        Instructor instructor = Instructor.builder()
                .email("ins@ucsb.edu")
                .build();
        ArrayList<Instructor> expectedInstructors = new ArrayList<>();
        expectedInstructors.addAll(Arrays.asList(instructor));
        when(instructorRepository.findAll()).thenReturn(new ArrayList<>(Arrays.asList(instructor)));

        // act
        MvcResult response = mockMvc.perform(get("/api/admin/instructors/get"))
                .andExpect(status().isOk())
                .andReturn();

        // assert
        verify(instructorRepository, times(1)).findAll();
        String expectedJson = mapper.writeValueAsString(expectedInstructors);
        String responseString = response.getResponse().getContentAsString();
        assertEquals(expectedJson, responseString);
    }
}
