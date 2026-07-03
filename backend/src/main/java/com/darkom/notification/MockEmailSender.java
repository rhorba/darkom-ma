package com.darkom.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MockEmailSender implements EmailSender {

  private static final Logger log = LoggerFactory.getLogger(MockEmailSender.class);

  @Override
  public void send(String to, String subject, String body) {
    log.info("[mock email] to={} subject=\"{}\" body=\"{}\"", to, subject, body);
  }
}
