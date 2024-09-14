package com.dulno.google.calendar.trigger.create;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import com.dulno.core.database.*;
import com.dulno.core.database.condition.DatabaseCondition;
import com.dulno.core.trigger.Trigger;
import com.dulno.core.trigger.TriggerContentDatabaseTable;
import com.dulno.core.trigger.TriggerInformation;
import com.dulno.core.workflow.component.input.InputComponentSelect;
import com.dulno.core.workflow.component.input.InputComponentVariable;
import com.dulno.core.workflow.component.output.OutputComponentVariable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor(staticName = "create")
public final class EventCreateTrigger implements Trigger {
  public static EventCreateTrigger create(
    InputComponentSelect googleAccountSelect,
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var contentColumns = Lists.<DatabaseColumn>newArrayList();
    contentColumns.add(DatabaseColumn.create("googleAccount", DatabaseDataType.TEXT));
    return new EventCreateTrigger(googleAccountSelect,
      TriggerContentDatabaseTable.create(databaseConnection, databaseKeyspace,
        "trigger_google_calendar_event_create", contentColumns));
  }

  private final  InputComponentSelect googleAccountSelect;
  private final TriggerContentDatabaseTable contentDatabaseTable;

  @Override
  public String type() {
    return "google-calendar-event-create-trigger";
  }

  @Override
  public TriggerInformation information() {
    return TriggerInformation.builder()
      .withName("google.calendar.trigger.event.create.name")
      .withDescription("google.calendar.trigger.event.create.description")
      .withInputVariable(InputComponentVariable.createSelect("google.calendar.trigger.event.create.input.account.name",
        "googleAccount", "google.calendar.trigger.event.create.input.account.description", googleAccountSelect))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.create.output.event.id", "eventId"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.create.output.event.name", "eventName"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.create.output.event.description", "eventDescription"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.create.output.event.location", "eventLocation"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.create.output.event.start.time", "eventStartTime"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.create.output.event.end.time", "eventEndTime"))
      .build();
  }

  @Override
  public void initialize() {
    contentDatabaseTable.createIfNotExists();
  }

  @Override
  public CompletableFuture<Void> insert(UUID triggerId, Map<String, Object> content) {
    return contentDatabaseTable.insertContent(triggerId,
      DatabaseRow.of(content.get("googleAccount")));
  }

  @Override
  public CompletableFuture<Map<String, Object>> findContent(UUID triggerId) {
    return contentDatabaseTable.findContent(triggerId).thenApply(row ->
      Map.of("googleAccount", row.findCell(1).stringValue()));
  }

  @Override
  public CompletableFuture<List<UUID>> findEntries(DatabaseCondition condition) {
    return contentDatabaseTable.findContentByCondition(condition).thenApply(
      rows -> rows.stream().map(row -> row.findCell(0).uuidValue()).toList());
  }

  @Override
  public CompletableFuture<Void> delete(UUID triggerId) {
    return contentDatabaseTable.deleteContent(triggerId);
  }
}
