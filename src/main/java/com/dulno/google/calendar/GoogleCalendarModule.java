package com.dulno.google.calendar;

import com.google.inject.Injector;
import com.dulno.core.CoreModule;
import com.dulno.core.account.AccountLink;
import com.dulno.core.action.ActionRepository;
import com.dulno.core.database.DatabaseConnection;
import com.dulno.core.database.DatabaseKeyspace;
import com.dulno.core.log.Log;
import com.dulno.core.module.Module;
import com.dulno.core.module.ModuleDescription;
import com.dulno.core.module.ModuleInformation;
import com.dulno.core.module.ModuleLoadPriority;
import com.dulno.core.trigger.TriggerRepository;
import com.dulno.core.workflow.component.input.InputComponentSelect;
import com.dulno.google.GoogleAccountLinkRepository;
import com.dulno.google.GoogleConfiguration;
import com.dulno.google.account.GoogleAccountDatabaseTable;
import com.dulno.google.account.GoogleUserAccountDatabaseTable;
import com.dulno.google.calendar.action.cancel.EventCancelAction;
import com.dulno.google.calendar.action.create.EventCreateAction;
import com.dulno.google.calendar.trigger.EventCheckSchedule;
import com.dulno.google.calendar.trigger.cancel.EventCancelTrigger;
import com.dulno.google.calendar.trigger.create.EventCreateTrigger;
import com.dulno.google.calendar.trigger.end.EventEndTrigger;
import com.dulno.google.calendar.trigger.start.EventStartTrigger;
import com.dulno.google.select.GoogleAccountSelect;

@ModuleDescription(name = "google-calendar", version = "1.0.0-SNAPSHOT",
  priority = ModuleLoadPriority.NEUTRAL)
public final class GoogleCalendarModule extends Module {
  private Log log;
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
    eventCheckSchedule = EventCheckSchedule.create(
      injector().getInstance(CoreModule.class),
      GoogleConfiguration.createAndLoad(),
      injector().getInstance(GoogleAccountDatabaseTable.class),
      injector().getInstance(GoogleCalendarEventDatabaseTable.class));
    eventCheckSchedule.start();
  }

  @Override
  public void disable() {
    eventCheckSchedule.stop();
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
  public TriggerRepository triggerRepository() {
    var databaseConnection = injector().getInstance(DatabaseConnection.class);
    var databaseKeyspace = injector().getInstance(DatabaseKeyspace.class);
    var repository = TriggerRepository.create();
    repository.registerTrigger(EventStartTrigger.create(googleAccountSelect,
      databaseConnection, databaseKeyspace));
    repository.registerTrigger(EventEndTrigger.create(googleAccountSelect,
      databaseConnection, databaseKeyspace));
    repository.registerTrigger(EventCreateTrigger.create(googleAccountSelect,
      databaseConnection, databaseKeyspace));
    repository.registerTrigger(EventCancelTrigger.create(googleAccountSelect,
      databaseConnection, databaseKeyspace));
    return repository;
  }

  @Override
  public ActionRepository actionRepository() {
    var databaseConnection = injector().getInstance(DatabaseConnection.class);
    var databaseKeyspace = injector().getInstance(DatabaseKeyspace.class);
    var googleConfiguration = injector().getInstance(GoogleConfiguration.class);
    var accountDatabaseTable = injector().getInstance(GoogleAccountDatabaseTable.class);
    var repository = ActionRepository.create();
    repository.registerAction(EventCreateAction.create(googleConfiguration,
      accountDatabaseTable, googleAccountSelect, databaseConnection,
      databaseKeyspace));
    repository.registerAction(EventCancelAction.create(googleConfiguration,
      accountDatabaseTable, googleAccountSelect, databaseConnection,
      databaseKeyspace));
    return repository;
  }
}