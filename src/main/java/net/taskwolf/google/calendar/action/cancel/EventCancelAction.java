package net.taskwolf.google.calendar.action.cancel;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import net.taskwolf.core.action.Action;
import net.taskwolf.core.action.ActionContentDatabaseTable;
import net.taskwolf.core.action.ActionInformation;
import net.taskwolf.core.database.*;
import net.taskwolf.core.workflow.component.input.InputComponentDataType;
import net.taskwolf.core.workflow.component.input.InputComponentSelect;
import net.taskwolf.core.workflow.component.input.InputComponentVariable;
import net.taskwolf.core.workflow.component.output.OutputComponentVariable;
import net.taskwolf.google.GoogleConfiguration;
import net.taskwolf.google.account.GoogleAccountDatabaseTable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor(staticName = "create")
public final class EventCancelAction implements Action<EventCancelActionExecutor> {
  public static EventCancelAction create(
    GoogleConfiguration googleConfiguration,
    GoogleAccountDatabaseTable googleAccountDatabaseTable,
    InputComponentSelect googleAccountSelect,
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var contentColumns = Lists.<DatabaseColumn>newArrayList();
    contentColumns.add(DatabaseColumn.create("googleAccount", DatabaseDataType.TEXT));
    contentColumns.add(DatabaseColumn.create("eventId", DatabaseDataType.TEXT));
    return new EventCancelAction(googleConfiguration, googleAccountDatabaseTable,
      googleAccountSelect, ActionContentDatabaseTable.create(databaseConnection,
      databaseKeyspace, "action_google_calendar_event_cancel", contentColumns));
  }

  private final GoogleConfiguration googleConfiguration;
  private final GoogleAccountDatabaseTable googleAccountDatabaseTable;
  private final InputComponentSelect googleAccountSelect;
  private final ActionContentDatabaseTable contentDatabaseTable;

  @Override
  public String type() {
    return "google-calendar-event-cancel-action";
  }

  @Override
  public ActionInformation information() {
    return ActionInformation.builder()
      .withName("google.calendar.action.event.cancel.name")
      .withDescription("google.calendar.action.event.cancel.description")
      .withInputVariable(InputComponentVariable.createSelect("google.calendar.action.event.cancel.input.account.name",
        "googleAccount", "google.calendar.action.event.cancel.input.account.description", googleAccountSelect))
      .withInputVariable(InputComponentVariable.createRequired("google.calendar.action.event.cancel.input.event.id.name",
        "eventId", "google.calendar.action.event.cancel.input.event.id.description", InputComponentDataType.TEXT))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.action.event.cancel.output.event.id", "eventId"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.action.event.cancel.output.event.title", "eventTitle"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.action.event.cancel.output.event.description", "eventDescription"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.action.event.cancel.output.event.location", "eventLocation"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.action.event.cancel.output.event.start.time", "eventStartTime"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.action.event.cancel.output.event.end.time", "eventEndTime"))
      .build();
  }

  @Override
  public void initialize() {
    contentDatabaseTable.createIfNotExists();
  }

  @Override
  public CompletableFuture<Void> insert(UUID actionId, Map<String, Object> content) {
    return contentDatabaseTable.insertContent(actionId, DatabaseRow.of(
      content.get("googleAccount"), content.get("eventId")));
  }

  @Override
  public CompletableFuture<Map<String, Object>> findContent(UUID triggerId) {
    return contentDatabaseTable.findContent(triggerId).thenApply(row ->
      Map.of("googleAccount", row.findCell(1).stringValue(),
        "eventId", row.findCell(2).stringValue()));
  }

  @Override
  public CompletableFuture<EventCancelActionExecutor> build(UUID actionId) {
    return contentDatabaseTable.findContent(actionId).thenApply(content ->
      EventCancelActionExecutor.create(googleConfiguration,
        googleAccountDatabaseTable, content.findCell(1).stringValue(),
        content.findCell(2).stringValue()));
  }

  @Override
  public CompletableFuture<Void> delete(UUID actionId) {
    return contentDatabaseTable.deleteContent(actionId);
  }
}
