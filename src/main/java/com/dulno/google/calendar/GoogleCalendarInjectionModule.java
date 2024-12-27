package com.dulno.google.calendar;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.RequiredArgsConstructor;
import com.dulno.core.database.DatabaseConnection;
import com.dulno.core.database.DatabaseKeyspace;
import com.dulno.google.account.GoogleAccountDatabaseTable;
import com.dulno.google.account.GoogleUserAccountDatabaseTable;

@RequiredArgsConstructor(staticName = "create")
public final class GoogleCalendarInjectionModule extends AbstractModule {
  @Provides
  @Singleton
  @Named("googleCalendarAccount")
  GoogleAccountDatabaseTable provideGoogleAccountDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var googleAccountDatabaseTable = GoogleAccountDatabaseTable.create(
      connection, keyspace, "google_calendar_account");
    googleAccountDatabaseTable.createIfNotExists();
    return googleAccountDatabaseTable;
  }

  @Provides
  @Singleton
  @Named("googleCalendarUserAccount")
  GoogleUserAccountDatabaseTable provideGoogleUserAccountDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var googleUserAccountDatabaseTable = GoogleUserAccountDatabaseTable.create(
      connection, keyspace, "google_calendar_user_account");
    googleUserAccountDatabaseTable.createIfNotExists();
    googleUserAccountDatabaseTable.createIndexIfNotExists("accounts");
    return googleUserAccountDatabaseTable;
  }

  @Provides
  @Singleton
  GoogleCalendarEventDatabaseTable provideGoogleCalendarEventDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var calendarEventDatabaseTable = GoogleCalendarEventDatabaseTable.create(
      connection, keyspace);
    calendarEventDatabaseTable.createIfNotExists();
    return calendarEventDatabaseTable;
  }
}
