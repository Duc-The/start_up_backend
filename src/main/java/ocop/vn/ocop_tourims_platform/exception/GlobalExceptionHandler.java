package ocop.vn.ocop_tourims_platform.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import ocop.vn.ocop_tourims_platform.common.*;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiResponse<Void>> handleLargeUpload(
      Exception exception, HttpServletRequest request) {
    return error(
        ErrorCode.VALIDATION_FAILED, List.of("Tệp tải lên vượt quá dung lượng cho phép"), request);
  }

  @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleMissingResource(
      Exception exception, HttpServletRequest request) {
    return error(ErrorCode.RESOURCE_NOT_FOUND, null, request);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleInvalidData(
      IllegalArgumentException exception, HttpServletRequest request) {
    return error(
        ErrorCode.VALIDATION_FAILED,
        List.of(
            exception.getMessage() == null
                ? "Dữ liệu đầu vào không hợp lệ"
                : exception.getMessage()),
        request);
  }

  @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<ApiResponse<Void>> handleConcurrentChange(
      Exception exception, HttpServletRequest request) {
    return error(
        ErrorCode.DATA_CONFLICT,
        List.of("Dữ liệu đã thay đổi, vui lòng tải lại trước khi cập nhật"),
        request);
  }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleDenied(
      Exception exception, HttpServletRequest request) {
    return error(ErrorCode.ACCESS_DENIED, null, request);
  }

  @ExceptionHandler(
      org.springframework.web.method.annotation.HandlerMethodValidationException.class)
  public ResponseEntity<ApiResponse<Void>> handleParameterValidation(
      org.springframework.web.method.annotation.HandlerMethodValidationException exception,
      HttpServletRequest request) {
    var errors =
        exception.getParameterValidationResults().stream()
            .flatMap(result -> result.getResolvableErrors().stream())
            .map(message -> message.getDefaultMessage())
            .toList();
    return error(ErrorCode.VALIDATION_FAILED, errors, request);
  }

  @ExceptionHandler({
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
    org.springframework.web.bind.MissingServletRequestParameterException.class
  })
  public ResponseEntity<ApiResponse<Void>> handleInvalidParameter(
      Exception exception, HttpServletRequest request) {
    return error(
        ErrorCode.VALIDATION_FAILED,
        List.of("Tham số yêu cầu bị thiếu hoặc không đúng định dạng"),
        request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    List<String> errors =
        exception.getBindingResult().getAllErrors().stream()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .distinct()
            .toList();
    return error(ErrorCode.VALIDATION_FAILED, errors, request);
  }

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusiness(
      BusinessException exception, HttpServletRequest request) {
    return error(exception.getErrorCode(), null, request);
  }

  @ExceptionHandler(JwtException.class)
  public ResponseEntity<ApiResponse<Void>> handleToken(
      JwtException exception, HttpServletRequest request) {
    return error(ErrorCode.INVALID_TOKEN, null, request);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    return error(
        ErrorCode.VALIDATION_FAILED,
        List.of("Nội dung JSON không hợp lệ hoặc thiếu dữ liệu yêu cầu"),
        request);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleDataConflict(
      DataIntegrityViolationException exception, HttpServletRequest request) {
    ErrorCode code = ErrorCode.DATA_CONFLICT;
    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
      if (cause instanceof ConstraintViolationException constraint
          && ("uk_users_email".equals(constraint.getConstraintName())
              || "ux_users_email_normalized".equals(constraint.getConstraintName()))) {
        code = ErrorCode.EMAIL_ALREADY_EXISTS;
        break;
      }
    }
    return error(code, null, request);
  }

  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ApiResponse<Void>> handleUnavailableData(
      DataAccessException exception, HttpServletRequest request) {
    log.error("Không thể truy cập dịch vụ dữ liệu", exception);
    return error(ErrorCode.SERVICE_UNAVAILABLE, null, request);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiResponse<Void>> handleUnsupportedMethod(
      HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
    ApiResponse<Void> body =
        ApiResponse.error(
            HttpStatus.METHOD_NOT_ALLOWED,
            "Phương thức HTTP không được hỗ trợ",
            "METHOD_NOT_ALLOWED",
            null,
            request.getRequestURI());
    return ResponseEntity.status(body.status()).body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpected(
      Exception exception, HttpServletRequest request) {
    log.error("Lỗi khi xử lý yêu cầu {}", request.getRequestURI(), exception);
    return error(ErrorCode.INTERNAL_SERVER_ERROR, null, request);
  }

  private ResponseEntity<ApiResponse<Void>> error(
      ErrorCode code, List<String> details, HttpServletRequest request) {
    ApiResponse<Void> body =
        ApiResponse.error(
            code.getStatus(), code.getMessage(), code.name(), details, request.getRequestURI());
    return ResponseEntity.status(body.status()).body(body);
  }
}
