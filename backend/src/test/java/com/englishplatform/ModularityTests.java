package com.englishplatform;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/** Ranh giới module (constitution I): vi phạm làm build thất bại. */
class ModularityTests {

  private final ApplicationModules modules =
      ApplicationModules.of(EnglishPlatformApplication.class);

  @Test
  void verifiesModuleBoundaries() {
    modules.verify();
  }

  @Test
  void writesModuleDocumentation() {
    new Documenter(modules).writeDocumentation();
  }
}
