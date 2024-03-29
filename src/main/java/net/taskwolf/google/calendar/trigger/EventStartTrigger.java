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
public final class EventStartTrigger extends EventTrigger {
  public static TriggerInformation information(
    InputComponentSelect googleAccountSelect
  ) {
    return TriggerInformation.builder()
      .withName("google.calendar.trigger.event.start.name")
      .withDescription("google.calendar.trigger.event.start.description")
      .withIdentifier("google-calendar-event-start-trigger")
      .withInputVariable(InputComponentVariable.createSelect("google.calendar.trigger.event.start.input.account.name",
        "googleAccount", "google.calendar.trigger.event.start.input.account.description", googleAccountSelect))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.start.output.event.id", "eventId"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.start.output.event.name", "eventName"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.start.output.event.description", "eventDescription"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.start.output.event.location", "eventLocation"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.start.output.event.start.time", "eventStartTime"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.start.output.event.end.time", "eventEndTime"))
      .build();
  }

  public static EventStartTrigger of(JSONObject content) {
    return create(content.getString("googleAccount"));
  }

  public static EventStartTrigger create(String googleAccount) {
    return new EventStartTrigger(googleAccount);
  }

  private EventStartTrigger(String googleAccount) {
    super(googleAccount);
  }
}

