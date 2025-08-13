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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import local.mylan.service.api.NotificationService;
import local.mylan.service.api.events.Event;
import local.mylan.service.api.events.Registration;

public final class DefaultNotificationService implements NotificationService {

    private final Map<Class<? extends Event>, Set<Consumer<Event>>> consumersMap = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public <T extends Event> Registration registerEventListener(final Class<T> type, final Consumer<T> consumer) {
        final Consumer<Event> wrapper = event -> consumer.accept(type.cast(event));
        consumersMap.computeIfAbsent(type, key -> ConcurrentHashMap.newKeySet()).add(wrapper);
        return () -> consumersMap.get(type).remove(wrapper);
    }

    @Override
    public void raiseEvent(final Event event) {
        final var consumers = consumersMap.get(event.getClass());
        if (consumers != null) {
            consumers.forEach(consumer -> executorService.submit(() -> consumer.accept(event)));
        }
    }

    @Override
    public void stop() {
        executorService.shutdown();
    }
}
