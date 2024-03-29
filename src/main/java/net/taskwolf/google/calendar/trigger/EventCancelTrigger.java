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
public final class EventCancelTrigger extends EventTrigger {
  public static TriggerInformation information(
    InputComponentSelect googleAccountSelect
  ) {
    return TriggerInformation.builder()
      .withName("google.calendar.trigger.event.cancel.name")
      .withDescription("google.calendar.trigger.event.cancel.description")
      .withIdentifier("google-calendar-event-cancel-trigger")
      .withInputVariable(InputComponentVariable.createSelect("google.calendar.trigger.event.cancel.input.account.name",
        "googleAccount", "google.calendar.trigger.event.cancel.input.account.description", googleAccountSelect))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.cancel.output.event.id", "eventId"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.cancel.output.event.name", "eventName"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.cancel.output.event.description", "eventDescription"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.cancel.output.event.location", "eventLocation"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.cancel.output.event.start.time", "eventStartTime"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.cancel.output.event.end.time", "eventEndTime"))
      .build();
  }

  public static EventCancelTrigger of(JSONObject content) {
    return create(content.getString("googleAccount"));
  }

  public static EventCancelTrigger create(String googleAccount) {
    return new EventCancelTrigger(googleAccount);
  }

  private EventCancelTrigger(String googleAccount) {
    super(googleAccount);
  }
}
