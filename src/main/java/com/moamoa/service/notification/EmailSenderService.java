package com.moamoa.service.notification;

import com.moamoa.domain.contest.Contest;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailSenderService {
  private final JavaMailSender sender;
  private final TemplateEngine templates;
  private final String from, admin, baseUrl;

  public EmailSenderService(
      JavaMailSender sender,
      TemplateEngine templates,
      @Value("${app.mail-from}") String from,
      @Value("${app.admin-email}") String admin,
      @Value("${app.base-url}") String baseUrl) {
    this.sender = sender;
    this.templates = templates;
    this.from = from;
    this.admin = admin;
    this.baseUrl = baseUrl;
  }

  public void sendReset(String to, String url) {
    var ctx = new Context(Locale.KOREAN);
    ctx.setVariable("url", url);
    send(to, "[moamoa] 비밀번호 재설정", templates.process("mail/reset", ctx));
  }

  public void sendEvents(String to, String subject, List<Contest> contests) {
    var ctx = new Context(Locale.KOREAN);
    ctx.setVariable("contests", contests);
    ctx.setVariable("baseUrl", baseUrl);
    send(to, subject, templates.process("mail/events", ctx));
  }

  public void sendAdmin(String subject, String message) {
    var ctx = new Context(Locale.KOREAN);
    ctx.setVariable("message", message);
    send(admin, subject, templates.process("mail/admin", ctx));
  }

  private void send(String to, String subject, String html) {
    try {
      var mime = sender.createMimeMessage();
      var helper = new MimeMessageHelper(mime, "UTF-8");
      helper.setFrom(from);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(html, true);
      sender.send(mime);
    } catch (jakarta.mail.MessagingException ex) {
      throw new IllegalStateException("Cannot build mail");
    }
  }
}
