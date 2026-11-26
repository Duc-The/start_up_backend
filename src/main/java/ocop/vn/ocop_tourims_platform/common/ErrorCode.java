package ocop.vn.ocop_tourims_platform.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email đã được sử dụng!"),
    PHONE_ALREADY_EXISTS(HttpStatus.CONFLICT, "Số điện thoại đã được sử dụng"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu yêu cầu"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác"),
    ACCOUNT_INACTIVE(HttpStatus.FORBIDDEN, "Tài khoản đã bị vô hiệu hóa"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Token không hợp lệ, đã hết hạn hoặc phiên đăng nhập đã bị thu hồi"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập để thực hiện yêu cầu"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện yêu cầu"),
    DATA_CONFLICT(HttpStatus.CONFLICT, "Dữ liệu bị trùng hoặc không đáp ứng ràng buộc của hệ thống"),
    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "Chưa tìm được đường đi phù hợp giữa các điểm đã chọn"),
    ROUTE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Chưa lấy được lộ trình từ Map4D. Vui lòng kiểm tra quyền chỉ đường của key hoặc thử lại sau"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ tạm thời gián đoạn, vui lòng thử lại sau"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống"
    );

    private final HttpStatus status;
    private final String message;
}
