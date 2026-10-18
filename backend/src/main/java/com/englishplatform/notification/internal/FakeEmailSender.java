package com.englishplatform.notification.internal;

import com.englishplatform.notification.EmailSender;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Adapter giả: lưu email trong bộ nhớ để test đọc lại; {@link #failNext} giả lập SMTP lỗi. */
@Component
@ConditionalOnProperty(name = "app.providers.email", havingValue = "fake")
public class FakeEmailSender implements EmailSender {

  private final List<EmailMessage> sent = new CopyOnWriteArrayList<>();
  private final AtomicInteger failuresLeft = new AtomicInteger();

  @Override
  public void send(EmailMessage message) {
    if (failuresLeft.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
      throw new IllegalStateException("SMTP giả lập lỗi");
    }
    sent.add(message);
  }

  /** {@code times} lần gửi tiếp theo sẽ ném lỗi. */
  public void failNext(int times) {
    failuresLeft.set(times);
  }

  public List<EmailMessage> sent() {
    return List.copyOf(sent);
  }

  public void clear() {
    sent.clear();
    failuresLeft.set(0);
  }
}
