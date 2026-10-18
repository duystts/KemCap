package com.englishplatform.catalog.internal.web;

import com.englishplatform.catalog.internal.CatalogAccess;
import com.englishplatform.catalog.internal.course.CatalogItemStatus;
import com.englishplatform.catalog.internal.course.CoursePackage;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /api/v1/packages} — gói học (FR-005). Sửa giá chỉ áp dụng cho ghi danh mới; ghi danh cũ
 * giữ giá đã sao chép.
 */
@RestController
@RequestMapping("/api/v1/packages")
class PackageController {

  private final CourseService service;

  PackageController(CourseService service) {
    this.service = service;
  }

  record PackageDto(
      UUID id,
      UUID courseId,
      String name,
      int sessionCount,
      int sessionMinutes,
      int validityDays,
      Long priceVnd,
      CatalogItemStatus status) {

    /** Giáo viên không cần biết học phí: {@code priceVnd} = null. */
    static PackageDto of(CoursePackage p, boolean showPrice) {
      return new PackageDto(
          p.getId(),
          p.getCourseId(),
          p.getName(),
          p.getSessionCount(),
          p.getSessionMinutes(),
          p.getValidityDays(),
          showPrice ? p.getPriceVnd() : null,
          p.getStatus());
    }
  }

  record CreatePackageRequest(
      @NotNull UUID courseId,
      @NotBlank @Size(max = 150) String name,
      int sessionCount,
      int sessionMinutes,
      int validityDays,
      long priceVnd) {}

  record UpdatePackageRequest(
      @NotBlank @Size(max = 150) String name,
      int sessionCount,
      int sessionMinutes,
      int validityDays,
      long priceVnd,
      @NotNull CatalogItemStatus status) {}

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  List<PackageDto> list(@RequestParam(required = false) UUID courseId) {
    CurrentUser user = CurrentUser.require();
    boolean admin = CatalogAccess.isAdmin(user);
    boolean showPrice = !CatalogAccess.isTeacher(user);
    return service.listPackages(courseId, admin).stream()
        .map(p -> PackageDto.of(p, showPrice))
        .toList();
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  PackageDto create(@Valid @RequestBody CreatePackageRequest r) {
    return PackageDto.of(
        service.createPackage(
            r.courseId(),
            new CourseService.PackageSpec(
                r.name(), r.sessionCount(), r.sessionMinutes(), r.validityDays(), r.priceVnd())),
        true);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  PackageDto update(@PathVariable UUID id, @Valid @RequestBody UpdatePackageRequest r) {
    return PackageDto.of(
        service.updatePackage(
            id,
            new CourseService.PackageSpec(
                r.name(), r.sessionCount(), r.sessionMinutes(), r.validityDays(), r.priceVnd()),
            r.status()),
        true);
  }
}
