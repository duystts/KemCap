package com.englishplatform.identity.internal;

import com.englishplatform.shared.web.BusinessException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/** Mật khẩu 8 ký tự đến 72 byte (giới hạn BCrypt), có chữ và số (contracts/rest-api.md). */
final class PasswordPolicy {

  private static final Pattern RULE = Pattern.compile("^(?=.*\\p{L})(?=.*\\d).{8,}$");

  private PasswordPolicy() {}

  static void check(String field, String password) {
    if (password == null || !RULE.matcher(password).matches()) {
      throw BusinessException.validation(field, "Mật khẩu tối thiểu 8 ký tự, có chữ và số");
    }
    if (password.getBytes(StandardCharsets.UTF_8).length > AuthService.MAX_PASSWORD_BYTES) {
      throw BusinessException.validation(field, "Mật khẩu quá dài (tối đa 72 byte)");
    }
  }
}
