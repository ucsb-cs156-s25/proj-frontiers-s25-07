package edu.ucsb.cs156.frontiers.startup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** This class contains a `run` method that is called once at application startup time. */
@Slf4j
@Configuration
public class FrontiersApplicationRunner implements ApplicationRunner {

  @Autowired FrontiersStartup frontiersStartup;

  /** Called once at application startup time*/
  @Override
  public void run(ApplicationArguments args) throws Exception {
    log.info("FrontiersApplicationRunner.run called");
    frontiersStartup.alwaysRunOnStartup();
  }
}
