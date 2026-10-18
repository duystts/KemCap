package com.englishplatform.catalog.internal.course;

import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khóa học và gói học (FR-005). Gói {@code INACTIVE} không dùng cho ghi danh mới; sửa giá gói không
 * đổi học phí của ghi danh đã có (ghi danh sao chép giá lúc tạo).
 */
@Service
public class CourseService {

  /** Thời lượng buổi cho phép (phút). */
  public static final Set<Integer> SESSION_MINUTES = Set.of(25, 45, 60, 90);

  private final CourseRepository courses;
  private final CoursePackageRepository packages;

  CourseService(CourseRepository courses, CoursePackageRepository packages) {
    this.courses = courses;
    this.packages = packages;
  }

  public record PackageSpec(
      String name, int sessionCount, int sessionMinutes, int validityDays, long priceVnd) {}

  @Transactional(readOnly = true)
  public List<Course> listCourses(boolean includeInactive) {
    return includeInactive
        ? courses.findAllByOrderByName()
        : courses.findByStatusOrderByName(CatalogItemStatus.ACTIVE);
  }

  @Transactional
  public Course createCourse(String name, String description) {
    requireUniqueName(name, null);
    return courses.save(new Course(name, description));
  }

  @Transactional
  public Course updateCourse(UUID id, String name, String description, CatalogItemStatus status) {
    Course course = course(id);
    requireUniqueName(name, id);
    course.update(name, description, status);
    return course;
  }

  @Transactional(readOnly = true)
  public List<CoursePackage> listPackages(UUID courseId, boolean includeInactive) {
    return packages.search(courseId, includeInactive ? null : CatalogItemStatus.ACTIVE);
  }

  @Transactional
  public CoursePackage createPackage(UUID courseId, PackageSpec spec) {
    if (!course(courseId).getStatus().equals(CatalogItemStatus.ACTIVE)) {
      throw BusinessException.validation("courseId", "Khóa học đã ngừng");
    }
    validate(spec);
    return packages.save(
        new CoursePackage(
            courseId,
            spec.name(),
            spec.sessionCount(),
            spec.sessionMinutes(),
            spec.validityDays(),
            spec.priceVnd()));
  }

  @Transactional
  public CoursePackage updatePackage(UUID id, PackageSpec spec, CatalogItemStatus status) {
    CoursePackage pkg = pkg(id);
    validate(spec);
    pkg.update(
        spec.name(),
        spec.sessionCount(),
        spec.sessionMinutes(),
        spec.validityDays(),
        spec.priceVnd(),
        status);
    return pkg;
  }

  /** Gói đang bán, dùng khi tạo ghi danh mới. */
  @Transactional(readOnly = true)
  public CoursePackage requireActivePackage(UUID id) {
    CoursePackage pkg =
        packages
            .findById(id)
            .orElseThrow(() -> BusinessException.validation("packageId", "Gói học không tồn tại"));
    if (!pkg.isActive()) {
      throw BusinessException.validation(
          "packageId", "Gói học đã ngừng, không dùng cho ghi danh mới");
    }
    return pkg;
  }

  @Transactional(readOnly = true)
  public CoursePackage pkg(UUID id) {
    return packages.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  private Course course(UUID id) {
    return courses.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  private void requireUniqueName(String name, UUID exceptId) {
    boolean taken =
        courses.findByNameIgnoreCase(name.trim()).stream()
            .anyMatch(c -> !c.getId().equals(exceptId));
    if (taken) {
      throw BusinessException.validation("name", "Tên khóa học đã tồn tại");
    }
  }

  private static void validate(PackageSpec spec) {
    if (spec.sessionCount() <= 0) {
      throw BusinessException.validation("sessionCount", "Số buổi phải lớn hơn 0");
    }
    if (!SESSION_MINUTES.contains(spec.sessionMinutes())) {
      throw BusinessException.validation(
          "sessionMinutes", "Thời lượng chỉ 25, 45, 60 hoặc 90 phút");
    }
    if (spec.validityDays() <= 0) {
      throw BusinessException.validation("validityDays", "Hạn dùng phải lớn hơn 0 ngày");
    }
    if (spec.priceVnd() < 0) {
      throw BusinessException.validation("priceVnd", "Giá không được âm");
    }
  }
}
