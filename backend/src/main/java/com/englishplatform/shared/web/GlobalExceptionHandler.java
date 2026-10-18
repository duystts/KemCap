package com.englishplatform.shared.web;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** RFC 9457 Problem Details + property {@code code} và {@code errors[{field, message}]}. */
@RestControllerAdvice
class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BusinessException.class)
  ProblemDetail handleBusiness(BusinessException ex) {
    return problem(ex.code(), ex.getMessage(), ex.errors());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
    List<BusinessException.FieldError> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> new BusinessException.FieldError(e.getField(), e.getDefaultMessage()))
            .toList();
    return problem(
        ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.defaultMessage(), errors);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
    return problem(ErrorCode.VALIDATION_FAILED, "Nội dung yêu cầu không đọc được", List.of());
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
    return problem(
        ErrorCode.CONCURRENT_MODIFICATION,
        ErrorCode.CONCURRENT_MODIFICATION.defaultMessage(),
        List.of());
  }

  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail handleAccessDenied(AccessDeniedException ex) {
    return problem(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.defaultMessage(), List.of());
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ProblemDetail handleNoResource(NoResourceFoundException ex) {
    return problem(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.defaultMessage(), List.of());
  }

  @ExceptionHandler(TypeMismatchException.class)
  ProblemDetail handleTypeMismatch(TypeMismatchException ex) {
    return problem(ErrorCode.VALIDATION_FAILED, "Tham số không đúng định dạng", List.of());
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpected(Exception ex) {
    // lỗi 4xx của Spring MVC (sai method, sai Content-Type, thiếu tham số...) giữ nguyên status
    if (ex instanceof ErrorResponse error && error.getStatusCode().is4xxClientError()) {
      return framework4xx(error);
    }
    log.error("Unexpected error", ex);
    ProblemDetail pd =
        ProblemDetail.forStatusAndDetail(
            org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
            "Lỗi hệ thống, vui lòng thử lại");
    pd.setProperty("code", "INTERNAL_ERROR");
    return pd;
  }

  private static ProblemDetail framework4xx(ErrorResponse error) {
    HttpStatusCode status = error.getStatusCode();
    ErrorCode code =
        status.value() == 404
            ? ErrorCode.NOT_FOUND
            : status.value() == 401 ? ErrorCode.UNAUTHENTICATED : ErrorCode.VALIDATION_FAILED;
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, code.defaultMessage());
    pd.setProperty("code", code.name());
    return pd;
  }

  static ProblemDetail problem(
      ErrorCode code, String message, List<BusinessException.FieldError> errors) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(code.status(), message);
    pd.setTitle(code.status().getReasonPhrase());
    pd.setProperty("code", code.name());
    if (!errors.isEmpty()) {
      pd.setProperty("errors", errors);
    }
    return pd;
  }
}
