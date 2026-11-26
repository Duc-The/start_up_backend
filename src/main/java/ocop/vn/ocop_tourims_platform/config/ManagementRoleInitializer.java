package ocop.vn.ocop_tourims_platform.config;

import lombok.RequiredArgsConstructor;
import ocop.vn.ocop_tourims_platform.repository.RoleRepository;
import org.springframework.boot.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@org.springframework.core.annotation.Order(0)
@RequiredArgsConstructor
public class ManagementRoleInitializer implements ApplicationRunner {
  private final RoleRepository roles;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    roles.ensureCustomerRole();
    roles.ensureRole("PARTNER", "Đối tác", "Quản lý cơ sở và nội dung được phân công");
    roles.ensureRole("ADMIN", "Quản trị viên", "Quản trị và kiểm duyệt toàn hệ thống");
  }
}
