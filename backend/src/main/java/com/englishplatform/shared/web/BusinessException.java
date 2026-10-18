package com.englishplatform.shared.web;

import java.util.List;

/** Lỗi nghiệp vụ trả về client dưới dạng Problem Details có {@code code}. */
public class BusinessException extends RuntimeException {

  private final ErrorCode code;
  private final transient List<FieldError> errors;

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

  public record FieldError(String field, String message) {}
}
