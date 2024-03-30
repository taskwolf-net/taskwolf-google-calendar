package net.taskwolf.google.calendar;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import net.taskwolf.core.database.DatabaseConnection;
import net.taskwolf.core.database.DatabaseKeyspace;
import net.taskwolf.google.account.GoogleAccountDatabaseTable;
import net.taskwolf.google.account.GoogleUserAccountDatabaseTable;

@RequiredArgsConstructor(staticName = "create")
public final class GoogleCalendarInjectionModule extends AbstractModule {
  @Provides
  @Singleton
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
