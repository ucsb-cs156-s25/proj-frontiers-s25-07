package edu.ucsb.cs156.frontiers.startup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.ApplicationArguments;

import static org.mockito.Mockito.*;

class FrontiersApplicationRunnerTests {

    private FrontiersApplicationRunner frontiersApplicationRunner;

    @Mock
    private FrontiersStartup frontiersStartup;

    @Mock
    private ApplicationArguments mockArgs;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        frontiersApplicationRunner = new FrontiersApplicationRunner();
        frontiersApplicationRunner.frontiersStartup = frontiersStartup; // Manually inject mock
    }

    @Test
    void test_run_calls_AlwaysRunOnStartup() throws Exception {
        frontiersApplicationRunner.run(mockArgs);
        verify(frontiersStartup, times(1)).alwaysRunOnStartup();
    }
}
