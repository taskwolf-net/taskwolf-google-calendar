package net.taskwolf.google.calendar;

import com.google.common.collect.Lists;
import net.taskwolf.core.CoreModule;
import net.taskwolf.core.action.ActionFactory;
import net.taskwolf.core.action.ActionInformation;
import net.taskwolf.core.log.Log;
import net.taskwolf.core.module.Module;
import net.taskwolf.core.module.ModuleDescription;
import net.taskwolf.core.module.ModuleInformation;
import net.taskwolf.core.module.ModuleLoadPriority;
import net.taskwolf.core.trigger.TriggerFactory;
import net.taskwolf.core.trigger.TriggerInformation;

import java.util.List;

@ModuleDescription(name = "google-calendar", version = "1.0.0-SNAPSHOT",
  priority = ModuleLoadPriority.NEUTRAL)
public final class GoogleCalendarModule extends Module {
  private Log log;
  private TriggerFactory triggerFactory;
  private ActionFactory actionFactory;

  public GoogleCalendarModule(CoreModule coreModule) {
    super(coreModule);
  }

  @Override
  public void enable() throws Exception {
    log = coreModule().log().subLog("Google Calendar");
  }

  @Override
  public void disable() {

  }

  @Override
  public TriggerFactory triggerFactory() {
    return triggerFactory;
  }

  @Override
  public ActionFactory actionFactory() {
    return actionFactory;
  }

  @Override
  public ModuleInformation moduleInformation() {
    return ModuleInformation.create("Goggle Calendar", "", "googlecalendar.png",
      ModuleInformation.Type.PUBLIC);
  }

  @Override
  public List<TriggerInformation> triggerInformation() {
    return Lists.newArrayList();
  }

  @Override
  public List<ActionInformation> actionInformation() {
    return Lists.newArrayList();
  }
}