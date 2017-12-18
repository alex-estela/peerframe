package fr.estela.peerframe.device.util;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import fr.estela.peerframe.api.model.Event;

@Component
public class EventCache {

    private static final int QUEUE_SIZE_EVENTS = 10;

    private ArrayDeque<Event> events = new ArrayDeque<>(QUEUE_SIZE_EVENTS);

    public synchronized void addEvent(String eventMessage, Event.TypeEnum eventType) {
        Event event = new Event();
        event.setTime(Instant.now().toString());
        event.setDescription(eventMessage);
        event.setType(eventType);

        if (events.size() == QUEUE_SIZE_EVENTS) events.poll();
        events.add(event);
    }

    public List<Event> getEvents() {
        return new ArrayList<>(events);
    }

}