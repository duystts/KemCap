package com.englishplatform.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.englishplatform.identity.Role;
import com.englishplatform.identity.UserStatus;
import com.englishplatform.identity.internal.PolicyConsent;
import com.englishplatform.identity.internal.PolicyConsentRepository;
import com.englishplatform.identity.internal.PrivacyProperties;
import com.englishplatform.identity.internal.UserAccount;
import com.englishplatform.identity.internal.UserAccountRepository;
import com.englishplatform.support.TestClockConfig.MutableClock;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Nền chung cho IT gọi API thật: tạo người dùng đã kích hoạt + đồng ý chính sách, lấy token. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({PostgresContainerConfig.class, TestClockConfig.class})
public abstract class ApiTestSupport {

  protected static final String PASSWORD = "Matkhau123";

  @Autowired protected MockMvc mvc;
  @Autowired protected UserAccountRepository users;
  @Autowired protected PolicyConsentRepository consents;
  @Autowired protected PrivacyProperties privacy;
  @Autowired protected PasswordEncoder passwordEncoder;
  @Autowired protected MutableClock clock;

  @AfterEach
  void resetClock() {
    clock.reset();
  }

  /** Người dùng ACTIVE đã đồng ý chính sách hiện hành, kèm access token. */
  public record TestUser(UUID id, String email, Role role, String token) {
    public String bearer() {
      return "Bearer " + token;
    }
  }

  protected TestUser user(Role role) throws Exception {
    UserAccount account = activeAccount(role);
    return new TestUser(account.getId(), account.getEmail(), role, login(account.getEmail()));
  }

  protected UserAccount activeAccount(Role role) {
    LocalDate dob = role == Role.STUDENT ? LocalDate.of(2000, 1, 1) : null;
    UserAccount account =
        new UserAccount(uniqueEmail(), "Người dùng " + role, role, dob, UserStatus.INVITED);
    account.activate(passwordEncoder.encode(PASSWORD));
    users.save(account);
    consents.save(new PolicyConsent(account.getId(), privacy.policyVersion(), Instant.now()));
    return account;
  }

  protected String login(String email) throws Exception {
    String body =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.accessToken");
  }

  protected ResultActions call(MockHttpServletRequestBuilder request, TestUser as, String json)
      throws Exception {
    request.header("Authorization", as.bearer());
    if (json != null) {
      request.contentType(MediaType.APPLICATION_JSON).content(json);
    }
    return mvc.perform(request);
  }

  protected ResultActions call(MockHttpServletRequestBuilder request, TestUser as)
      throws Exception {
    return call(request, as, null);
  }

  protected static String read(ResultActions result, String path) throws Exception {
    Object value = JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    return String.valueOf(value);
  }

  protected static String uniqueEmail() {
    return "u" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
  }
}
