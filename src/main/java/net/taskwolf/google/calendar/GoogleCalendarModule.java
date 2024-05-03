package net.taskwolf.google.calendar;

import com.google.inject.Injector;
import net.taskwolf.core.CoreModule;
import net.taskwolf.core.account.AccountLink;
import net.taskwolf.core.action.ActionRepository;
import net.taskwolf.core.database.DatabaseConnection;
import net.taskwolf.core.database.DatabaseKeyspace;
import net.taskwolf.core.log.Log;
import net.taskwolf.core.module.Module;
import net.taskwolf.core.module.ModuleDescription;
import net.taskwolf.core.module.ModuleInformation;
import net.taskwolf.core.module.ModuleLoadPriority;
import net.taskwolf.core.trigger.TriggerRepository;
import net.taskwolf.core.workflow.component.input.InputComponentSelect;
import net.taskwolf.google.GoogleAccountLinkRepository;
import net.taskwolf.google.GoogleConfiguration;
import net.taskwolf.google.account.GoogleAccountDatabaseTable;
import net.taskwolf.google.account.GoogleUserAccountDatabaseTable;
import net.taskwolf.google.calendar.action.cancel.EventCancelAction;
import net.taskwolf.google.calendar.action.create.EventCreateAction;
import net.taskwolf.google.calendar.trigger.EventCheckSchedule;
import net.taskwolf.google.calendar.trigger.cancel.EventCancelTrigger;
import net.taskwolf.google.calendar.trigger.create.EventCreateTrigger;
import net.taskwolf.google.calendar.trigger.end.EventEndTrigger;
import net.taskwolf.google.calendar.trigger.start.EventStartTrigger;
import net.taskwolf.google.select.GoogleAccountSelect;

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