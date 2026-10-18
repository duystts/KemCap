package com.englishplatform.identity;

import java.util.UUID;

/**
 * {@code inviteToken} là token thô (chỉ để dựng link email); {@code tokenId} là id bản ghi {@code
 * auth_token}, dùng làm khóa chống trùng. Publication bị xóa khi listener xong (NFR-SEC-03).
 */
public record UserInvited(
    UUID userId, String email, String fullName, Role role, UUID tokenId, String inviteToken) {}
