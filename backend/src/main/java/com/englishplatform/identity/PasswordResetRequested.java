package com.englishplatform.identity;

import java.util.UUID;

/** Xem {@link UserInvited} về {@code tokenId} và token thô. */
public record PasswordResetRequested(UUID userId, String email, UUID tokenId, String resetToken) {}
