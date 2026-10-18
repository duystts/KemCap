package com.englishplatform.identity;

import java.util.UUID;

/** Mỗi module tự xóa dữ liệu định danh của học viên trong listener idempotent (R16). */
public record StudentAnonymized(UUID studentId, UUID requestId) {}
