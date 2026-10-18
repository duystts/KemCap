package com.englishplatform.shared.security;

import jakarta.servlet.Filter;

/**
 * Module nghiệp vụ đóng góp filter chạy sau khi xác thực JWT (ví dụ consent gate của identity) mà
 * không để {@code shared} phụ thuộc ngược vào module đó.
 */
public interface SecurityFilterContributor {

  Filter filter();
}
