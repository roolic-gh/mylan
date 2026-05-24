/*
 * Copyright 2025 Ruslan Kashapov
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
package local.mylan.service.spi;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import local.mylan.service.api.NotificationService;
import local.mylan.service.api.events.Event;
import local.mylan.service.api.events.EventListener;
import local.mylan.service.api.events.Registration;

public final class DefaultNotificationService implements NotificationService {
    private final AtomicLong idCount = new AtomicLong(0);
    private final Map<Long, ListenerRecord> listeners = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public <T extends Event> Registration registerEventListener(final Integer targetUserId,
        final Class<T> type, final EventListener<T> listener) {

        final var id = idCount.incrementAndGet();
        listeners.put(id, new ListenerRecord(targetUserId, type, event -> listener.onEvent(type.cast(event))));
        return () -> listeners.remove(id);
    }

    @Override
    public void raiseEvent(final Integer targetUserId, final Event event) {
        listeners.values().stream().
            filter(rec -> rec.eventType.isInstance(event) && Objects.equals(rec.userId, targetUserId))
            .forEach(rec -> executorService.submit(() -> rec.eventListener().onEvent(event)));

    }

    @Override
    public void stop() {
        listeners.clear();
        executorService.shutdown();
    }

    private record ListenerRecord(Integer userId, Class<? extends Event> eventType,
        EventListener<Event> eventListener) {
    }
}
