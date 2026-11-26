package ocop.vn.ocop_tourims_platform.config;

import java.util.*;
import lombok.RequiredArgsConstructor;
import ocop.vn.ocop_tourims_platform.entity.*;
import ocop.vn.ocop_tourims_platform.repository.*;
import ocop.vn.ocop_tourims_platform.repository.management.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component @Profile("local") @Order(100) @RequiredArgsConstructor
public class LocalTestAccounts implements ApplicationRunner {
  private final UserRepository users;
  private final RoleRepository roles;
  private final UserManagementRepository administration;
  private final BusinessManagementRepository businesses;
  private final BusinessMemberManagementRepository members;
  private final PasswordEncoder passwords;
  @Value("${local-test.password:Ocop@123456}") private String password;
  @Override @Transactional public void run(ApplicationArguments args) {
    administration.lockAdministration();
    for(String code:List.of("CUSTOMER","PARTNER","ADMIN")) {
      String email=code.toLowerCase(Locale.ROOT)+"@ocop.local";
      if(users.findByEmailIgnoreCase(email).isPresent()) continue;
      User u=new User();u.setEmail(email);u.setDisplayName(switch(code){case "ADMIN"->"Quản trị OCOP GO";case "PARTNER"->"Đối tác OCOP GO";default->"Khách hàng OCOP GO";});
      u.setPasswordHash(passwords.encode(password));u.addRole(roles.findByCode(code).orElseThrow(),null);users.saveAndFlush(u);
      if(code.equals("PARTNER")) {
        Business b=new Business();b.setName("Cơ sở thử nghiệm OCOP GO");b.setIntroduction("Cơ sở dành cho tài khoản đối tác thử nghiệm");businesses.saveAndFlush(b);
        BusinessMember m=new BusinessMember();m.setBusiness(b);m.setUser(u);members.save(m);
      }
    }
  }
}
