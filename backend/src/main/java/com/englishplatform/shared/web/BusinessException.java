package com.englishplatform.shared.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lỗi nghiệp vụ trả về client dưới dạng Problem Details có {@code code}. */
public class BusinessException extends RuntimeException {

  private final ErrorCode code;
  private final transient List<FieldError> errors;
  private final transient Map<String, Object> details = new LinkedHashMap<>();

  public BusinessException(ErrorCode code) {
    this(code, code.defaultMessage(), List.of());
  }

  public BusinessException(ErrorCode code, String message) {
    this(code, message, List.of());
  }

  public BusinessException(ErrorCode code, String message, List<FieldError> errors) {
    super(message);
    this.code = code;
    this.errors = errors;
  }

  public static BusinessException validation(String field, String message) {
    return new BusinessException(
        ErrorCode.VALIDATION_FAILED,
        ErrorCode.VALIDATION_FAILED.defaultMessage(),
        List.of(new FieldError(field, message)));
  }

  public ErrorCode code() {
    return code;
  }

  public List<FieldError> errors() {
    return errors;
  }

  /**
   * Thêm property vào Problem Details, ví dụ {@code sessions[]} khi ngày nghỉ trùng buổi đã đặt.
   */
  public BusinessException withDetail(String key, Object value) {
    details.put(key, value);
    return this;
  }

  public Map<String, Object> details() {
    return details;
  }

  public record FieldError(String field, String message) {}
}
