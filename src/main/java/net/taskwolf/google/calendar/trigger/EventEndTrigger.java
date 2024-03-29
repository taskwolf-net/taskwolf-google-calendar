package net.taskwolf.google.calendar.trigger;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.taskwolf.core.trigger.TriggerInformation;
import net.taskwolf.core.workflow.component.input.InputComponentSelect;
import net.taskwolf.core.workflow.component.input.InputComponentVariable;
import net.taskwolf.core.workflow.component.output.OutputComponentVariable;
import org.json.JSONObject;

@Getter
@Accessors(fluent = true)
public final class EventEndTrigger extends EventTrigger {
  public static TriggerInformation information(
    InputComponentSelect googleAccountSelect
  ) {
    return TriggerInformation.builder()
      .withName("google.calendar.trigger.event.end.name")
      .withDescription("google.calendar.trigger.event.end.description")
      .withIdentifier("google-calendar-event-end-trigger")
      .withInputVariable(InputComponentVariable.createSelect("google.calendar.trigger.event.end.input.account.name",
        "googleAccount", "google.calendar.trigger.event.end.input.account.description", googleAccountSelect))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.end.output.event.name", "eventName"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.end.output.event.description", "eventDescription"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.end.output.event.location", "eventLocation"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.end.output.event.start.time", "eventStartTime"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.end.output.event.end.time", "eventEndTime"))
      .build();
  }

  public static EventEndTrigger of(JSONObject content) {
    return create(content.getString("googleAccount"));
  }

  public static EventEndTrigger create(String googleAccount) {
    return new EventEndTrigger(googleAccount);
  }

  private EventEndTrigger(String googleAccount) {
    super(googleAccount);
  }
}
