package com.englishplatform.notification.internal;

import com.englishplatform.notification.EmailSender;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Render email Thymeleaf ({@code templates/email/*.html}), ghi {@code notification_dispatch} (bỏ
 * qua nếu {@code dedupe_key} đã có), tạo thông báo trong hệ thống và thử gửi ngay. Lỗi gửi chỉ ghi
 * lại để {@link RetryDispatchJob} thử tiếp — không làm hỏng nghiệp vụ đã commit.
 */
@Service
public class DispatchService {

  private static final Logger log = LoggerFactory.getLogger(DispatchService.class);

  private final NotificationDispatchRepository dispatches;
  private final NotificationRepository notifications;
  private final EmailSender emailSender;
  private final TemplateEngine templateEngine;
  private final Clock clock;

  DispatchService(
      NotificationDispatchRepository dispatches,
      NotificationRepository notifications,
      EmailSender emailSender,
      TemplateEngine templateEngine,
      Clock clock) {
    this.dispatches = dispatches;
    this.notifications = notifications;
    this.emailSender = emailSender;
    this.templateEngine = templateEngine;
    this.clock = clock;
  }

  /** Thông tin thông báo trong hệ thống; {@code null} nếu chỉ gửi email. */
  public record InApp(String type, String title, String body, String link) {}

  @Transactional
  public void notify(
      String dedupeKey,
      UUID userId,
      String email,
      String subject,
      String templateName,
      Map<String, Object> model,
      InApp inApp) {
    if (dispatches.existsByDedupeKey(dedupeKey)) {
      return;
    }
    Context context = new Context();
    context.setVariables(model);
    context.setVariable("subject", subject);
    String html = templateEngine.process("email/" + templateName, context);
    NotificationDispatch dispatch =
        dispatches.save(new NotificationDispatch(dedupeKey, email, subject, html, clock.instant()));
    if (inApp != null && userId != null) {
      notifications.save(
          new Notification(userId, inApp.type(), inApp.title(), inApp.body(), inApp.link()));
    }
    attempt(dispatch);
  }

  /** Gửi một bản ghi PENDING; cập nhật SENT hoặc lịch thử lại. */
  void attempt(NotificationDispatch dispatch) {
    try {
      emailSender.send(
          new EmailSender.EmailMessage(
              dispatch.getRecipient(), dispatch.getSubject(), dispatch.getBodyHtml()));
      dispatch.markSent(clock.instant());
    } catch (RuntimeException e) {
      log.warn("Gửi email {} lỗi: {}", dispatch.getDedupeKey(), e.getMessage());
      dispatch.markFailed(clock.instant(), e.getMessage());
    }
  }
}
