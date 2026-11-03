package com.englishplatform.identity.internal;

import com.englishplatform.identity.UserStatus;
import com.englishplatform.shared.security.CurrentUser;
import com.englishplatform.shared.security.SecurityFilterContributor;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

/**
 * Chặn theo trạng thái tài khoản mỗi request có token (trừ các đường dẫn công khai):
 *
 * <ul>
 *   <li>tài khoản không còn {@code ACTIVE} (ngừng kích hoạt, ẩn danh hóa) → 401 {@code
 *       UNAUTHENTICATED} ngay, không chờ access token 15 phút hết hạn;
 *   <li>chưa đồng ý phiên bản chính sách hiện hành → chỉ được {@code GET /me}, {@code POST
 *       /me/consents}; còn lại 403 {@code CONSENT_REQUIRED} (FR-030).
 * </ul>
 */
@Component
class ConsentGateFilter implements SecurityFilterContributor {

  private static final UrlPathHelper PATHS = new UrlPathHelper();

  private static final String INACTIVE_BODY =
      "{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,"
          + "\"detail\":\"Tài khoản không còn hoạt động\",\"code\":\"UNAUTHENTICATED\"}";

  private static final String BODY =
      "{\"type\":\"about:blank\",\"title\":\"Forbidden\",\"status\":403,"
          + "\"detail\":\"Bạn cần đồng ý điều khoản và chính sách dữ liệu hiện hành\","
          + "\"code\":\"CONSENT_REQUIRED\"}";

  private final ConsentService consentService;
  private final UserAccountRepository users;

  ConsentGateFilter(ConsentService consentService, UserAccountRepository users) {
    this.consentService = consentService;
    this.users = users;
  }

  @Override
  public Filter filter() {
    return new OncePerRequestFilter() {
      @Override
      protected void doFilterInternal(
          HttpServletRequest request, HttpServletResponse response, FilterChain chain)
          throws ServletException, IOException {
        Optional<CurrentUser> user = CurrentUser.find();
        if (user.isPresent() && checked(request)) {
          if (!users.existsByIdAndStatus(user.get().id(), UserStatus.ACTIVE)) {
            write(response, 401, INACTIVE_BODY);
            return;
          }
          if (guarded(request) && !consentService.hasConsentedCurrent(user.get().id())) {
            write(response, 403, BODY);
            return;
          }
        }
        chain.doFilter(request, response);
      }
    };
  }

  private static void write(HttpServletResponse response, int status, String body)
      throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(body);
  }

  /** Đường dẫn đã giải mã như Spring MVC khớp route; URI thô bị vượt qua bằng {@code /api/v%31}. */
  private static String path(HttpServletRequest request) {
    return PATHS.getPathWithinApplication(request);
  }

  /** Mặc định phải kiểm; chỉ miễn các đường dẫn công khai đã liệt kê trong SecurityConfig. */
  static boolean checked(HttpServletRequest request) {
    String path = path(request);
    return !path.startsWith("/api/v1/auth/")
        && !path.startsWith("/actuator/health")
        && !path.startsWith("/v3/api-docs")
        && !path.startsWith("/swagger-ui")
        && !path.equals("/error");
  }

  static boolean guarded(HttpServletRequest request) {
    String path = path(request);
    String method = request.getMethod();
    if (!checked(request)) {
      return false;
    }
    if (path.equals("/api/v1/me") && "GET".equals(method)) {
      return false;
    }
    return !(path.equals("/api/v1/me/consents") && "POST".equals(method));
  }
}
