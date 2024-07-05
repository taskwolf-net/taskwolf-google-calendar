package net.taskwolf.google.calendar.trigger;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import lombok.RequiredArgsConstructor;
import net.taskwolf.core.CoreModule;
import net.taskwolf.core.iterator.AsyncIterator;
import net.taskwolf.core.trigger.TriggerEntry;
import net.taskwolf.google.GoogleConfiguration;
import net.taskwolf.google.account.GoogleAccount;
import net.taskwolf.google.account.GoogleAccountDatabaseTable;
import net.taskwolf.google.account.GoogleCredential;
import net.taskwolf.google.calendar.GoogleCalendarEventDatabaseTable;
import net.taskwolf.google.calendar.GoogleCalendarEventTime;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@RequiredArgsConstructor(staticName = "create")
public final class EventCheckSchedule {
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
    coreModule.findAllTriggerEntries("google-calendar",
      "google-calendar-event-start-trigger").thenAccept(startEntries ->
      coreModule.findAllTriggerEntries("google-calendar",
        "google-calendar-event-end-trigger").thenAccept(endEntries ->
        coreModule.findAllTriggerEntries("google-calendar",
          "google-calendar-event-create-trigger").thenAccept(createEntries ->
          coreModule.findAllTriggerEntries("google-calendar",
            "google-calendar-event-cancel-trigger").thenAccept(cancelEntries ->
              assignTriggersToAccounts(startEntries, endEntries, createEntries,
                cancelEntries).thenAccept(this::readEvents)))));
  }

  private CompletableFuture<Multimap<String, TriggerEntry>> assignTriggersToAccounts(
    List<TriggerEntry> startEntries, List<TriggerEntry> endEntries,
    List<TriggerEntry> createEntries, List<TriggerEntry> cancelEntries
    ) {
    var entries = Lists.<TriggerEntry>newArrayList();
    entries.addAll(startEntries);
    entries.addAll(endEntries);
    entries.addAll(createEntries);
    entries.addAll(cancelEntries);
    var futureResponse = new CompletableFuture<Multimap<String, TriggerEntry>>();
    var result = HashMultimap.<String, TriggerEntry>create();
    AsyncIterator.execute(entries, entry ->
      coreModule.findTrigger(entry.module(), entry.type()).get()
        .findContent(entry.id()).thenAccept(content ->
          result.put((String) content.get("googleAccount"), entry)).thenAccept(
            value -> futureResponse.complete(result)));
    return futureResponse;
  }

  private void readEvents(
    Multimap<String, TriggerEntry> entries
  ) {
    for (var googleId : entries.keySet()) {
      var accountTriggers = entries.get(googleId);
      googleAccountDatabaseTable.findAccount(googleId)
        .thenApply(this::createCalendarService).thenApplyAsync(this::listCalendarEvents)
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
    try {
      var minTime = new DateTime(System.currentTimeMillis() - 1000 * 60 * 60 * 24);
      var maxTime = new DateTime(System.currentTimeMillis() + 1000 * 60 * 60 * 24);
      var events = service.events().list("primary")
        .setTimeMin(minTime)
        .setTimeMax(maxTime)
        .setSingleEvents(true)
        .setShowDeleted(false)
        .execute().getItems();
      events.addAll(service.events().list("primary")
        .setSingleEvents(false)
        .setShowDeleted(true)
        .execute().getItems().stream()
        .filter(event -> events.stream().noneMatch(existingEvent ->
          event.getId().split("_")[0].equals(existingEvent.getId().split("_")[0])))
        .toList());
      return events;
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
    Collection<TriggerEntry> triggers
  ) {
    updateEventDatabaseEntries(accountId, previousEvents, createdEvents,
      canceledEvents);
    for (var entry : triggers) {
      if (entry.type().equals("google-calendar-event-start-trigger")) {
        executeEventStartTrigger(entry.id(), currentEvents);
      } else if (entry.type().equals("google-calendar-event-end-trigger")) {
        executeEventEndTrigger(entry.id(), currentEvents);
      } else if (entry.type().equals("google-calendar-event-create-trigger")) {
        executeEventCreateTrigger(entry.id(), createdEvents);
      } else if (entry.type().equals("google-calendar-event-cancel-trigger")) {
        executeEventCancelTrigger(entry.id(), canceledEvents);
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

  private void executeEventStartTrigger(UUID triggerId, List<Event> events) {
    var now = System.currentTimeMillis();
    var threshold = INBOX_CHECK_INTERVAL * 1000;
    for (var event : events) {
      if (event.getStatus().equals("cancelled")) {
        continue;
      }
      var timeDifference  = event.getStart().getDateTime().getValue() - now;
      if (timeDifference > 0 && timeDifference <= threshold) {
        executeEventTrigger(triggerId, event);
      }
    }
  }

  private void executeEventEndTrigger(UUID triggerId, List<Event> events) {
    var now = System.currentTimeMillis();
    var threshold = INBOX_CHECK_INTERVAL * 1000;
    for (var event : events) {
      if (event.getStatus().equals("cancelled")) {
        continue;
      }
      var timeDifference  = event.getEnd().getDateTime().getValue() - now;
      if (timeDifference > 0 && timeDifference <= threshold) {
        executeEventTrigger(triggerId, event);
      }
    }
  }

  private void executeEventCreateTrigger(UUID triggerId, List<Event> createdEvents) {
    for (var event : createdEvents) {
      executeEventTrigger(triggerId, event);
    }
  }

  private void executeEventCancelTrigger(UUID triggerId, List<Event> canceledEvents) {
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
