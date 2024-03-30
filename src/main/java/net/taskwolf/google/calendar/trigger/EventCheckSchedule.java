package net.taskwolf.google.calendar.trigger;

import com.datastax.oss.driver.shaded.guava.common.collect.Maps;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import lombok.RequiredArgsConstructor;
import net.taskwolf.core.CoreModule;
import net.taskwolf.core.trigger.TriggerEntry;
import net.taskwolf.core.trigger.TriggerFactory;
import net.taskwolf.google.GoogleConfiguration;
import net.taskwolf.google.account.GoogleAccount;
import net.taskwolf.google.account.GoogleAccountDatabaseTable;
import net.taskwolf.google.account.GoogleCredential;
import net.taskwolf.google.calendar.GoogleCalendarEventDatabaseTable;
import net.taskwolf.google.calendar.GoogleCalendarEventTime;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor(staticName = "create")
public final class EventCheckSchedule {
  private final TriggerFactory triggerFactory;
  private final CoreModule coreModule;
  private final GoogleConfiguration googleConfiguration;
  private final GoogleAccountDatabaseTable googleAccountDatabaseTable;
  private final GoogleCalendarEventDatabaseTable googleCalendarEventDatabaseTable;
  private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
  private ScheduledFuture<?> scheduler;

  private static final int INBOX_CHECK_INITIAL_DELAY = 10;
  private static final int INBOX_CHECK_INTERVAL = 5 * 60;
  private static final TimeUnit INBOX_CHECK_TIME_UNIT = TimeUnit.SECONDS;

  public void start() {
    scheduler = executorService.scheduleAtFixedRate(this::execute,
      INBOX_CHECK_INITIAL_DELAY, INBOX_CHECK_INTERVAL, INBOX_CHECK_TIME_UNIT);
  }

  private void execute() {
    coreModule.findTriggerEntries("google calendar",
      "google-calendar-event-start-trigger").thenAccept(startEntries ->
      coreModule.findTriggerEntries("google calendar",
        "google-calendar-event-end-trigger").thenAccept(endEntries ->
        coreModule.findTriggerEntries("google calendar",
          "google-calendar-event-create-trigger").thenAccept(createEntries ->
          coreModule.findTriggerEntries("google calendar",
            "google-calendar-event-cancel-trigger").thenAccept(cancelEntries ->
            readEvents(assignTriggersToAccounts(startEntries, endEntries,
              createEntries, cancelEntries))))));
  }

  private Multimap<String, Map.Entry<UUID, EventTrigger>> assignTriggersToAccounts(
    List<TriggerEntry> startEntries, List<TriggerEntry> endEntries,
    List<TriggerEntry> createEntries, List<TriggerEntry> cancelEntries
    ) {
    var entries = Lists.<TriggerEntry>newArrayList();
    entries.addAll(startEntries);
    entries.addAll(endEntries);
    entries.addAll(createEntries);
    entries.addAll(cancelEntries);
    var result = HashMultimap.<String, Map.Entry<UUID, EventTrigger>>create();
    for (var trigger : entries) {
      var eventTrigger = (EventTrigger) triggerFactory.create(trigger.type(),
        trigger.content());
      result.put(eventTrigger.googleAccount(), new AbstractMap.SimpleEntry<>(
        trigger.id(), eventTrigger));
    }
    return result;
  }

  private void readEvents(
    Multimap<String, Map.Entry<UUID, EventTrigger>> entries
  ) {
    for (var googleId : entries.keySet()) {
      var accountTriggers = entries.get(googleId);
      googleAccountDatabaseTable.findAccount(googleId)
        .thenApply(this::createCalendarService).thenApply(this::listCalendarEvents)
        .thenAccept(currentEvents -> googleCalendarEventDatabaseTable
          .findCalendarEvents(googleId).thenAccept(previousEvents ->
            processEventTriggers(googleId, currentEvents, previousEvents,
              findCreatedEvents(currentEvents, previousEvents),
              findCanceledEvents(currentEvents, previousEvents), accountTriggers)));
    }
  }

  private Calendar createCalendarService(GoogleAccount account) {
    try {
      var credential = GoogleCredential.of(googleConfiguration.clientId(),
        googleConfiguration.clientSecret(), account).buildCredential();
      return new Calendar.Builder(GoogleNetHttpTransport.newTrustedTransport(),
        new GsonFactory(), credential)
        .setApplicationName("Taskwolf")
        .build();
    } catch (Exception ignored) {
      return null;
    }
  }

  private List<Event> listCalendarEvents(Calendar service) {
    var now = new DateTime(System.currentTimeMillis());
    try {
      return service.events().list("primary")
        .setTimeMin(now)
        .setSingleEvents(true)
        .setShowDeleted(true)
        .execute().getItems();
    } catch (Exception exception) {
      exception.printStackTrace();
      return Lists.newArrayList();
    }
  }

  private List<Event> findCreatedEvents(
    List<Event> currentEvents, List<String> previousEvents
  ) {
    var createdEvents = Lists.<Event>newArrayList();
    for (var event : currentEvents) {
      var isCreated = previousEvents.stream().noneMatch(previousEvent ->
        previousEvent.equals(event.getId())) && !event.getStatus().equals("cancelled");
      if (isCreated) {
        createdEvents.add(event);
      }
    }
    return createdEvents;
  }

  private List<Event> findCanceledEvents(
    List<Event> currentEvents, List<String> previousEvents
  ) {
    var canceledEvents = Lists.<Event>newArrayList();
    for (var event : currentEvents) {
      var isCanceled = previousEvents.stream().anyMatch(previousEvent ->
        previousEvent.equals(event.getId())) && event.getStatus().equals("cancelled");
      if (isCanceled) {
        canceledEvents.add(event);
      }
    }
    return canceledEvents;
  }

  private void processEventTriggers(
    String accountId, List<Event> currentEvents, List<String> previousEvents,
    List<Event> createdEvents, List<Event> canceledEvents,
    Collection<Map.Entry<UUID, EventTrigger>> triggers
  ) {
    updateEventDatabaseEntries(accountId, previousEvents, createdEvents,
      canceledEvents);
    for (var entry : triggers) {
      if (entry.getValue() instanceof EventStartTrigger eventStartTrigger) {
        executeEventStartTrigger(entry.getKey(), eventStartTrigger, currentEvents);
      } else if (entry.getValue() instanceof EventEndTrigger eventEndTrigger) {
        executeEventEndTrigger(entry.getKey(), eventEndTrigger, currentEvents);
      } else if (entry.getValue() instanceof EventCreateTrigger eventCreateTrigger) {
        executeEventCreateTrigger(entry.getKey(), eventCreateTrigger, createdEvents);
      } else if (entry.getValue() instanceof EventCancelTrigger eventCancelTrigger) {
        executeEventCancelTrigger(entry.getKey(), eventCancelTrigger, canceledEvents);
      }
    }
  }

  private void updateEventDatabaseEntries(
    String accountId, List<String> previousEvents, List<Event> createdEvents,
    List<Event> canceledEvents
  ) {
    previousEvents.addAll(createdEvents.stream().map(Event::getId).toList());
    previousEvents.removeAll(canceledEvents.stream().map(Event::getId).toList());
    googleCalendarEventDatabaseTable.updateCalendarEvents(accountId, previousEvents);
  }

  private void executeEventStartTrigger(
    UUID triggerId, EventStartTrigger eventStartTrigger, List<Event> events
  ) {
    var now = System.currentTimeMillis();
    var threshold = INBOX_CHECK_INTERVAL * 1000;
    for (var event : events) {
      var timeDifference  = event.getStart().getDateTime().getValue() - now;
      if (timeDifference > 0 && timeDifference <= threshold) {
        executeEventTrigger(triggerId, event);
      }
    }
  }

  private void executeEventEndTrigger(
    UUID triggerId, EventEndTrigger eventEndTrigger, List<Event> events
  ) {
    var now = System.currentTimeMillis();
    var threshold = INBOX_CHECK_INTERVAL * 1000;
    for (var event : events) {
      var timeDifference  = event.getEnd().getDateTime().getValue() - now;
      if (timeDifference > 0 && timeDifference <= threshold) {
        executeEventTrigger(triggerId, event);
      }
    }
  }

  private void executeEventCreateTrigger(
    UUID triggerId, EventCreateTrigger eventCreateTrigger,
    List<Event> createdEvents
  ) {
    for (var event : createdEvents) {
      executeEventTrigger(triggerId, event);
    }
  }

  private void executeEventCancelTrigger(
    UUID triggerId, EventCancelTrigger eventCancelTrigger,
    List<Event> canceledEvents
  ) {
    for (var event : canceledEvents) {
      executeEventTrigger(triggerId, event);
    }
  }

  private void executeEventTrigger(UUID triggerId, Event event) {
    coreModule.createWorkflow(triggerId).thenAccept(workflow ->
      workflow.trigger(createEventTriggerInformation(event)));
  }

  private Map<String, Object> createEventTriggerInformation(Event event) {
    var summary = event.getSummary() == null ? "" : event.getSummary();
    var description = event.getDescription() == null ? "" : event.getDescription();
    var location = event.getLocation() == null ? "" : event.getLocation();
    var information = Maps.<String, Object>newHashMap();
    information.put("eventId", event.getId());
    information.put("eventName", summary);
    information.put("eventDescription", description);
    information.put("eventLocation", location);
    information.put("eventStartTime", GoogleCalendarEventTime.of(
      event.getStart().getDateTime()).convertToFormattedTime().get());
    information.put("eventEndTime",  GoogleCalendarEventTime.of(
      event.getEnd().getDateTime()).convertToFormattedTime().get());
    return information;
  }

  public void stop() {
    scheduler.cancel(false);
  }
}
