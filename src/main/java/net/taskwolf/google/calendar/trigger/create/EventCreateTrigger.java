package net.taskwolf.google.calendar.trigger.create;

import net.taskwolf.google.account.GoogleUserAccountDatabaseTable;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import net.taskwolf.core.database.*;
import net.taskwolf.core.database.condition.DatabaseCondition;
import net.taskwolf.workflow.trigger.Trigger;
import net.taskwolf.workflow.trigger.TriggerContentDatabaseTable;
import net.taskwolf.workflow.trigger.TriggerInformation;
import net.taskwolf.workflow.component.input.InputComponentSelect;
import net.taskwolf.workflow.component.input.InputComponentVariable;
import net.taskwolf.workflow.component.output.OutputComponentVariable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor(staticName = "create")
public final class EventCreateTrigger implements Trigger {
  public static EventCreateTrigger create(
    GoogleUserAccountDatabaseTable googleUserAccountDatabaseTable,
    InputComponentSelect googleAccountSelect,
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var contentColumns = Lists.<DatabaseColumn>newArrayList();
    contentColumns.add(DatabaseColumn.create("owner", DatabaseDataType.UUID));
    contentColumns.add(DatabaseColumn.create("googleAccount", DatabaseDataType.TEXT));
    return new EventCreateTrigger(googleUserAccountDatabaseTable, googleAccountSelect,
      TriggerContentDatabaseTable.create(databaseConnection, databaseKeyspace,
        "trigger_google_calendar_event_create", contentColumns));
  }

  private final GoogleUserAccountDatabaseTable googleUserAccountDatabaseTable;
  private final InputComponentSelect googleAccountSelect;
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
  public CompletableFuture<Void> insert(
    UUID triggerId, UUID ownerId, Map<String, Object> content
  ) {
    return contentDatabaseTable.insertContent(triggerId,
      DatabaseRow.of(ownerId, content.get("googleAccount")));
  }

  @Override
  public CompletableFuture<Boolean> checkExecution(UUID triggerId) {
    return contentDatabaseTable.findContent(triggerId)
      .thenCompose(row -> googleUserAccountDatabaseTable.accountExists(
          row.findCell(1).uuidValue())
        .thenCompose(exists -> checkExecution(row.findCell(1).uuidValue(),
          row.findCell(2).stringValue(), exists)));
  }

  public CompletableFuture<Boolean> checkExecution(
    UUID ownerId, String googleAccountId, boolean hasAccounts
  ) {
    if (!hasAccounts) {
      return CompletableFuture.completedFuture(false);
    }
    return googleUserAccountDatabaseTable.findAccounts(ownerId)
      .thenApply(accounts -> accounts.contains(googleAccountId));
  }

  @Override
  public CompletableFuture<Map<String, Object>> findContent(UUID triggerId) {
    return contentDatabaseTable.findContent(triggerId).thenApply(row ->
      Map.of("googleAccount", row.findCell(2).stringValue()));
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
