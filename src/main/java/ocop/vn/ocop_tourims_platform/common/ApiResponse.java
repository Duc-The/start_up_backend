package ocop.vn.ocop_tourims_platform.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import org.springframework.http.HttpStatus;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record ApiResponse<T>(
        int status,
        String message,
        T data,
        String errorCode,
        List<String> errors,
        String path
) {
    public static <T> ApiResponse<T> success(HttpStatus status, String message, T data, String path) {
        return new ApiResponse<>(status.value(), message, data, null, null, path);
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String message, String errorCode, List<String> errors, String path) {
        return new ApiResponse<>(status.value(), message, null, errorCode, errors, path);
    }
}
