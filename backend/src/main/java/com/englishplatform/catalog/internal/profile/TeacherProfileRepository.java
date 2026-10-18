package com.englishplatform.catalog.internal.profile;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, UUID> {}
