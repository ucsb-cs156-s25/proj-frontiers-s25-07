package edu.ucsb.cs156.frontiers.startup;

import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.List;

import static org.mockito.Mockito.*;

class FrontiersStartupTests {

    @Mock
    private AdminRepository adminRepository;

    private FrontiersStartup frontiersStartup;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        frontiersStartup = new FrontiersStartup();
        frontiersStartup.adminRepository = adminRepository;
        frontiersStartup.adminEmails = List.of("acdamstedt@ucsb.edu", "phtcon@ucsb.edu");
    }

    @Test
    void test_AlwaysRunOnStartup_saves_admins() {
        frontiersStartup.alwaysRunOnStartup();

        verify(adminRepository).save(new Admin("acdamstedt@ucsb.edu"));
        verify(adminRepository).save(new Admin("phtcon@ucsb.edu"));
        verifyNoMoreInteractions(adminRepository);
    }

    @Test
    void test_AlwaysRunOnStartup_handles_exception() {
        doThrow(new RuntimeException("Simulated error")).when(adminRepository).save(any(Admin.class));

        frontiersStartup.alwaysRunOnStartup();

        verify(adminRepository, times(1)).save(any(Admin.class));
    }
}
