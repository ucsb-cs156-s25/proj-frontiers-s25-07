package edu.ucsb.cs156.frontiers.startup;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;

import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;

/** This class contains a `run` method that is called once at application startup time. */
@Slf4j
@Component
public class FrontiersStartup {

    @Value("#{'${app.admin.emails}'.split(',')}")
    private List<String> adminEmails;
    @Autowired private AdminRepository adminRepository;

  /**
   * Called once at application startup time . Put code here if you want it to run once each time
   * the Spring Boot application starts up in all environments.
   */
  public void alwaysRunOnStartup() {
    log.info("alwaysRunOnStartup called");
    try {
      adminEmails.forEach(
        (email) -> {
          Admin admin = new Admin(email);
          adminRepository.save(admin);
        }
      );
    } catch (Exception e) {
      log.error("Error in loading all ADMIN_EMAILS:", e);
    }
  }
}