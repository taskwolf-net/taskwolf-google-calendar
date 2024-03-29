package net.taskwolf.google.calendar;

import com.google.common.collect.Lists;
import net.taskwolf.core.database.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class GoogleCalendarEventDatabaseTable extends DatabaseTable {
  private static final String TABLE_NAME = "google_calendar_event";

  public static GoogleCalendarEventDatabaseTable create(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var columns = Lists.<DatabaseColumn>newArrayList();
    columns.add(DatabaseColumn.create("account", DatabaseDataType.TEXT,
      DatabaseColumn.Type.PRIMARY_KEY));
    columns.add(DatabaseListColumn.create("events", DatabaseDataType.TEXT));
    return new GoogleCalendarEventDatabaseTable(connection, keyspace, TABLE_NAME, columns);
  }

  private GoogleCalendarEventDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String name,
    List<DatabaseColumn> columns
  ) {
    super(connection, keyspace, name, columns);
  }

  public void insertCalendarEvents(String accountId, List<String> eventIds) {
    insert(DatabaseRow.of(accountId, eventIds));
  }

  public void addCalendarEvent(String accountId, String eventId) {
    selectRow(DatabaseCell.create(accountId)).thenAccept(row ->
      addCalendarEvent(accountId, eventId, row));
  }

  private void addCalendarEvent(String accountId, String eventId, DatabaseRow row) {
    var eventIds = row.findCell(1).<String>listValue();
    eventIds.add(eventId);
    updateCalendarEvents(accountId, eventIds);
  }

  public void removeCalendarEvent(String accountId, String eventId) {
    selectRow(DatabaseCell.create(accountId)).thenAccept(row ->
      removeCalendarEvent(accountId, eventId, row));
  }

  private void removeCalendarEvent(String accountId, String eventId, DatabaseRow row) {
    var eventIds = row.findCell(1).<String>listValue();
    if (eventIds.size() == 1) {
      deleteCalendarEvents(accountId);
      return;
    }
    eventIds.remove(eventId);
    updateCalendarEvents(accountId, eventIds);
  }

  private void updateCalendarEvents(String accountId, List<String> eventIds) {
    update(DatabaseCell.create(accountId), DatabaseRow.of(accountId, eventIds));
  }

  public void deleteCalendarEvents(String accountId) {
    delete(DatabaseCell.create(accountId));
  }

  public CompletableFuture<Boolean> calendarEventsExists(String accountId) {
    return exists(DatabaseCell.create(accountId));
  }

  public CompletableFuture<List<String>> findCalendarEvents(String accountId) {
    return selectRow(DatabaseCell.create(accountId)).thenApply(row ->
      row.findCell(1).listValue());
  }
}