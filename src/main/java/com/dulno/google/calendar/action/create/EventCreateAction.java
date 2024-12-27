package com.dulno.google.calendar.action.create;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import com.dulno.workflow.action.Action;
import com.dulno.workflow.action.ActionContentDatabaseTable;
import com.dulno.workflow.action.ActionInformation;
import com.dulno.core.database.*;
import com.dulno.workflow.component.input.InputComponentDataType;
import com.dulno.workflow.component.input.InputComponentSelect;
import com.dulno.workflow.component.input.InputComponentVariable;
import com.dulno.workflow.component.output.OutputComponentVariable;
import com.dulno.google.GoogleConfiguration;
import com.dulno.google.account.GoogleAccountDatabaseTable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor(staticName = "create")
public final class EventCreateAction implements Action<EventCreateActionExecutor> {
  public static EventCreateAction create(
    GoogleConfiguration googleConfiguration,
    GoogleAccountDatabaseTable googleAccountDatabaseTable,
    InputComponentSelect googleAccountSelect,
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var contentColumns = Lists.<DatabaseColumn>newArrayList();
    contentColumns.add(DatabaseColumn.create("googleAccount", DatabaseDataType.TEXT));
    contentColumns.add(DatabaseColumn.create("eventTitle", DatabaseDataType.TEXT));
    contentColumns.add(DatabaseColumn.create("eventDescription", DatabaseDataType.TEXT));
    contentColumns.add(DatabaseColumn.create("eventLocation", DatabaseDataType.TEXT));
    contentColumns.add(DatabaseColumn.create("eventStartTime", DatabaseDataType.TEXT));
    contentColumns.add(DatabaseColumn.create("eventEndTime", DatabaseDataType.TEXT));
    return new EventCreateAction(googleConfiguration, googleAccountDatabaseTable,
      googleAccountSelect, ActionContentDatabaseTable.create(databaseConnection,
        databaseKeyspace, "action_google_calendar_event_create", contentColumns));
  }

  private final GoogleConfiguration googleConfiguration;
  private final GoogleAccountDatabaseTable googleAccountDatabaseTable;
  private final InputComponentSelect googleAccountSelect;
  private final ActionContentDatabaseTable contentDatabaseTable;

  @Override
  public String type() {
    return "google-calendar-event-create-action";
  }

  @Override
  public ActionInformation information() {
    return ActionInformation.builder()
      .withName("google.calendar.action.event.create.name")
      .withDescription("google.calendar.action.event.create.description")
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

  @Override
  public void initialize() {
    contentDatabaseTable.createIfNotExists();
  }

  @Override
  public CompletableFuture<Void> insert(UUID actionId, Map<String, Object> content) {
    return contentDatabaseTable.insertContent(actionId, DatabaseRow.of(
      content.get("googleAccount"), content.get("eventTitle"),
      content.get("eventDescription"), content.get("eventLocation"),
        content.get("eventStartTime"), content.get("eventEndTime")));
  }

  @Override
  public CompletableFuture<Map<String, Object>> findContent(UUID triggerId) {
    return contentDatabaseTable.findContent(triggerId).thenApply(row ->
      Map.of("googleAccount", row.findCell(1).stringValue(),
        "eventTitle", row.findCell(2).stringValue(),
        "eventDescription", row.findCell(3).stringValue(),
        "eventLocation", row.findCell(4).stringValue(),
        "eventStartTime", row.findCell(5).stringValue(),
        "eventEndTime", row.findCell(6).stringValue()));
  }

  @Override
  public CompletableFuture<EventCreateActionExecutor> build(UUID actionId) {
    return contentDatabaseTable.findContent(actionId).thenApply(content ->
      EventCreateActionExecutor.create(googleConfiguration,
        googleAccountDatabaseTable, content.findCell(1).stringValue(),
        content.findCell(2).stringValue(), content.findCell(3).stringValue(),
        content.findCell(4).stringValue(), content.findCell(5).stringValue(),
        content.findCell(6).stringValue()));
  }

  @Override
  public CompletableFuture<Void> delete(UUID actionId) {
    return contentDatabaseTable.deleteContent(actionId);
  }
}
