package com.englishplatform.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Chỉ {@code GET /me} và {@code POST /me/consents} được đi qua khi chưa đồng ý chính sách (R14).
 */
class ConsentGateFilterTest {

  private static MockHttpServletRequest request(String method, String path) {
    return new MockHttpServletRequest(method, path);
  }

  @Test
  void consentEndpointsAreNotGuarded() {
    assertThat(ConsentGateFilter.guarded(request("GET", "/api/v1/me"))).isFalse();
    assertThat(ConsentGateFilter.guarded(request("POST", "/api/v1/me/consents"))).isFalse();
  }

  @Test
  void otherMethodsOnConsentEndpointsAreGuarded() {
    assertThat(ConsentGateFilter.guarded(request("PATCH", "/api/v1/me"))).isTrue();
    assertThat(ConsentGateFilter.guarded(request("GET", "/api/v1/me/consents"))).isTrue();
  }

  @Test
  void businessApisAreGuarded() {
    assertThat(ConsentGateFilter.guarded(request("GET", "/api/v1/sessions"))).isTrue();
    assertThat(ConsentGateFilter.guarded(request("POST", "/api/v1/data-subject-requests")))
        .isTrue();
    assertThat(ConsentGateFilter.guarded(request("GET", "/api/v1/me/other"))).isTrue();
  }

  @Test
  void authAndNonApiPathsAreNotChecked() {
    assertThat(ConsentGateFilter.checked(request("POST", "/api/v1/auth/login"))).isFalse();
    assertThat(ConsentGateFilter.guarded(request("POST", "/api/v1/auth/login"))).isFalse();
    assertThat(ConsentGateFilter.checked(request("GET", "/actuator/health"))).isFalse();
    assertThat(ConsentGateFilter.guarded(request("GET", "/actuator/health"))).isFalse();
  }

  @Test
  void percentEncodedAndUnknownPathsAreChecked() {
    assertThat(ConsentGateFilter.guarded(request("GET", "/api/v%31/sessions"))).isTrue();
    assertThat(ConsentGateFilter.guarded(request("GET", "/api/v%31/me"))).isFalse();
    assertThat(ConsentGateFilter.checked(request("GET", "/api/v%31/me"))).isTrue();
    assertThat(ConsentGateFilter.guarded(request("GET", "/api/v2/sessions"))).isTrue();
    assertThat(ConsentGateFilter.guarded(request("GET", "/actuator/env"))).isTrue();
  }

  @Test
  void contextPathIsIgnored() {
    MockHttpServletRequest withContext = request("GET", "/app/api/v1/me");
    withContext.setContextPath("/app");
    assertThat(ConsentGateFilter.guarded(withContext)).isFalse();
  }
}
