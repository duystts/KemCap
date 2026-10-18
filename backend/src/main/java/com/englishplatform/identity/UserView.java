package com.englishplatform.identity;

import java.util.UUID;

/** Thông tin người dùng mà module khác được đọc qua {@link UserDirectory}. */
public record UserView(
    UUID id, String email, String fullName, Role role, UserStatus status, String timezone) {}
