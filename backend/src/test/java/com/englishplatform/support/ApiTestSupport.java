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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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

  /**
   * Token cấp lại theo clock hiện tại khi test đã tua clock (hạn token theo Clock của app — R11).
   */
  private final Map<UUID, Instant> tokenIssuedAt = new ConcurrentHashMap<>();

  private final Map<UUID, String> freshTokens = new ConcurrentHashMap<>();

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
    String token = login(account.getEmail());
    tokenIssuedAt.put(account.getId(), clock.instant());
    freshTokens.put(account.getId(), token);
    return new TestUser(account.getId(), account.getEmail(), role, token);
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
    request.header("Authorization", "Bearer " + tokenFor(as));
    if (json != null) {
      request.contentType(MediaType.APPLICATION_JSON).content(json);
    }
    return mvc.perform(request);
  }

  /** Như frontend tự refresh: clock đã qua hơn 10 phút kể từ lúc cấp → đăng nhập lại. */
  private String tokenFor(TestUser as) throws Exception {
    Instant issued = tokenIssuedAt.get(as.id());
    if (issued == null || !clock.instant().isAfter(issued.plus(Duration.ofMinutes(10)))) {
      return freshTokens.getOrDefault(as.id(), as.token());
    }
    String token = login(as.email());
    tokenIssuedAt.put(as.id(), clock.instant());
    freshTokens.put(as.id(), token);
    return token;
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
