package com.darkom.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** FR-5's "configurable rent due dates" - the reminder window is configurable, not hardcoded. */
@ConfigurationProperties(prefix = "app.reminders")
public class RentReminderProperties {

  private int daysBeforeDue;
  private String cron;

  public int getDaysBeforeDue() {
    return daysBeforeDue;
  }

  public void setDaysBeforeDue(int daysBeforeDue) {
    this.daysBeforeDue = daysBeforeDue;
  }

  public String getCron() {
    return cron;
  }

  public void setCron(String cron) {
    this.cron = cron;
  }
}
