package com.englishplatform.catalog.internal.web;

import com.englishplatform.catalog.internal.CatalogAccess;
import com.englishplatform.catalog.internal.profile.ProfileService;
import com.englishplatform.catalog.internal.profile.StudentProfile;
import com.englishplatform.catalog.internal.profile.TeacherProfile;
import com.englishplatform.identity.UserStatus;
import com.englishplatform.shared.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Hồ sơ và danh sách học viên (contracts/rest-api.md nhóm catalog). Không có quyền xem → 404. */
@RestController
@RequestMapping("/api/v1")
class ProfileController {

  private final ProfileService service;

  ProfileController(ProfileService service) {
    this.service = service;
  }

  record TeacherProfileDto(UUID userId, String bio, List<String> specialties, String certificates) {
    static TeacherProfileDto of(TeacherProfile p) {
      return new TeacherProfileDto(
          p.getUserId(), p.getBio(), p.getSpecialties(), p.getCertificates());
    }
  }

  record TeacherProfileRequest(String bio, List<String> specialties, String certificates) {}

  record StudentProfileDto(
      UUID userId, StudentProfile.CefrLevel selfReportedLevel, String occupation, String notes) {
    static StudentProfileDto of(StudentProfile p, boolean includeNotes) {
      return new StudentProfileDto(
          p.getUserId(),
          p.getSelfReportedLevel(),
          p.getOccupation(),
          includeNotes ? p.getNotes() : null);
    }
  }

  record StudentProfileRequest(
      StudentProfile.CefrLevel selfReportedLevel,
      @Size(max = 100) String occupation,
      String notes) {}

  record StudentListItem(
      UUID id,
      String fullName,
      String email,
      UserStatus status,
      StudentProfile.CefrLevel selfReportedLevel,
      Set<UUID> teacherIds) {
    static StudentListItem of(ProfileService.StudentSummary s) {
      return new StudentListItem(
          s.user().id(),
          s.user().fullName(),
          s.user().email(),
          s.user().status(),
          s.selfReportedLevel(),
          s.teacherIds());
    }
  }

  @GetMapping("/teachers/{id}/profile")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  TeacherProfileDto teacherProfile(@PathVariable UUID id) {
    return TeacherProfileDto.of(service.teacherProfile(CurrentUser.require(), id));
  }

  @PutMapping("/teachers/{id}/profile")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  TeacherProfileDto updateTeacherProfile(
      @PathVariable UUID id, @Valid @RequestBody TeacherProfileRequest r) {
    return TeacherProfileDto.of(
        service.updateTeacherProfile(
            CurrentUser.require(), id, r.bio(), r.specialties(), r.certificates()));
  }

  /** Ghi chú nội bộ ({@code notes}) chỉ admin thấy. */
  @GetMapping("/students/{id}/profile")
  @PreAuthorize("isAuthenticated()")
  StudentProfileDto studentProfile(@PathVariable UUID id) {
    CurrentUser user = CurrentUser.require();
    return StudentProfileDto.of(service.studentProfile(user, id), CatalogAccess.isAdmin(user));
  }

  @PutMapping("/students/{id}/profile")
  @PreAuthorize("hasRole('ADMIN')")
  StudentProfileDto updateStudentProfile(
      @PathVariable UUID id, @Valid @RequestBody StudentProfileRequest r) {
    return StudentProfileDto.of(
        service.updateStudentProfile(id, r.selfReportedLevel(), r.occupation(), r.notes()), true);
  }

  /** {@code needsConsulting} được nhận nhưng chưa lọc cho tới khi có API của learning (U4). */
  @GetMapping("/students")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  List<StudentListItem> students(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) UUID teacherId,
      @RequestParam(required = false) UserStatus status,
      @RequestParam(required = false) Boolean needsConsulting) {
    return service.listStudents(CurrentUser.require(), q, teacherId, status).stream()
        .map(StudentListItem::of)
        .toList();
  }
}
