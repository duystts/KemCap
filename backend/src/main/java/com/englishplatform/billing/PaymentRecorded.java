package com.englishplatform.billing;

import java.util.UUID;

/** Catalog nghe event này để kích hoạt ghi danh ở lần thanh toán đầu tiên (FR-025). */
public record PaymentRecorded(
    UUID enrollmentId, UUID paymentId, long amountVnd, boolean isFirstPayment) {}
