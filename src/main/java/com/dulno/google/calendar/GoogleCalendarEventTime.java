package com.dulno.google.calendar;

import com.google.api.client.util.DateTime;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Optional;
import java.util.TimeZone;

public final class GoogleCalendarEventTime {
  public static GoogleCalendarEventTime of(DateTime dateTime) {
    return new GoogleCalendarEventTime(dateTime);
  }

  public static GoogleCalendarEventTime of(String formattedTime) {
    return new GoogleCalendarEventTime(formattedTime);
  }

  private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
  private DateTime dateTime;
  private String formattedTime;

  private GoogleCalendarEventTime(DateTime dateTime) {
    this.dateTime = dateTime;
    dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
  }

  private GoogleCalendarEventTime(String formattedTime) {
    this.formattedTime = formattedTime;
    dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
  }

  public Optional<String> convertToFormattedTime() {
    if (dateTime == null) {
      return Optional.empty();
    }
    var calendar = Calendar.getInstance();
    calendar.setTimeInMillis(dateTime.getValue());
    return Optional.of(dateFormat.format(calendar.getTime()));
  }

  public Optional<DateTime> convertToDateTime() {
    if (formattedTime == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(new DateTime(dateFormat.parse(formattedTime).getTime()));
    } catch (Exception ignored) {
      return Optional.empty();
    }
  }
}
