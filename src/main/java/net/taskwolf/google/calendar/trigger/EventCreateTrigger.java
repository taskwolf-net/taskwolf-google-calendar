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
public final class EventCreateTrigger extends EventTrigger {
  public static TriggerInformation information(
    InputComponentSelect googleAccountSelect
  ) {
    return TriggerInformation.builder()
      .withName("google.calendar.trigger.event.create.name")
      .withDescription("google.calendar.trigger.event.create.description")
      .withIdentifier("google-calendar-event-create-trigger")
      .withInputVariable(InputComponentVariable.createSelect("google.calendar.trigger.event.create.input.account.name",
        "googleAccount", "google.calendar.trigger.event.create.input.account.description", googleAccountSelect))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.create.output.event.id", "eventId"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.create.output.event.name", "eventName"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.create.output.event.description", "eventDescription"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.create.output.event.location", "eventLocation"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.create.output.event.start.time", "eventStartTime"))
      .withOutputVariable(OutputComponentVariable.create("google.calendar.trigger.event.create.output.event.end.time", "eventEndTime"))
      .build();
  }

  public static EventCreateTrigger of(JSONObject content) {
    return create(content.getString("googleAccount"));
  }

  public static EventCreateTrigger create(String googleAccount) {
    return new EventCreateTrigger(googleAccount);
  }

  private EventCreateTrigger(String googleAccount) {
    super(googleAccount);
  }
}
