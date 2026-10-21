package com.englishplatform.notification.internal;

import com.englishplatform.notification.EmailSender;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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

  /**
   * Thông tin thông báo trong hệ thống; {@code null} nếu chỉ gửi email. {@code aboutStudentId}: học
   * viên được nhắc tới khi người nhận là GV/admin (để xóa khi ẩn danh hóa).
   */
  public record InApp(String type, String title, String body, String link, UUID aboutStudentId) {

    public InApp(String type, String title, String body, String link) {
      this(type, title, body, link, null);
    }
  }

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
    UUID id = UUID.randomUUID();
    if (dispatches.insertIfAbsent(id, dedupeKey, email, userId, subject, html, clock.instant())
        == 0) {
      return; // transaction song song đã ghi cùng khóa
    }
    NotificationDispatch dispatch = dispatches.findById(id).orElseThrow();
    if (inApp != null && userId != null) {
      notifications.save(
          new Notification(
              userId,
              inApp.type(),
              inApp.title(),
              inApp.body(),
              inApp.link(),
              inApp.aboutStudentId()));
    }
    attempt(dispatch);
  }

  /**
   * Gửi lại một bản PENDING trong transaction riêng: một bản xung đột version (vừa bị ẩn danh hóa)
   * chỉ hỏng chính nó, không làm rollback các email khác đã gửi trong cùng lượt.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void retry(UUID dispatchId) {
    dispatches
        .findById(dispatchId)
        .filter(d -> d.getStatus() == NotificationDispatch.Status.PENDING)
        .ifPresent(this::attempt);
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
