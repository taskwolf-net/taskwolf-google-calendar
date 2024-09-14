package com.dulno.google.calendar;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.common.collect.Lists;
import com.dulno.google.GoogleAccountLink;
import com.dulno.google.GoogleConfiguration;
import com.dulno.google.account.GoogleAccount;
import com.dulno.google.account.GoogleAccountDatabaseTable;
import com.dulno.google.account.GoogleCredential;
import com.dulno.google.account.GoogleUserAccountDatabaseTable;

import java.util.List;
import java.util.UUID;

public final class GoogleCalendarAccountLink extends GoogleAccountLink {
  public static GoogleCalendarAccountLink create(
    GoogleConfiguration googleConfiguration,
    GoogleAccountDatabaseTable googleAccountDatabaseTable,
    GoogleUserAccountDatabaseTable googleUserAccountDatabaseTable,
    GoogleCalendarEventDatabaseTable googleCalendarEventDatabaseTable
  ) {
    return new GoogleCalendarAccountLink(googleConfiguration, googleAccountDatabaseTable,
      googleUserAccountDatabaseTable, googleCalendarEventDatabaseTable);
  }

  private final GoogleCalendarEventDatabaseTable googleCalendarEventDatabaseTable;

  private GoogleCalendarAccountLink(
    GoogleConfiguration googleConfiguration,
    GoogleAccountDatabaseTable googleAccountDatabaseTable,
    GoogleUserAccountDatabaseTable googleUserAccountDatabaseTable,
    GoogleCalendarEventDatabaseTable googleCalendarEventDatabaseTable
  ) {
    super(googleConfiguration, googleAccountDatabaseTable,
      googleUserAccountDatabaseTable, "google-calendar",
      Lists.newArrayList("https://www.googleapis.com/auth/calendar",
        "https://www.googleapis.com/auth/calendar.events"));
    this.googleCalendarEventDatabaseTable = googleCalendarEventDatabaseTable;
  }

  @Override
  public void registerAccount(UUID id, String identifier) throws Exception {
    super.registerAccount(id, identifier);
    googleAccountDatabaseTable.findAccount(identifier)
      .thenApply(this::createCalendarService).thenApplyAsync(this::listCalendarEvents)
      .thenAccept(events -> setupCalendarEvents(identifier, events));
  }

  private Calendar createCalendarService(GoogleAccount account) {
    try {
      var credential = GoogleCredential.of(googleConfiguration.clientId(),
        googleConfiguration.clientSecret(), account).buildCredential();
      return new Calendar.Builder(GoogleNetHttpTransport.newTrustedTransport(),
        new GsonFactory(), credential)
        .setApplicationName("Dulno")
        .build();
    } catch (Exception ignored) {
      return null;
    }
  }

  private List<Event> listCalendarEvents(Calendar service) {
    var now = new DateTime(System.currentTimeMillis());
    try {
      return service.events().list("primary")
        .setTimeMin(now)
        .setSingleEvents(true)
        .setShowDeleted(true)
        .execute().getItems();
    } catch (Exception exception) {
      exception.printStackTrace();
      return Lists.newArrayList();
    }
  }

  private void setupCalendarEvents(String accountId, List<Event> events) {
    googleCalendarEventDatabaseTable.insertCalendarEvents(accountId,
      events.stream().filter(event -> !event.getStatus().equals("cancelled"))
        .map(Event::getId).toList());
  }

  @Override
  public void removeAccount(UUID id, String identifier) {
    super.removeAccount(id, identifier);
    googleCalendarEventDatabaseTable.deleteCalendarEvents(identifier);
  }
}
