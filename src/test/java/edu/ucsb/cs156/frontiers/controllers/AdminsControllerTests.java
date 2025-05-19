package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;

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

import java.time.LocalDateTime;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = AdminsController.class)
@Import(TestConfig.class)
public class AdminsControllerTests extends ControllerTestCase {

        @MockBean
        AdminRepository adminRepository;

        @MockBean
        UserRepository userRepository;

        // Authorization tests for post

       @Test
       public void logged_out_users_cannot_post() throws Exception {
               mockMvc.perform(post("/api/admin/post"))
                               .andExpect(status().is(403));
       }

       @WithMockUser(roles = { "USER" })
       @Test
       public void logged_in_regular_users_cannot_post() throws Exception {
               mockMvc.perform(post("/api/admin/post"))
                               .andExpect(status().is(403)); // only admins can post
       }

       // Functionality tests

       @WithMockUser(roles = { "ADMIN", "USER" })
       @Test
       public void an_admin_user_can_post_a_new_admin() throws Exception {


               Admin admin = Admin.builder()
                               .adminEmail("acdamstedt@ucsb.edu")
                               .build();


               when(adminRepository.save(eq(admin))).thenReturn(admin);


               // act
               MvcResult response = mockMvc.perform(
                               post("/api/admin/post?adminEmail=acdamstedt@ucsb.edu")
                                               .with(csrf()))
                               .andExpect(status().isOk()).andReturn();


               // assert
               verify(adminRepository, times(1)).save(admin);
               String expectedJson = mapper.writeValueAsString(admin);
               String responseString = response.getResponse().getContentAsString();
               assertEquals(expectedJson, responseString);
       }
}

