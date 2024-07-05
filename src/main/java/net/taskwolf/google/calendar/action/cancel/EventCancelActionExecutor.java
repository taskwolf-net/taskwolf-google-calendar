package net.taskwolf.google.calendar.action.cancel;

import com.datastax.oss.driver.shaded.guava.common.collect.Maps;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import lombok.AllArgsConstructor;
import net.taskwolf.core.action.ActionExecutor;
import net.taskwolf.core.action.ActionResult;
import net.taskwolf.core.workflow.placeholder.PlaceholderDissolve;
import net.taskwolf.google.GoogleConfiguration;
import net.taskwolf.google.account.GoogleAccountDatabaseTable;
import net.taskwolf.google.account.GoogleCredential;
import net.taskwolf.google.calendar.GoogleCalendarEventTime;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor(staticName = "create")
public final class EventCancelActionExecutor implements ActionExecutor {
  private final GoogleConfiguration configuration;
  private final GoogleAccountDatabaseTable googleAccountDatabaseTable;
  private final String googleAccount;
  private String eventId;

  @Override
  public CompletableFuture<ActionResult> execute(Map<String, Object> information) {
    var dissolve = PlaceholderDissolve.create(information);
    eventId = dissolve.dissolve(eventId);
    var futureResponse = new CompletableFuture<ActionResult>();
    googleAccountDatabaseTable.findAccount(googleAccount).thenAcceptAsync(account ->
      futureResponse.complete(ActionResult.success(buildInformation(
        cancelEvent(GoogleCredential.of(configuration.clientId(),
          configuration.clientSecret(), account).buildCredential())))));
    return futureResponse;
  }

  private Event cancelEvent(Credential credential) {
    try {
      var service = new Calendar.Builder(GoogleNetHttpTransport.newTrustedTransport(),
        new GsonFactory(), credential)
        .setApplicationName("Taskwolf")
        .build();
      var event = service.events().get("primary", eventId).execute();
      event.setStatus("cancelled");
      service.events().update("primary", eventId, event).execute();
      return event;
    } catch (Exception exception) {
      exception.printStackTrace();
      return null;
    }
  }

  private Map<String, Object> buildInformation(Event event) {
    if (event == null) {
      return Maps.newHashMap();
    }
    var summary = event.getSummary() == null ? "" : event.getSummary();
    var description = event.getDescription() == null ? "" : event.getDescription();
    var location = event.getLocation() == null ? "" : event.getLocation();
    var information = Maps.<String, Object>newHashMap();
    information.put("eventId", event.getId());
    information.put("eventName", summary);
    information.put("eventDescription", description);
    information.put("eventLocation", location);
    information.put("eventStartTime", GoogleCalendarEventTime.of(
      event.getStart().getDateTime()).convertToFormattedTime().get());
    information.put("eventEndTime",  GoogleCalendarEventTime.of(
      event.getEnd().getDateTime()).convertToFormattedTime().get());
    return information;
  }
}
