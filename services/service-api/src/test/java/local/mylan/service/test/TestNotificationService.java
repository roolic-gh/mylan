/*
 * Copyright 2026 Ruslan Kashapov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package local.mylan.service.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import local.mylan.service.api.NotificationService;
import local.mylan.service.api.events.Event;
import local.mylan.service.api.events.EventListener;
import local.mylan.service.api.events.Registration;

public class TestNotificationService implements NotificationService {
    private final List<ListenerRecord> listeners = new ArrayList<>();
    private final List<EventRecord> events = new ArrayList<>();

    @Override
    public <T extends Event> Registration registerEventListener(final Integer targetUserId,
        final Class<T> type, final EventListener<T> listener) {
        final var rec = new ListenerRecord(targetUserId, type, event -> listener.onEvent(type.cast(event)));
        listeners.add(rec);
        return () -> listeners.remove(rec);
    }

    @Override
    public void raiseEvent(final Integer targetUserId, final Event event) {
        events.add(new EventRecord(targetUserId, event));
        listeners.stream().
            filter(rec -> rec.eventType.isInstance(event) && Objects.equals(rec.userId, targetUserId))
            .forEach(rec -> rec.eventListener().onEvent(event));
    }

    public void reset() {
        listeners.clear();
        clearEvents();
    }

    public void clearEvents() {
        events.clear();
    }

    public List<Event> getEvents() {
        return getEvents(null);
    }

    public List<Event> getEvents(final Integer userId) {
        return events.stream().filter(rec -> Objects.equals(rec.userId, userId)).map(EventRecord::event).toList();
    }

    private record ListenerRecord(Integer userId, Class<? extends Event> eventType,
        EventListener<Event> eventListener) {
    }

    private record EventRecord(Integer userId, Event event) {
    }
}
