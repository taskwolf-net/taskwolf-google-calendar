package net.taskwolf.google.calendar;

import com.google.common.collect.Lists;
import net.taskwolf.google.GoogleAccountLink;
import net.taskwolf.google.GoogleConfiguration;
import net.taskwolf.google.account.GoogleAccountDatabaseTable;
import net.taskwolf.google.account.GoogleUserAccountDatabaseTable;

public final class GoogleCalendarAccountLink extends GoogleAccountLink {
  public static GoogleCalendarAccountLink create(
    GoogleConfiguration googleConfiguration,
    GoogleAccountDatabaseTable googleAccountDatabaseTable,
    GoogleUserAccountDatabaseTable googleUserAccountDatabaseTable
  ) {
    return new GoogleCalendarAccountLink(googleConfiguration, googleAccountDatabaseTable,
      googleUserAccountDatabaseTable);
  }

  private GoogleCalendarAccountLink(
    GoogleConfiguration googleConfiguration,
    GoogleAccountDatabaseTable googleAccountDatabaseTable,
    GoogleUserAccountDatabaseTable googleUserAccountDatabaseTable
  ) {
    super(googleConfiguration, googleAccountDatabaseTable,
      googleUserAccountDatabaseTable, "google-calendar",
      Lists.newArrayList("https://www.googleapis.com/auth/calendar",
        "https://www.googleapis.com/auth/calendar.events"));
  }
}
