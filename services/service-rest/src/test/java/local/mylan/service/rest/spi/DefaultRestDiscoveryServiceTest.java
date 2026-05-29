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
package local.mylan.service.rest.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.SettableFuture;
import local.mylan.service.api.DiscoveryService;
import local.mylan.service.api.NotificationService;
import local.mylan.service.api.UserContext;
import local.mylan.service.api.events.DiscoveryStatusEvent;
import local.mylan.service.api.events.Event;
import local.mylan.service.api.model.DiscoveryStatus;
import local.mylan.service.api.model.User;
import local.mylan.service.rest.api.RestDiscoveryService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultRestDiscoveryServiceTest {
    private static final Integer USER_ID = Integer.valueOf(1001);
    static DiscoveryStatus statusRunning = new DiscoveryStatus();
    static DiscoveryStatus statusCompleted = new DiscoveryStatus();

    @Mock
    DiscoveryService discoveryService;
    @Mock
    NotificationService notificationService;
    @Captor
    ArgumentCaptor<Event> eventCaptor;

    RestDiscoveryService restService;

    @BeforeAll
    static void beforeAll() {
        statusRunning.setRunning(true);
        statusRunning.setStartTime(1000L);
        statusCompleted.setStartTime(1000L);
        statusCompleted.setEndTime(3000L);
        statusCompleted.setDevicesDiscovered(3);
    }

    @BeforeEach
    void beforeEach() {
        restService = new DefaultRestDiscoveryService(discoveryService, notificationService);
        doReturn(statusRunning).when(discoveryService).currentStatus();
    }

    @Test
    void getDiscoveryStatus() {
        assertEquals(statusRunning, restService.getDiscoveryStatus());
    }

    @Test
    void startDiscoveryGuest() {
        final var userCtx = new UserContext(new User("guest", "guest", false), null);
        final var status = restService.startDiscovery(userCtx);
        assertEquals(statusRunning, status);
        // no discovery started by anonimous
        verify(discoveryService, never()).startDiscovery();
    }

    @Test
    void startDiscoveryUser() {
        final var userCtx = new UserContext(new User(USER_ID, "user", "User", false), null);
        final var future = SettableFuture.<DiscoveryStatus>create();
        doReturn(future).when(discoveryService).startDiscovery();

        final var status = restService.startDiscovery(userCtx);
        assertEquals(statusRunning, status);
        // verify discovery started
        verify(discoveryService, times(1)).startDiscovery();
        // verify user event set to be raised on completion
        future.set(statusCompleted);
        verify(notificationService, timeout(1000).times(1)).raiseEvent(eq(USER_ID), eventCaptor.capture());
        final var statusEvent = assertInstanceOf(DiscoveryStatusEvent.class, eventCaptor.getValue());
        // verify status data populated to event
        assertEquals(statusCompleted.isRunning(), statusEvent.isRunning());
        assertEquals(statusCompleted.getStartTime(), statusEvent.getStartTime());
        assertEquals(statusCompleted.getEndTime(), statusEvent.getEndTime());
        assertEquals(statusCompleted.getDevicesDiscovered(), statusEvent.getDevicesDiscovered());
    }
}
