package net.taskwolf.google.calendar.action;

import com.datastax.oss.driver.shaded.guava.common.collect.Maps;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import lombok.AllArgsConstructor;
import net.taskwolf.core.action.Action;
import net.taskwolf.core.action.ActionInformation;
import net.taskwolf.core.action.ActionResult;
import net.taskwolf.core.workflow.component.input.InputComponentDataType;
import net.taskwolf.core.workflow.component.input.InputComponentSelect;
import net.taskwolf.core.workflow.component.input.InputComponentVariable;
import net.taskwolf.core.workflow.component.output.OutputComponentVariable;
import net.taskwolf.core.workflow.placeholder.PlaceholderDissolve;
import net.taskwolf.google.GoogleConfiguration;
import net.taskwolf.google.account.GoogleAccountDatabaseTable;
import net.taskwolf.google.account.GoogleCredential;
import net.taskwolf.google.calendar.GoogleCalendarEventTime;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor(staticName = "create")
public final class EventCreateAction implements Action {
  public static ActionInformation information(
    InputComponentSelect googleAccountSelect
  ) {
    return ActionInformation.builder()
      .withName("google.calendar.action.event.create.name")
      .withDescription("google.calendar.action.event.create.description")
      .withIdentifier("google-calendar-event-create-action")
      .withInputVariable(InputComponentVariable.createSelect("google.calendar.action.event.create.input.account.name",
        "googleAccount", "google.calendar.action.event.create.input.account.description", googleAccountSelect))
      .withInputVariable(InputComponentVariable.createRequired("google.calendar.action.event.create.input.event.title.name",
        "eventTitle", "google.calendar.action.event.create.input.event.title.description", InputComponentDataType.TEXT))
      .withInputVariable(InputComponentVariable.createRequired("google.calendar.action.event.create.input.event.description.name",
        "eventDescription", "google.calendar.action.event.create.input.event.description.description", InputComponentDataType.TEXT))
      .withInputVariable(InputComponentVariable.createRequired("google.calendar.action.event.create.input.event.location.name",
        "eventLocation", "google.calendar.action.event.create.input.event.location.description", InputComponentDataType.TEXT))
      .withInputVariable(InputComponentVariable.createRequired("google.calendar.action.event.create.input.event.start.time.name",
        "eventStartTime", "google.calendar.action.event.create.input.event.start.time.description", InputComponentDataType.TEXT))
      .withInputVariable(InputComponentVariable.createRequired("google.calendar.action.event.create.input.event.end.time.name",
        "eventEndTime", "google.calendar.action.event.create.input.event.end.time.description", InputComponentDataType.TEXT))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.action.event.create.output.event.id", "eventId"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.action.event.create.output.event.title", "eventTitle"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.action.event.create.output.event.description", "eventDescription"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.action.event.create.output.event.location", "eventLocation"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.action.event.create.output.event.start.time", "eventStartTime"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.action.event.create.output.event.end.time", "eventEndTime"))
      .build();
  }

  public static EventCreateAction of(
    GoogleConfiguration configuration,
    GoogleAccountDatabaseTable googleAccountDatabaseTable, JSONObject content
  ) {
    return create(configuration, googleAccountDatabaseTable,
      content.getString("googleAccount"), content.getString("eventTitle"),
      content.getString("eventDescription"), content.getString("eventLocation"),
      content.getString("eventStartTime"), content.getString("eventEndTime"));
  }

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
    googleAccountDatabaseTable.findAccount(googleAccount).thenAccept(account ->
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
        .setApplicationName("Taskwolf")
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
