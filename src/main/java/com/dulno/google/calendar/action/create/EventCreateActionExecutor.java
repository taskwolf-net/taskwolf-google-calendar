package com.dulno.google.calendar.action.create;

import com.datastax.oss.driver.shaded.guava.common.collect.Maps;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import lombok.AllArgsConstructor;
import com.dulno.workflow.action.ActionExecutor;
import com.dulno.workflow.action.ActionResult;
import com.dulno.workflow.placeholder.PlaceholderDissolve;
import com.dulno.google.GoogleConfiguration;
import com.dulno.google.account.GoogleAccountDatabaseTable;
import com.dulno.google.account.GoogleCredential;
import com.dulno.google.calendar.GoogleCalendarEventTime;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor(staticName = "create")
public final class EventCreateActionExecutor implements ActionExecutor {
  private final GoogleConfiguration configuration;
  private final GoogleAccountDatabaseTable googleAccountDatabaseTable;
  private final String googleAccount;
  private String eventTitle;
  private String eventDescription;
  private String eventLocation;
  private String eventStartTime;
  private String eventEndTime;

  @Override
  public CompletableFuture<ActionResult> execute(Map<String, Object> information) {
    var dissolve = PlaceholderDissolve.create(information);
    eventTitle = dissolve.dissolve(eventTitle);
    eventDescription = dissolve.dissolve(eventDescription);
    eventLocation = dissolve.dissolve(eventLocation);
    eventStartTime = dissolve.dissolve(eventStartTime);
    eventEndTime = dissolve.dissolve(eventEndTime);
    var eventStartDateTime = GoogleCalendarEventTime.of(eventStartTime).convertToDateTime();
    if (eventStartDateTime.isEmpty()) {
      return ActionResult.futureFailure("google.calendar.action.event.create.failure.wrong.start.time.format");
    }
    var eventEndDateTime = GoogleCalendarEventTime.of(eventEndTime).convertToDateTime();
    if (eventEndDateTime.isEmpty()) {
      return ActionResult.futureFailure("google.calendar.action.event.create.failure.wrong.end.time.format");
    }
    var futureResponse = new CompletableFuture<ActionResult>();
    googleAccountDatabaseTable.findAccount(googleAccount).thenApplyAsync(account ->
      futureResponse.complete(ActionResult.success(buildInformation(
        insertEvent(GoogleCredential.of(configuration.clientId(),
            configuration.clientSecret(), account).buildCredential(),
          eventStartDateTime.get(), eventEndDateTime.get())))));
    return futureResponse;
  }

  private Event insertEvent(
    Credential credential, DateTime eventStartDateTime,
    DateTime eventEndDateTime
  ) {
    try {
      var service = new Calendar.Builder(GoogleNetHttpTransport.newTrustedTransport(),
        new GsonFactory(), credential)
        .setApplicationName("Dulno")
        .build();
      var event = createEvent(eventStartDateTime, eventEndDateTime);
      return service.events().insert("primary", event).execute();
    } catch (Exception exception) {
      exception.printStackTrace();
      return null;
    }
  }

  private Event createEvent(DateTime eventStartDateTime, DateTime eventEndDateTime) {
    var event = new Event()
      .setSummary(eventTitle)
      .setDescription(eventDescription)
      .setLocation(eventLocation);
    var start = new EventDateTime()
      .setDateTime(eventStartDateTime);
    event.setStart(start);
    var end = new EventDateTime()
      .setDateTime(eventEndDateTime);
    event.setEnd(end);
    return event;
  }

  private Map<String, Object> buildInformation(Event event) {
    if (event == null) {
      return Maps.newHashMap();
    }
    var information = Maps.<String, Object>newHashMap();
    information.put("eventId", event.getId());
    information.put("eventTitle", eventTitle);
    information.put("eventDescription", eventDescription);
    information.put("eventLocation", eventLocation);
    information.put("eventStartTime", eventStartTime);
    information.put("eventEndTime", eventEndTime);
    return information;
  }
}
