package com.englishplatform.catalog.internal.web;

import com.englishplatform.catalog.internal.CatalogAccess;
import com.englishplatform.catalog.internal.course.CatalogItemStatus;
import com.englishplatform.catalog.internal.course.Course;
import com.englishplatform.catalog.internal.course.CourseService;
import com.englishplatform.shared.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /api/v1/courses} — mọi người đọc khóa đang mở (admin thấy cả khóa đã ngừng); admin sửa.
 */
@RestController
@RequestMapping("/api/v1/courses")
class CourseController {

  private final CourseService service;

  CourseController(CourseService service) {
    this.service = service;
  }

  record CourseDto(UUID id, String name, String description, CatalogItemStatus status) {
    static CourseDto of(Course c) {
      return new CourseDto(c.getId(), c.getName(), c.getDescription(), c.getStatus());
    }
  }

  record CreateCourseRequest(@NotBlank @Size(max = 150) String name, String description) {}

  record UpdateCourseRequest(
      @NotBlank @Size(max = 150) String name,
      String description,
      @NotNull CatalogItemStatus status) {}

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  List<CourseDto> list() {
    boolean admin = CatalogAccess.isAdmin(CurrentUser.require());
    return service.listCourses(admin).stream().map(CourseDto::of).toList();
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  CourseDto create(@Valid @RequestBody CreateCourseRequest request) {
    return CourseDto.of(service.createCourse(request.name(), request.description()));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  CourseDto update(@PathVariable UUID id, @Valid @RequestBody UpdateCourseRequest request) {
    return CourseDto.of(
        service.updateCourse(id, request.name(), request.description(), request.status()));
  }
}
