package com.dulno.google.calendar;

import com.google.common.collect.Lists;
import com.dulno.core.database.*;

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

  public void updateCalendarEvents(String accountId, List<String> eventIds) {
    update(accountId, DatabaseRow.of(accountId, eventIds));
  }

  public void deleteCalendarEvents(String accountId) {
    delete(accountId);
  }

  public CompletableFuture<Boolean> calendarEventsExists(String accountId) {
    return exists(accountId);
  }

  public CompletableFuture<List<String>> findCalendarEvents(String accountId) {
    return selectRow(accountId).thenApply(row -> row.findCell(1).listValue());
  }
}