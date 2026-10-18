package com.englishplatform.catalog.internal;

import com.englishplatform.catalog.internal.profile.StudentProfileRepository;
import com.englishplatform.identity.StudentAnonymized;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Ẩn danh hóa (FR-031): xóa {@code notes}, {@code self_reported_level}, {@code occupation}.
 * Idempotent — Event Publication Registry có thể phát lại khi khởi động lại.
 */
@Component
class AnonymizationListener {

  private final StudentProfileRepository profiles;

  AnonymizationListener(StudentProfileRepository profiles) {
    this.profiles = profiles;
  }

  @ApplicationModuleListener
  void on(StudentAnonymized event) {
    profiles.findById(event.studentId()).ifPresent(profile -> profile.anonymize());
  }
}
