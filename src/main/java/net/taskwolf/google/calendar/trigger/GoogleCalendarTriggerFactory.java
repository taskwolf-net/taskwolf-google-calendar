package net.taskwolf.google.calendar.trigger;

import lombok.RequiredArgsConstructor;
import net.taskwolf.core.trigger.Trigger;
import net.taskwolf.core.trigger.TriggerFactory;
import org.json.JSONObject;

@RequiredArgsConstructor(staticName = "create")
public class GoogleCalendarTriggerFactory implements TriggerFactory {
  @Override
  public Trigger create(String type, String content) {
    var json = new JSONObject(content);
    if (type.equals("google-calendar-event-start-trigger")) {
      return EventStartTrigger.of(json);
    }
    if (type.equals("google-calendar-event-end-trigger")) {
      return EventEndTrigger.of(json);
    }
    if (type.equals("google-calendar-event-create-trigger")) {
      return EventCreateTrigger.of(json);
    }
    if (type.equals("google-calendar-event-cancel-trigger")) {
      return EventCancelTrigger.of(json);
    }
    return null;
  }
}
