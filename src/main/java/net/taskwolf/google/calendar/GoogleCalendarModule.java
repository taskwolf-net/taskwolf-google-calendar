package net.taskwolf.google.calendar;

import com.google.common.collect.Lists;
import com.google.inject.Injector;
import net.taskwolf.core.CoreModule;
import net.taskwolf.core.account.AccountLink;
import net.taskwolf.core.action.ActionFactory;
import net.taskwolf.core.action.ActionInformation;
import net.taskwolf.core.log.Log;
import net.taskwolf.core.module.Module;
import net.taskwolf.core.module.ModuleDescription;
import net.taskwolf.core.module.ModuleInformation;
import net.taskwolf.core.module.ModuleLoadPriority;
import net.taskwolf.core.trigger.TriggerFactory;
import net.taskwolf.core.trigger.TriggerInformation;
import net.taskwolf.core.workflow.component.input.InputComponentSelect;
import net.taskwolf.google.GoogleAccountLinkRepository;
import net.taskwolf.google.GoogleConfiguration;
import net.taskwolf.google.account.GoogleAccountDatabaseTable;
import net.taskwolf.google.account.GoogleUserAccountDatabaseTable;
import net.taskwolf.google.calendar.action.EventCancelAction;
import net.taskwolf.google.calendar.action.EventCreateAction;
import net.taskwolf.google.calendar.action.GoogleCalendarActionFactory;
import net.taskwolf.google.calendar.trigger.*;
import net.taskwolf.google.select.GoogleAccountSelect;

import java.util.List;

@ModuleDescription(name = "google-calendar", version = "1.0.0-SNAPSHOT",
  priority = ModuleLoadPriority.NEUTRAL)
public final class GoogleCalendarModule extends Module {
  private Log log;
  private TriggerFactory triggerFactory;
  private ActionFactory actionFactory;
  private GoogleCalendarAccountLink accountLink;
  private InputComponentSelect googleAccountSelect;
  private EventCheckSchedule eventCheckSchedule;

  public GoogleCalendarModule(Injector injector) {
    super(injector.createChildInjector(GoogleCalendarInjectionModule.create()));
  }

  @Override
  public void enable() throws Exception {
    log = injector().getInstance(Log.class).subLog("Google Calendar");
    var googleConfiguration = GoogleConfiguration.createAndLoad();
    var googleAccountDatabaseTable = injector().getInstance(
      GoogleAccountDatabaseTable.class);
    var googleUserAccountDatabaseTable = injector().getInstance(
      GoogleUserAccountDatabaseTable.class);
    triggerFactory = GoogleCalendarTriggerFactory.create();
    actionFactory = GoogleCalendarActionFactory.create(googleConfiguration,
      googleAccountDatabaseTable);
    accountLink = GoogleCalendarAccountLink.create(googleConfiguration,
      googleAccountDatabaseTable, googleUserAccountDatabaseTable,
      injector().getInstance(GoogleCalendarEventDatabaseTable.class));
    injector().getInstance(GoogleAccountLinkRepository.class)
      .registerGoogleAccountLink(accountLink);
    googleAccountSelect = GoogleAccountSelect.create(googleAccountDatabaseTable,
      googleUserAccountDatabaseTable);
    startEventCheckSchedule();
  }

  private void startEventCheckSchedule() throws Exception {
    eventCheckSchedule = EventCheckSchedule.create(triggerFactory,
      injector().getInstance(CoreModule.class), GoogleConfiguration.createAndLoad(),
      injector().getInstance(GoogleAccountDatabaseTable.class),
      injector().getInstance(GoogleCalendarEventDatabaseTable.class));
    eventCheckSchedule.start();
  }

  @Override
  public void disable() {
    eventCheckSchedule.stop();
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
  public AccountLink accountLink() {
    return accountLink;
  }

  @Override
  public ModuleInformation moduleInformation() {
    return ModuleInformation.create("Google Calendar", "", "googlecalendar.png",
      ModuleInformation.Type.PUBLIC);
  }

  @Override
  public List<TriggerInformation> triggerInformation() {
    return Lists.newArrayList(EventStartTrigger.information(googleAccountSelect),
      EventEndTrigger.information(googleAccountSelect),
      EventCreateTrigger.information(googleAccountSelect),
      EventCancelTrigger.information(googleAccountSelect));
  }

  @Override
  public List<ActionInformation> actionInformation() {
    return Lists.newArrayList(EventCreateAction.information(googleAccountSelect),
      EventCancelAction.information(googleAccountSelect));
  }
}