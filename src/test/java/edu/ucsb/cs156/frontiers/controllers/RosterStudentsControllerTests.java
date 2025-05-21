package edu.ucsb.cs156.frontiers.controllers;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.jobs.UpdateOrgMembershipJob;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import lombok.extern.slf4j.Slf4j;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.springframework.http.MediaType;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@Slf4j
@WebMvcTest(controllers = {RosterStudentsController.class})
@AutoConfigureDataJpa
public class RosterStudentsControllerTests extends ControllerTestCase {

        @MockitoBean
        private CourseRepository courseRepository;

        @MockitoBean
        private RosterStudentRepository rosterStudentRepository;

        @Autowired
        private CurrentUserService currentUserService;

        @MockitoBean
        private UpdateUserService updateUserService;

        @MockitoBean
        private OrganizationMemberService organizationMemberService;

        @MockitoBean
        private JobService service;

        @Autowired
        private ObjectMapper objectMapper;

        Course course1 = Course.builder()
                        .id(1L)
                        .courseName("CS156")
                        .orgName("ucsb-cs156-s25")
                        .term("S25")
                        .school("UCSB")
                        .build();

        RosterStudent rs1 = RosterStudent.builder()
                        .firstName("Chris")
                        .lastName("Gaucho")
                        .studentId("A123456")
                        .email("cgaucho@example.org")
                        .course(course1)
                        .rosterStatus(RosterStatus.MANUAL)
                        .orgStatus(OrgStatus.NONE)
                        .build();

        RosterStudent rs2 = RosterStudent.builder()
                        .id(2L)
                        .firstName("Lauren")
                        .lastName("Del Playa")
                        .studentId("A987654")
                        .email("ldelplaya@ucsb.edu")
                        .course(course1)
                        .rosterStatus(RosterStatus.ROSTER)
                        .orgStatus(OrgStatus.NONE)
                        .build();

        /**
         * Test the POST endpoint
         */
        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testPostRosterStudent() throws Exception {

                when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));
                when(rosterStudentRepository.save(any(RosterStudent.class))).thenReturn(rs1);

                // act

                MvcResult response = mockMvc.perform(post("/api/rosterstudents/post")
                                .with(csrf())
                                .param("studentId", "A123456")
                                .param("firstName", "Chris")
                                .param("lastName", "Gaucho")
                                .param("email", "cgaucho@example.org")
                                .param("courseId", "1"))
                                .andExpect(status().isOk())
                                .andReturn();

                // assert

                verify(courseRepository, times(1)).findById(eq(1L));
                verify(rosterStudentRepository, times(1)).save(eq(rs1));

                String responseString = response.getResponse().getContentAsString();
                String expectedJson = mapper.writeValueAsString(rs1);
                assertEquals(expectedJson, responseString);

        }

        /**
         * Test that you cannot post a single roster student for a course that does not
         * exist
         * 
         * @throws Exception
         */

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void test_AdminCannotPostRosterStudentForCourseThatDoesNotExist() throws Exception {
                when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

                // act

                MvcResult response = mockMvc.perform(post("/api/rosterstudents/post")
                                .with(csrf())
                                .param("studentId", "A123456")
                                .param("firstName", "Chris")
                                .param("lastName", "Gaucho")
                                .param("email", "cgaucho@example.org")
                                .param("courseId", "1"))
                                .andExpect(status().isNotFound())
                                .andReturn();

                // assert

                String responseString = response.getResponse().getContentAsString();
                Map<String, String> expectedMap = Map.of(
                                "type", "EntityNotFoundException",
                                "message", "Course with id 1 not found");
                String expectedJson = mapper.writeValueAsString(expectedMap);
                assertEquals(expectedJson, responseString);

        }

        /**
         * Test the GET endpoint
         */

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testRosterStudentsByCourse() throws Exception {

                // arrange

                when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));
                when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(java.util.List.of(rs1, rs2));

                // act

                MvcResult response = mockMvc.perform(get("/api/rosterstudents/course")
                                .param("courseId", "1"))
                                .andExpect(status().isOk())
                                .andReturn();

                // assert

                String responseString = response.getResponse().getContentAsString();
                String expectedJson = mapper.writeValueAsString(java.util.List.of(rs1, rs2));
                assertEquals(expectedJson, responseString);
        }

        /** Test whether admin can get roster students for a non existing course */

        @WithMockUser(roles = { "ADMIN" })
        @Test
        public void admin_can_get_roster_students_for_a_non_existing_course() throws Exception {

                // arrange

                when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

                // act

                MvcResult response = mockMvc.perform(get("/api/rosterstudents/course")
                                .param("courseId", "1"))
                                .andExpect(status().isNotFound())
                                .andReturn();

                // assert

                verify(courseRepository, atLeastOnce()).findById(eq(1L));
                String responseString = response.getResponse().getContentAsString();
                Map<String, String> expectedMap = Map.of(
                                "type", "EntityNotFoundException",
                                "message", "Course with id 1 not found");
                String expectedJson = mapper.writeValueAsString(expectedMap);
                assertEquals(expectedJson, responseString);

        }

        /** Test whether admin can upload students */

        private final String sampleCSVContents = """
                        Enrl Cd,Perm #,Grade,Final Units,Student Last,Student First Middle,Quarter,Course ID,Section,Meeting Time(s) / Location(s),Email,ClassLevel,Major1,Major2,Date/Time,Pronoun

                        08235,A123456,,4.0,GAUCHO,CHRIS FAKE,F23,CMPSC156,0100,T R   2:00- 3:15 SH 1431     W    5:00- 5:50 PHELP 3525  W    6:00- 6:50 PHELP 3525  W    7:00- 7:50 PHELP 3525  ,cgaucho@ucsb.edu,SR,CMPSC,,9/27/2023 9:39:25 AM,
                        08250,A987654,,4.0,DEL PLAYA,LAUREN,F23,CMPSC156,0100,T R   2:00- 3:15 SH 1431     W    5:00- 5:50 PHELP 3525  W    6:00- 6:50 PHELP 3525  W    7:00- 7:50 PHELP 3525  ,ldelplaya@umail.ucsb.edu,SR,CMPSC,,9/27/2023 9:39:25 AM,She (She/Her/Hers)
                        08243,1234567,,4.0,TARDE,SABADO,F23,CMPSC156,0100,T R   2:00- 3:15 SH 1431     W    5:00- 5:50 PHELP 3525  W    6:00- 6:50 PHELP 3525  W    7:00- 7:50 PHELP 3525  ,sabadotarde@umail.ucsb.edu,SR,CMPSC,,9/27/2023 9:39:25 AM,He (He/Him/His)
                        """;

        @WithMockUser(roles = { "ADMIN" })
        @Test
        public void admin_can_upload_students_for_an_existing_course() throws Exception {

                // arrange

                RosterStudent rs1BeforeWithId = RosterStudent.builder()
                                .id(1L)
                                .firstName("Chris")
                                .lastName("Gaucho")
                                .studentId("A123456")
                                .email("cgaucho@ucsb.edu")
                                .course(course1)
                                .rosterStatus(RosterStatus.MANUAL)
                                .orgStatus(OrgStatus.NONE)
                                .build();

                RosterStudent rs1AfterWithId = RosterStudent.builder()
                                .id(1L)
                                .firstName("CHRIS FAKE")
                                .lastName("GAUCHO")
                                .studentId("A123456")
                                .email("cgaucho@ucsb.edu")
                                .course(course1)
                                .rosterStatus(RosterStatus.ROSTER)
                                .orgStatus(OrgStatus.NONE)
                                .build();

                RosterStudent rs2BeforeWithId = RosterStudent.builder()
                                .id(2L)
                                .firstName("Lauren")
                                .lastName("Del Playa")
                                .studentId("A987654")
                                .email("ldelplaya@umail.ucsb.edu")
                                .course(course1)
                                .rosterStatus(RosterStatus.ROSTER)
                                .orgStatus(OrgStatus.NONE)
                                .build();

                RosterStudent rs2AfterWithId = RosterStudent.builder()
                                .id(2L)
                                .course(course1)
                                .firstName("LAUREN")
                                .lastName("DEL PLAYA")
                                .email("ldelplaya@ucsb.edu")
                                .studentId("A987654")
                                .rosterStatus(RosterStatus.ROSTER)
                                .orgStatus(OrgStatus.NONE)
                                .build();

                RosterStudent rs3NoId = RosterStudent.builder()
                                .course(course1)
                                .firstName("SABADO")
                                .lastName("TARDE")
                                .email("sabadotarde@ucsb.edu")
                                .studentId("1234567")
                                .rosterStatus(RosterStatus.ROSTER)
                                .orgStatus(OrgStatus.NONE)
                                .build();

                RosterStudent rs3WithId = RosterStudent.builder()
                                .id(3L)
                                .course(course1)
                                .firstName("SABADO")
                                .lastName("TARDE")
                                .email("sabadotarde@ucsb.edu")
                                .studentId("1234567")
                                .rosterStatus(RosterStatus.ROSTER)
                                .orgStatus(OrgStatus.NONE)
                                .build();

                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "egrades.csv",
                                MediaType.TEXT_PLAIN_VALUE,
                                sampleCSVContents.getBytes());

                when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));
                when(rosterStudentRepository.findByCourseIdAndStudentId(eq(1L), eq("A123456")))
                                .thenReturn(Optional.of(rs1BeforeWithId));
                when(rosterStudentRepository.findByCourseIdAndStudentId(eq(1L), eq("A987654")))
                                .thenReturn(Optional.of(rs2BeforeWithId));
                when(rosterStudentRepository.findByCourseIdAndStudentId(eq(1L), eq("1234567")))
                                .thenReturn(Optional.empty());

                when(rosterStudentRepository.save(eq(rs1AfterWithId))).thenReturn(rs1AfterWithId);
                when(rosterStudentRepository.save(eq(rs2AfterWithId))).thenReturn(rs2AfterWithId);
                when(rosterStudentRepository.save(eq(rs3NoId))).thenReturn(rs3WithId);

                doNothing().when(updateUserService).attachUserToRosterStudent(eq(rs1AfterWithId));               
                doNothing().when(updateUserService).attachUserToRosterStudent(eq(rs2AfterWithId));
                doNothing().when(updateUserService).attachUserToRosterStudent(eq(rs3WithId));

                // act

                MvcResult response = mockMvc
                                .perform(multipart("/api/rosterstudents/upload/egrades")
                                                .file(file)
                                                .param("courseId", "1")
                                                .with(csrf()))
                                .andExpect(status().isOk()).andReturn();

                // assert

                verify(courseRepository, atLeastOnce()).findById(eq(1L));
                verify(rosterStudentRepository, atLeastOnce()).findByCourseIdAndStudentId(eq(1L), eq("A123456"));
                verify(rosterStudentRepository, atLeastOnce()).findByCourseIdAndStudentId(eq(1L), eq("A987654"));
                verify(rosterStudentRepository, atLeastOnce()).findByCourseIdAndStudentId(eq(1L), eq("1234567"));
                verify(rosterStudentRepository, atLeastOnce()).save(eq(rs1AfterWithId));
                verify(rosterStudentRepository, atLeastOnce()).save(eq(rs2AfterWithId));
                verify(rosterStudentRepository, atLeastOnce()).save(eq(rs3NoId));

                verify(updateUserService, times(1)).attachUserToRosterStudent(eq(rs1AfterWithId));
                verify(updateUserService, times(1)).attachUserToRosterStudent(eq(rs2AfterWithId));
                verify(updateUserService, times(1)).attachUserToRosterStudent(eq(rs3WithId));

                String responseString = response.getResponse().getContentAsString();
                Map<String, String> expectedMap = Map.of(
                                "filename", "egrades.csv",
                                "message", "Inserted 1 new students, Updated 2 students");
                String expectedJson = mapper.writeValueAsString(expectedMap);
                assertEquals(expectedJson, responseString);

        }

        /** Test that you cannot upload a roster for a course that does not exist */

        @WithMockUser(roles = { "ADMIN" })
        @Test
        public void admin_cannot_upload_students_for_a_course_that_does_not_exist() throws Exception {

                // arrange

                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "egrades.csv",
                                MediaType.TEXT_PLAIN_VALUE,
                                sampleCSVContents.getBytes());

                when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

                // act

                MvcResult response = mockMvc
                                .perform(multipart("/api/rosterstudents/upload/egrades")
                                                .file(file)
                                                .param("courseId", "1")
                                                .with(csrf()))
                                .andExpect(status().isNotFound()).andReturn();

                // assert

                verify(courseRepository, atLeastOnce()).findById(eq(1L));
                String responseString = response.getResponse().getContentAsString();
                Map<String, String> expectedMap = Map.of(
                                "type", "EntityNotFoundException",
                                "message", "Course with id 1 not found");
                String expectedJson = mapper.writeValueAsString(expectedMap);
                assertEquals(expectedJson, responseString);
        }

        @Test
        @WithMockUser(roles = {"ADMIN"})
        public void just_no_org_name() throws Exception {
                Course course = Course.builder().courseName("course").installationId("1234").creator(currentUserService.getUser()).build();
                doReturn(Optional.of(course)).when(courseRepository).findById(eq(2L));
                MvcResult response = mockMvc.perform(post("/api/rosterstudents/updateCourseMembership")
                                .with(csrf())
                                .param("courseId", "2")
                        ).andExpect(status().isBadRequest())
                        .andReturn();
                Map<String, Object> json = responseToJson(response);
                assertEquals("NoLinkedOrganizationException", json.get("type"));
                assertEquals("No linked GitHub Organization to course. Please link a GitHub Organization first.", json.get("message"));
        }

        @Test
        @WithMockUser(roles = {"ADMIN"})
        public void not_registered_org() throws Exception {
                Course course = Course.builder().courseName("course").orgName("ucsb-cs156").creator(currentUserService.getUser()).build();
                doReturn(Optional.of(course)).when(courseRepository).findById(eq(2L));
                MvcResult response = mockMvc.perform(post("/api/rosterstudents/updateCourseMembership")
                                .with(csrf())
                                .param("courseId", "2")
                        ).andExpect(status().isBadRequest())
                        .andReturn();
                Map<String, Object> json = responseToJson(response);
                assertEquals("NoLinkedOrganizationException", json.get("type"));
                assertEquals("No linked GitHub Organization to course. Please link a GitHub Organization first.", json.get("message"));
        }

        @Test
        @WithMockUser(roles = {"ADMIN"})
        public void job_actually_fires() throws Exception {
                Course course = Course.builder().id(2L).orgName("ucsb-cs156").installationId("1234").courseName("course").creator(currentUserService.getUser()).build();
                doReturn(Optional.of(course)).when(courseRepository).findById(eq(2L));
                Job job = Job.builder().status("processing").build();
                doReturn(job).when(service).runAsJob(any(UpdateOrgMembershipJob.class));
                MvcResult response = mockMvc.perform(post("/api/rosterstudents/updateCourseMembership")
                                .with(csrf())
                                .param("courseId", "2")
                        ).andExpect(status().isOk())
                        .andReturn();

                String expectedJson = objectMapper.writeValueAsString(job);
                String actualJson = response.getResponse().getContentAsString();
                assertEquals(expectedJson, actualJson);
        }

        @Test
        @WithMockUser(roles = {"ADMIN"})
        public void notFound() throws Exception {
                doReturn(Optional.empty()).when(courseRepository).findById(eq(2L));
                MvcResult response = mockMvc.perform(post("/api/rosterstudents/updateCourseMembership")
                                .with(csrf())
                                .param("courseId", "2")
                        ).andExpect(status().isNotFound())
                        .andReturn();
                Map<String, Object> json = responseToJson(response);
                assertEquals("EntityNotFoundException", json.get("type"));
                assertEquals("Course with id 2 not found", json.get("message"));
        }

        /**
         * Tests for the linkGitHub endpoint
         */
        @Test
        @WithMockUser(roles = { "USER" })
        public void testLinkGitHub_success() throws Exception {
                // Arrange
                User currentUser = currentUserService.getUser();

                RosterStudent rosterStudent = RosterStudent.builder()
                        .id(3L)
                        .firstName("Test")
                        .lastName("User")
                        .studentId("A555555")
                        .email("testuser@ucsb.edu")
                        .course(course1)
                        .rosterStatus(RosterStatus.ROSTER)
                        .orgStatus(OrgStatus.NONE)
                        .githubId(0)  // Not linked yet
                        .githubLogin(null)  // Not linked yet
                        .user(currentUser)  // Current user owns this roster entry
                        .build();

                RosterStudent rosterStudentUpdated = RosterStudent.builder()
                        .id(3L)
                        .firstName("Test")
                        .lastName("User")
                        .studentId("A555555")
                        .email("testuser@ucsb.edu")
                        .course(course1)
                        .rosterStatus(RosterStatus.ROSTER)
                        .orgStatus(OrgStatus.NONE)
                        .githubId(currentUser.getGithubId())
                        .githubLogin(currentUser.getGithubLogin())
                        .user(currentUser)
                        .build();

                when(rosterStudentRepository.findById(eq(3L))).thenReturn(Optional.of(rosterStudent));

                // Act
                MvcResult response = mockMvc.perform(put("/api/rosterstudents/linkGitHub")
                                .with(csrf())
                                .param("rosterStudentId", "3"))
                        .andExpect(status().isOk())
                        .andReturn();

                when(rosterStudentRepository.save(eq(rosterStudentUpdated))).thenReturn(rosterStudentUpdated);
                // Assert
                verify(rosterStudentRepository).findById(eq(3L));

                // Verify the GitHub ID and login were set
                verify(rosterStudentRepository, times(1)).save(eq(rosterStudentUpdated));

                assertEquals("Successfully linked GitHub account to roster student", response.getResponse().getContentAsString());
        }

        @Test
        @WithMockUser(roles = { "USER" })
        public void testLinkGitHub_notFound() throws Exception {
                // Arrange
                when(rosterStudentRepository.findById(eq(99L))).thenReturn(Optional.empty());

                // Act
                MvcResult response = mockMvc.perform(put("/api/rosterstudents/linkGitHub")
                                .with(csrf())
                                .param("rosterStudentId", "99"))
                        .andExpect(status().isNotFound())
                        .andReturn();

                // Assert
                verify(rosterStudentRepository).findById(eq(99L));

                // Verify correct error response
                String responseString = response.getResponse().getContentAsString();
                Map<String, String> expectedMap = Map.of(
                        "type", "EntityNotFoundException",
                        "message", "RosterStudent with id 99 not found");
                String expectedJson = mapper.writeValueAsString(expectedMap);
                assertEquals(expectedJson, responseString);
        }

        @Test
        @WithMockUser(roles = { "USER" })
        public void testLinkGitHub_unauthorized() throws Exception {
                // Arrange
                User currentUser = currentUserService.getUser();

                User differentUser = User.builder()
                        .id(-1L)  // Different from current user
                        .build();

                RosterStudent rosterStudent = RosterStudent.builder()
                        .id(4L)
                        .firstName("Other")
                        .lastName("Student")
                        .studentId("A666666")
                        .email("otherstudent@ucsb.edu")
                        .course(course1)
                        .rosterStatus(RosterStatus.ROSTER)
                        .orgStatus(OrgStatus.NONE)
                        .user(differentUser)  // Belongs to a different user
                        .build();

                when(rosterStudentRepository.findById(eq(4L))).thenReturn(Optional.of(rosterStudent));

                // Act & Assert
                mockMvc.perform(put("/api/rosterstudents/linkGitHub")
                                .with(csrf())
                                .param("rosterStudentId", "4"))
                        .andExpect(status().isForbidden());

                // Verify nothing was saved
                verify(rosterStudentRepository, never()).save(any(RosterStudent.class));
        }

        @Test
        @WithMockUser(roles = { "USER" })
        public void testLinkGitHub_alreadyLinked() throws Exception {
                // Arrange
                User currentUser = currentUserService.getUser();

                RosterStudent rosterStudent = RosterStudent.builder()
                        .id(5L)
                        .firstName("Already")
                        .lastName("Linked")
                        .studentId("A777777")
                        .email("alreadylinked@ucsb.edu")
                        .course(course1)
                        .rosterStatus(RosterStatus.ROSTER)
                        .orgStatus(OrgStatus.NONE)
                        .githubId(98765)  // Already has a GitHub ID
                        .githubLogin("existinguser")  // Already has a GitHub login
                        .user(currentUser)  // Current user owns this roster entry
                        .build();

                when(rosterStudentRepository.findById(eq(5L))).thenReturn(Optional.of(rosterStudent));

                // Act
                MvcResult response = mockMvc.perform(put("/api/rosterstudents/linkGitHub")
                                .with(csrf())
                                .param("rosterStudentId", "5"))
                        .andExpect(status().isBadRequest())
                        .andReturn();

                // Assert
                verify(rosterStudentRepository).findById(eq(5L));
                verify(rosterStudentRepository, never()).save(any(RosterStudent.class));

                assertEquals("This roster student is already linked to a GitHub account", response.getResponse().getContentAsString());
        }

        @Test
        @WithMockUser(roles = { "USER" })
        public void testLinkGitHub_success_no_login_only() throws Exception {
                // Arrange
                User currentUser = currentUserService.getUser();

                RosterStudent rosterStudent = RosterStudent.builder()
                        .id(3L)
                        .firstName("Test")
                        .lastName("User")
                        .studentId("A555555")
                        .email("testuser@ucsb.edu")
                        .course(course1)
                        .rosterStatus(RosterStatus.ROSTER)
                        .orgStatus(OrgStatus.NONE)
                        .githubId(123456789)  // Not linked yet
                        .githubLogin(null)  // Not linked yet
                        .user(currentUser)  // Current user owns this roster entry
                        .build();

                RosterStudent rosterStudentUpdated = RosterStudent.builder()
                        .id(3L)
                        .firstName("Test")
                        .lastName("User")
                        .studentId("A555555")
                        .email("testuser@ucsb.edu")
                        .course(course1)
                        .rosterStatus(RosterStatus.ROSTER)
                        .orgStatus(OrgStatus.NONE)
                        .githubId(currentUser.getGithubId())
                        .githubLogin(currentUser.getGithubLogin())
                        .user(currentUser)
                        .build();

                when(rosterStudentRepository.findById(eq(3L))).thenReturn(Optional.of(rosterStudent));

                // Act
                MvcResult response = mockMvc.perform(put("/api/rosterstudents/linkGitHub")
                                .with(csrf())
                                .param("rosterStudentId", "3"))
                        .andExpect(status().isOk())
                        .andReturn();

                when(rosterStudentRepository.save(eq(rosterStudentUpdated))).thenReturn(rosterStudentUpdated);
                // Assert
                verify(rosterStudentRepository).findById(eq(3L));

                // Verify the GitHub ID and login were set
                verify(rosterStudentRepository, times(1)).save(eq(rosterStudentUpdated));

                assertEquals("Successfully linked GitHub account to roster student", response.getResponse().getContentAsString());
        }

        @Test
        @WithMockUser(roles = { "USER" })
        public void testGetAssociatedRosterStudents() throws Exception {
                // Arrange
                User currentUser = currentUserService.getUser();

                RosterStudent rs1WithUser = RosterStudent.builder()
                        .id(1L)
                        .firstName("Chris")
                        .lastName("Gaucho")
                        .studentId("A123456")
                        .email("cgaucho@example.org")
                        .course(course1)
                        .rosterStatus(RosterStatus.MANUAL)
                        .orgStatus(OrgStatus.NONE)
                        .user(currentUser)
                        .build();

                RosterStudent rs2WithUser = RosterStudent.builder()
                        .id(2L)
                        .firstName("Lauren")
                        .lastName("Del Playa")
                        .studentId("A987654")
                        .email("ldelplaya@ucsb.edu")
                        .course(course1)
                        .rosterStatus(RosterStatus.ROSTER)
                        .orgStatus(OrgStatus.NONE)
                        .user(currentUser)
                        .build();

                List<RosterStudent> expectedRosterStudents = List.of(rs1WithUser, rs2WithUser);

                when(rosterStudentRepository.findAllByUser(eq(currentUser))).thenReturn(expectedRosterStudents);

                // Act
                MvcResult response = mockMvc.perform(get("/api/rosterstudents/associatedRosterStudents")
                                .with(csrf()))
                        .andExpect(status().isOk())
                        .andReturn();

                // Assert
                verify(rosterStudentRepository, times(1)).findAllByUser(eq(currentUser));

                String responseString = response.getResponse().getContentAsString();
                String expectedJson = mapper.writeValueAsString(expectedRosterStudents);
                assertEquals(expectedJson, responseString);
        }

        @Test
        @WithMockUser(roles = { "USER" })
        public void testGetAssociatedRosterStudents_noStudentsFound() throws Exception {
                // Arrange
                User currentUser = currentUserService.getUser();

                when(rosterStudentRepository.findAllByUser(eq(currentUser))).thenReturn(List.of());

                // Act
                MvcResult response = mockMvc.perform(get("/api/rosterstudents/associatedRosterStudents")
                                .with(csrf()))
                        .andExpect(status().isOk())
                        .andReturn();

                // Assert
                verify(rosterStudentRepository, times(1)).findAllByUser(eq(currentUser));

                String responseString = response.getResponse().getContentAsString();
                String expectedJson = mapper.writeValueAsString(List.of());
                assertEquals(expectedJson, responseString);
        }

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testUpdateRosterStudent_emptyFields() throws Exception {
                MvcResult response = mockMvc.perform(put("/api/rosterstudents/update")
                        .with(csrf())
                        .param("id", "1")
                        .param("firstName", "   ")
                        .param("lastName", "   ")
                        .param("studentId", "   "))
                        .andExpect(status().isBadRequest())
                        .andReturn();

                verify(rosterStudentRepository, never()).findById(any());
                verify(rosterStudentRepository, never()).save(any(RosterStudent.class));

                String responseString = response.getResponse().getErrorMessage();
                assertEquals("Required fields cannot be empty", responseString);
        }

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testUpdateRosterStudent_success() throws Exception {
                RosterStudent existingStudent = RosterStudent.builder()
                        .id(1L)
                        .firstName("Old")
                        .lastName("OldName")
                        .studentId("A123456")
                        .email("old@ucsb.edu")
                        .course(course1)
                        .rosterStatus(RosterStatus.ROSTER)
                        .orgStatus(OrgStatus.NONE)
                        .build();

                RosterStudent updatedStudent = RosterStudent.builder()
                        .id(1L)
                        .firstName("New")
                        .lastName("NewName")
                        .studentId("A123456")
                        .email("old@ucsb.edu")
                        .course(course1)
                        .rosterStatus(RosterStatus.ROSTER)
                        .orgStatus(OrgStatus.NONE)
                        .build();

                when(rosterStudentRepository.findById(eq(1L))).thenReturn(Optional.of(existingStudent));
                when(rosterStudentRepository.save(any(RosterStudent.class))).thenReturn(updatedStudent);

                MvcResult response = mockMvc.perform(put("/api/rosterstudents/update")
                        .with(csrf())
                        .param("id", "1")
                        .param("firstName", "   New   ")
                        .param("lastName", "   NewName   ")
                        .param("studentId", "   A123456   "))
                        .andExpect(status().isOk())
                        .andReturn();

                ArgumentCaptor<RosterStudent> captor = ArgumentCaptor.forClass(RosterStudent.class);
                verify(rosterStudentRepository).save(captor.capture());
                RosterStudent saved = captor.getValue();
                assertEquals("New", saved.getFirstName());
                assertEquals("NewName", saved.getLastName());
                assertEquals("A123456", saved.getStudentId());

                String responseString = response.getResponse().getContentAsString();
                String expectedJson = mapper.writeValueAsString(updatedStudent);
                assertEquals(expectedJson, responseString);
        }

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testUpdateRosterStudent_duplicateStudentId() throws Exception {
                RosterStudent existingStudent = RosterStudent.builder()
                        .id(1L)
                        .firstName("Old")
                        .lastName("OldName")
                        .studentId("A123456")
                        .email("old@ucsb.edu")
                        .course(course1)
                        .rosterStatus(RosterStatus.ROSTER)
                        .orgStatus(OrgStatus.NONE)
                        .build();

                RosterStudent otherStudent = RosterStudent.builder()
                        .id(2L)
                        .firstName("Other")
                        .lastName("Student")
                        .studentId("A789012")
                        .email("other@ucsb.edu")
                        .course(course1)
                        .rosterStatus(RosterStatus.ROSTER)
                        .orgStatus(OrgStatus.NONE)
                        .build();

                when(rosterStudentRepository.findById(eq(1L))).thenReturn(Optional.of(existingStudent));
                when(rosterStudentRepository.findByCourseIdAndStudentId(eq(1L), eq("A789012")))
                        .thenReturn(Optional.of(otherStudent));

                MvcResult response = mockMvc.perform(put("/api/rosterstudents/update")
                        .with(csrf())
                        .param("id", "1")
                        .param("firstName", "New")
                        .param("lastName", "NewName")
                        .param("studentId", "A789012"))
                        .andExpect(status().isBadRequest())
                        .andReturn();

                verify(rosterStudentRepository).findById(eq(1L));
                verify(rosterStudentRepository).findByCourseIdAndStudentId(eq(1L), eq("A789012"));
                verify(rosterStudentRepository, never()).save(any(RosterStudent.class));

                String responseString = response.getResponse().getErrorMessage();
                assertEquals("Student ID already exists in this course", responseString);
        }

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testUpdateRosterStudent_notFound() throws Exception {
                when(rosterStudentRepository.findById(eq(99L))).thenReturn(Optional.empty());

                MvcResult response = mockMvc.perform(put("/api/rosterstudents/update")
                        .with(csrf())
                        .param("id", "99")
                        .param("firstName", "New")
                        .param("lastName", "Name")
                        .param("studentId", "A123456"))
                        .andExpect(status().isNotFound())
                        .andReturn();

                verify(rosterStudentRepository).findById(eq(99L));
                verify(rosterStudentRepository, never()).save(any(RosterStudent.class));

                String responseString = response.getResponse().getContentAsString();
                Map<String, String> expectedMap = Map.of(
                        "type", "EntityNotFoundException",
                        "message", "RosterStudent with id 99 not found");
                String expectedJson = mapper.writeValueAsString(expectedMap);
                assertEquals(expectedJson, responseString);
        }

        @Test
        @WithMockUser(roles = { "USER" })
        public void testUpdateRosterStudent_unauthorized() throws Exception {
                mockMvc.perform(put("/api/rosterstudents/update")
                        .with(csrf())
                        .param("id", "1")
                        .param("firstName", "New")
                        .param("lastName", "Name")
                        .param("studentId", "A123456"))
                        .andExpect(status().isForbidden());

                verify(rosterStudentRepository, never()).findById(any());
                verify(rosterStudentRepository, never()).save(any());
        }

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testUpdateRosterStudent_newStudentIdNotExists() throws Exception {
                RosterStudent existingStudent = RosterStudent.builder()
                        .id(1L)
                        .firstName("Old")
                        .lastName("OldName")
                        .studentId("A123456")
                        .email("old@ucsb.edu")
                        .course(course1)
                        .rosterStatus(RosterStatus.ROSTER)
                        .orgStatus(OrgStatus.NONE)
                        .build();

                RosterStudent updatedStudent = RosterStudent.builder()
                        .id(1L)
                        .firstName("New")
                        .lastName("NewName")
                        .studentId("A999999") 
                        .email("old@ucsb.edu")
                        .course(course1)
                        .rosterStatus(RosterStatus.ROSTER)
                        .orgStatus(OrgStatus.NONE)
                        .build();

                when(rosterStudentRepository.findById(eq(1L))).thenReturn(Optional.of(existingStudent));
                when(rosterStudentRepository.findByCourseIdAndStudentId(eq(1L), eq("A999999")))
                        .thenReturn(Optional.empty());
                when(rosterStudentRepository.save(any(RosterStudent.class))).thenReturn(updatedStudent);

                MvcResult response = mockMvc.perform(put("/api/rosterstudents/update")
                        .with(csrf())
                        .param("id", "1")
                        .param("firstName", "   New   ")
                        .param("lastName", "   NewName   ")
                        .param("studentId", "   A999999   "))
                        .andExpect(status().isOk())
                        .andReturn();

                ArgumentCaptor<RosterStudent> captor = ArgumentCaptor.forClass(RosterStudent.class);
                verify(rosterStudentRepository).save(captor.capture());
                RosterStudent saved = captor.getValue();
                assertEquals("New", saved.getFirstName());
                assertEquals("NewName", saved.getLastName());
                assertEquals("A999999", saved.getStudentId());

                String responseString = response.getResponse().getContentAsString();
                String expectedJson = mapper.writeValueAsString(updatedStudent);
                assertEquals(expectedJson, responseString);
        }

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testUpdateRosterStudent_sameStudentIdWithWhitespace() throws Exception {
                RosterStudent existingStudent = RosterStudent.builder()
                        .id(1L)
                        .firstName("Old")
                        .lastName("OldName")
                        .studentId("A123456")
                        .email("old@ucsb.edu")
                        .course(course1)
                        .rosterStatus(RosterStatus.ROSTER)
                        .orgStatus(OrgStatus.NONE)
                        .build();

                RosterStudent updatedStudent = RosterStudent.builder()
                        .id(1L)
                        .firstName("New")
                        .lastName("NewName")
                        .studentId("A123456")
                        .email("old@ucsb.edu")
                        .course(course1)
                        .rosterStatus(RosterStatus.ROSTER)
                        .orgStatus(OrgStatus.NONE)
                        .build();

                when(rosterStudentRepository.findById(eq(1L))).thenReturn(Optional.of(existingStudent));
                when(rosterStudentRepository.save(any(RosterStudent.class))).thenReturn(updatedStudent);

                MvcResult response = mockMvc.perform(put("/api/rosterstudents/update")
                        .with(csrf())
                        .param("id", "1")
                        .param("firstName", "  New  ")
                        .param("lastName", "  NewName  ")
                        .param("studentId", "  A123456  "))
                        .andExpect(status().isOk())
                        .andReturn();

                ArgumentCaptor<RosterStudent> captor = ArgumentCaptor.forClass(RosterStudent.class);
                verify(rosterStudentRepository).save(captor.capture());
                RosterStudent saved = captor.getValue();
                assertEquals("New", saved.getFirstName());
                assertEquals("NewName", saved.getLastName());
                assertEquals("A123456", saved.getStudentId());

                String responseString = response.getResponse().getContentAsString();
                String expectedJson = mapper.writeValueAsString(updatedStudent);
                assertEquals(expectedJson, responseString);
        }

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testUpdateRosterStudent_nullFields() throws Exception {
                MvcResult response = mockMvc.perform(put("/api/rosterstudents/update")
                        .with(csrf())
                        .param("id", "1")
                        .param("lastName", "Doe")
                        .param("studentId", "A123456"))
                        .andExpect(status().isBadRequest())
                        .andReturn();

                verify(rosterStudentRepository, never()).findById(any());
                verify(rosterStudentRepository, never()).save(any(RosterStudent.class));

                String responseString = response.getResponse().getErrorMessage();
                assertEquals("Required fields cannot be empty", responseString);
        }

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testUpdateRosterStudent_nullFirstName() throws Exception {
                MvcResult response = mockMvc.perform(put("/api/rosterstudents/update")
                        .with(csrf())
                        .param("id", "1")
                        .param("lastName", "Doe")
                        .param("studentId", "A123456"))
                        .andExpect(status().isBadRequest())
                        .andReturn();

                verify(rosterStudentRepository, never()).findById(any());
                verify(rosterStudentRepository, never()).save(any(RosterStudent.class));

                String responseString = response.getResponse().getErrorMessage();
                assertEquals("Required fields cannot be empty", responseString);
        }

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testUpdateRosterStudent_nullLastName() throws Exception {
                MvcResult response = mockMvc.perform(put("/api/rosterstudents/update")
                        .with(csrf())
                        .param("id", "1")
                        .param("firstName", "John")
                        .param("studentId", "A123456"))
                        .andExpect(status().isBadRequest())
                        .andReturn();

                verify(rosterStudentRepository, never()).findById(any());
                verify(rosterStudentRepository, never()).save(any(RosterStudent.class));

                String responseString = response.getResponse().getErrorMessage();
                assertEquals("Required fields cannot be empty", responseString);
        }

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testUpdateRosterStudent_nullStudentId() throws Exception {
                MvcResult response = mockMvc.perform(put("/api/rosterstudents/update")
                        .with(csrf())
                        .param("id", "1")
                        .param("firstName", "John")
                        .param("lastName", "Doe"))
                        .andExpect(status().isBadRequest())
                        .andReturn();

                verify(rosterStudentRepository, never()).findById(any());
                verify(rosterStudentRepository, never()).save(any(RosterStudent.class));

                String responseString = response.getResponse().getErrorMessage();
                assertEquals("Required fields cannot be empty", responseString);
        }

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testUpdateRosterStudent_emptyFirstName() throws Exception {
                MvcResult response = mockMvc.perform(put("/api/rosterstudents/update")
                        .with(csrf())
                        .param("id", "1")
                        .param("firstName", "")
                        .param("lastName", "Doe")
                        .param("studentId", "A123456"))
                        .andExpect(status().isBadRequest())
                        .andReturn();

                verify(rosterStudentRepository, never()).findById(any());
                verify(rosterStudentRepository, never()).save(any(RosterStudent.class));

                String responseString = response.getResponse().getErrorMessage();
                assertEquals("Required fields cannot be empty", responseString);
        }

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testUpdateRosterStudent_emptyLastName() throws Exception {
                MvcResult response = mockMvc.perform(put("/api/rosterstudents/update")
                        .with(csrf())
                        .param("id", "1")
                        .param("firstName", "John")
                        .param("lastName", "")
                        .param("studentId", "A123456"))
                        .andExpect(status().isBadRequest())
                        .andReturn();

                verify(rosterStudentRepository, never()).findById(any());
                verify(rosterStudentRepository, never()).save(any(RosterStudent.class));

                String responseString = response.getResponse().getErrorMessage();
                assertEquals("Required fields cannot be empty", responseString);
        }

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testUpdateRosterStudent_emptyStudentId() throws Exception {
                MvcResult response = mockMvc.perform(put("/api/rosterstudents/update")
                        .with(csrf())
                        .param("id", "1")
                        .param("firstName", "John")
                        .param("lastName", "Doe")
                        .param("studentId", ""))
                        .andExpect(status().isBadRequest())
                        .andReturn();

                verify(rosterStudentRepository, never()).findById(any());
                verify(rosterStudentRepository, never()).save(any(RosterStudent.class));

                String responseString = response.getResponse().getErrorMessage();
                assertEquals("Required fields cannot be empty", responseString);
        }

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testDeleteRosterStudent_success() throws Exception {
                RosterStudent rosterStudent = RosterStudent.builder()
                        .id(1L)
                        .firstName("Test")
                        .lastName("Student")
                        .studentId("A123456")
                        .email("test@ucsb.edu")
                        .course(course1)
                        .rosterStatus(RosterStatus.ROSTER)
                        .orgStatus(OrgStatus.NONE)
                        .build();

                List<RosterStudent> students = new ArrayList<>();
                students.add(rosterStudent);
                course1.setRosterStudents(students);

                List<RosterStudent> studentsSpy = Mockito.spy(students);
                course1.setRosterStudents(studentsSpy);

                when(rosterStudentRepository.findById(eq(1L))).thenReturn(Optional.of(rosterStudent));
                when(courseRepository.save(any(Course.class))).thenReturn(course1);

                MvcResult response = mockMvc.perform(delete("/api/rosterstudents/delete")
                        .with(csrf())
                        .param("id", "1"))
                        .andExpect(status().isOk())
                        .andReturn();

                verify(rosterStudentRepository).findById(eq(1L));
                verify(courseRepository).save(any(Course.class));
                verify(rosterStudentRepository).delete(eq(rosterStudent));
                verify(studentsSpy).remove(eq(rosterStudent));

                assertEquals("Successfully deleted roster student and removed him/her from the course list", 
                        response.getResponse().getContentAsString());
        }

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testDeleteRosterStudent_notFound() throws Exception {
                when(rosterStudentRepository.findById(eq(99L))).thenReturn(Optional.empty());

                MvcResult response = mockMvc.perform(delete("/api/rosterstudents/delete")
                        .with(csrf())
                        .param("id", "99"))
                        .andExpect(status().isNotFound())
                        .andReturn();

                verify(rosterStudentRepository).findById(eq(99L));
                verify(rosterStudentRepository, never()).delete(any(RosterStudent.class));
                verify(courseRepository, never()).save(any(Course.class));

                String responseString = response.getResponse().getContentAsString();
                Map<String, String> expectedMap = Map.of(
                        "type", "EntityNotFoundException",
                        "message", "RosterStudent with id 99 not found");
                String expectedJson = mapper.writeValueAsString(expectedMap);
                assertEquals(expectedJson, responseString);
        }

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void testDeleteRosterStudent_nullCourse() throws Exception {
                RosterStudent rosterStudent = RosterStudent.builder()
                        .id(1L)
                        .firstName("Test")
                        .lastName("Student")
                        .studentId("A123456")
                        .email("test@ucsb.edu")
                        .course(null)
                        .rosterStatus(RosterStatus.ROSTER)
                        .orgStatus(OrgStatus.NONE)
                        .build();

                when(rosterStudentRepository.findById(eq(1L))).thenReturn(Optional.of(rosterStudent));

                MvcResult response = mockMvc.perform(delete("/api/rosterstudents/delete")
                        .with(csrf())
                        .param("id", "1"))
                        .andExpect(status().isOk())
                        .andReturn();

                verify(rosterStudentRepository).findById(eq(1L));
                verify(rosterStudentRepository).delete(eq(rosterStudent));
                verify(courseRepository, never()).save(any(Course.class));

                assertEquals("Successfully deleted roster student and removed him/her from the course list", 
                        response.getResponse().getContentAsString());
        }

        @Test
        @WithMockUser(roles = { "USER" })
        public void testDeleteRosterStudent_unauthorized() throws Exception {
                mockMvc.perform(delete("/api/rosterstudents/delete")
                        .with(csrf())
                        .param("id", "1"))
                        .andExpect(status().isForbidden());

                verify(rosterStudentRepository, never()).findById(any());
                verify(rosterStudentRepository, never()).delete(any(RosterStudent.class));
                verify(courseRepository, never()).save(any(Course.class));
        }
}
