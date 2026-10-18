package com.englishplatform.identity.internal.seed;

import com.englishplatform.identity.Role;
import com.englishplatform.identity.UserStatus;
import com.englishplatform.identity.internal.PolicyConsent;
import com.englishplatform.identity.internal.PolicyConsentRepository;
import com.englishplatform.identity.internal.PrivacyProperties;
import com.englishplatform.identity.internal.UserAccount;
import com.englishplatform.identity.internal.UserAccountRepository;
import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tài khoản demo (profile {@code demo-seed}), mật khẩu {@code Demo@1234}, kèm đồng ý chính sách
 * hiện hành để consent gate không chặn demo. Chạy lại không tạo trùng.
 */
@Component
@Profile("demo-seed")
@Order(10)
class IdentityDemoSeed implements ApplicationRunner {

  static final String PASSWORD = "Demo@1234";
  private static final Logger log = LoggerFactory.getLogger(IdentityDemoSeed.class);

  private final UserAccountRepository users;
  private final PolicyConsentRepository consents;
  private final PrivacyProperties privacy;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  IdentityDemoSeed(
      UserAccountRepository users,
      PolicyConsentRepository consents,
      PrivacyProperties privacy,
      PasswordEncoder passwordEncoder,
      Clock clock) {
    this.users = users;
    this.consents = consents;
    this.privacy = privacy;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (users.findByEmailIgnoreCase("admin@demo.local").isPresent()) {
      return;
    }
    String hash = passwordEncoder.encode(PASSWORD);
    create("admin@demo.local", "Giáo vụ Demo", Role.ADMIN, null, hash);
    create("teacher1@demo.local", "Nguyễn Thị Lan", Role.TEACHER, null, hash);
    create("teacher2@demo.local", "Trần Văn Minh", Role.TEACHER, null, hash);
    String[] students = {
      "Lê Hoàng An", "Phạm Thu Hà", "Võ Quốc Bảo", "Đặng Mỹ Linh", "Bùi Gia Huy"
    };
    for (int i = 0; i < students.length; i++) {
      create(
          "student" + (i + 1) + "@demo.local",
          students[i],
          Role.STUDENT,
          LocalDate.of(1995 + i, 3, 10 + i),
          hash);
    }
    log.info("Demo seed: tạo 8 tài khoản demo (mật khẩu {})", PASSWORD);
  }

  private void create(String email, String name, Role role, LocalDate dob, String hash) {
    UserAccount user = new UserAccount(email, name, role, dob, UserStatus.INVITED);
    user.activate(hash);
    users.save(user);
    consents.save(new PolicyConsent(user.getId(), privacy.policyVersion(), clock.instant()));
  }
}
