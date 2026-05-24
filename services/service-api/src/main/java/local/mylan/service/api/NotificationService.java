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
package local.mylan.service.api;

import local.mylan.service.api.events.Event;
import local.mylan.service.api.events.EventListener;
import local.mylan.service.api.events.Registration;

public interface NotificationService {

    default <T extends Event> Registration registerEventListener(Class<T> eventType, EventListener<T> listener) {
        return registerEventListener(null, eventType, listener);
    }

    <T extends Event> Registration registerEventListener(Integer targetUserId, Class<T> eventType,
        EventListener<T> listener);

    default void raiseEvent(Event event) {
        raiseEvent(null, event);
    }

    void raiseEvent(Integer targetUserId, Event event);

    default void stop() {
    }
}
