package com.englishplatform.notification;

/**
 * Port gửi email (constitution II). Adapter chọn bằng {@code app.providers.email}: {@code smtp}
 * (Mailpit/Brevo/Gmail) hoặc {@code fake} (lưu bộ nhớ, dùng cho test).
 */
public interface EmailSender {

  void send(EmailMessage message);

  record EmailMessage(String to, String subject, String html) {}
}
