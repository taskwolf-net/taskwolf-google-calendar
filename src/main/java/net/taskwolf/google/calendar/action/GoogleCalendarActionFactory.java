package net.taskwolf.google.calendar.action;

import lombok.RequiredArgsConstructor;
import net.taskwolf.core.action.Action;
import net.taskwolf.core.action.ActionFactory;
import net.taskwolf.google.GoogleConfiguration;
import net.taskwolf.google.account.GoogleAccountDatabaseTable;
import org.json.JSONObject;

@RequiredArgsConstructor(staticName = "create")
public final class GoogleCalendarActionFactory implements ActionFactory {
  private final GoogleConfiguration googleConfiguration;
  private final GoogleAccountDatabaseTable googleAccountDatabaseTable;

  @Override
  public Action create(String type, String content) {
    var json = new JSONObject(content);
    if (type.equals("google-calendar-event-create-action")) {
      return EventCreateAction.of(googleConfiguration,
        googleAccountDatabaseTable, json);
    }
    if (type.equals("google-calendar-event-cancel-action")) {
      return EventCancelAction.of(googleConfiguration,
        googleAccountDatabaseTable, json);
    }
    return null;
  }
}