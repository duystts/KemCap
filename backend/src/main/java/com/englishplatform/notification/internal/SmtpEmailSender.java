package com.englishplatform.notification.internal;

import com.englishplatform.notification.EmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.providers.email", havingValue = "smtp", matchIfMissing = true)
class SmtpEmailSender implements EmailSender {

  private final JavaMailSender mailSender;
  private final String from;

  SmtpEmailSender(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
    this.mailSender = mailSender;
    this.from = from;
  }

  @Override
  public void send(EmailMessage message) {
    try {
      MimeMessage mime = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
      helper.setFrom(from);
      helper.setTo(message.to());
      helper.setSubject(message.subject());
      helper.setText(message.html(), true);
      mailSender.send(mime);
    } catch (MessagingException e) {
      throw new IllegalStateException("Không tạo được email", e);
    }
  }
}
