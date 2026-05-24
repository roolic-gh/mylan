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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;
import local.mylan.service.api.NotificationService;
import local.mylan.service.api.events.Event;
import local.mylan.service.api.events.EventListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultNotificationServiceTest {

    private static final Integer USER_ID1 = Integer.valueOf(101);
    private static final Integer USER_ID2 = Integer.valueOf(102);

    private static final TestEventA EVENT_A1 = new TestEventA("A1");
    private static final TestEventA EVENT_A2 = new TestEventA("A2");
    private static final TestEventA EVENT_A3 = new TestEventA("A3");
    private static final TestEventA EVENT_A4 = new TestEventA("A4");
    private static final TestEventA EVENT_A5 = new TestEventA("A5");
    private static final TestEventB EVENT_B1 = new TestEventB("B1");
    private static final TestEventB EVENT_B2 = new TestEventB("B2");
    private static final TestEventB EVENT_B3 = new TestEventB("B3");
    private static final TestEventX EVENT_X1 = new TestEventX("X1");
    private static final TestEventX EVENT_X2 = new TestEventX("X2");
    private static final TestEventX EVENT_Y1 = new TestEventY("Y1");
    private static final TestEventX EVENT_Y2 = new TestEventY("Y2");

    @Mock
    EventListener<TestEventA> listenerA1;
    @Mock
    EventListener<TestEventA> listenerA2;
    @Mock
    EventListener<TestEventB> listenerB;
    @Mock
    EventListener<TestEventX> listenerX;
    @Mock
    EventListener<TestEventY> listenerY;
    @Captor
    ArgumentCaptor<TestEventA> eventCaptorA1;
    @Captor
    ArgumentCaptor<TestEventA> eventCaptorA2;
    @Captor
    ArgumentCaptor<TestEventB> eventCaptorB;
    @Captor
    ArgumentCaptor<TestEventX> eventCaptorX;
    @Captor
    ArgumentCaptor<TestEventY> eventCaptorY;

    NotificationService service;

    @BeforeEach
    void beforeEach() {
        service = new DefaultNotificationService();
    }

    @AfterEach
    void afterEach() {
        service.stop();
    }

    @Test
    void noUserEvents() throws IOException {
        final var regA1 = service.registerEventListener(TestEventA.class, listenerA1);
        final var regA2 = service.registerEventListener(TestEventA.class, listenerA2);
        final var regB = service.registerEventListener(TestEventB.class, listenerB);

        List.of(EVENT_A1, EVENT_A2, EVENT_B1, EVENT_A3, EVENT_A4, EVENT_B2).forEach(service::raiseEvent);
        regA2.terminate();
        regB.terminate();
        List.of(EVENT_A5, EVENT_B3).forEach(service::raiseEvent);
        regA1.terminate();

        verify(listenerA1, timeout(300).times(5)).onEvent(eventCaptorA1.capture());
        assertEquals(List.of(EVENT_A1, EVENT_A2, EVENT_A3, EVENT_A4, EVENT_A5), eventCaptorA1.getAllValues());

        verify(listenerA2, timeout(300).times(4)).onEvent(eventCaptorA2.capture());
        assertEquals(List.of(EVENT_A1, EVENT_A2, EVENT_A3, EVENT_A4), eventCaptorA2.getAllValues());

        verify(listenerB, timeout(300).times(2)).onEvent(eventCaptorB.capture());
        assertEquals(List.of(EVENT_B1, EVENT_B2), eventCaptorB.getAllValues());
    }

    @Test
    void userEvents(){
        service.registerEventListener(USER_ID1, TestEventA.class, listenerA1);
        service.registerEventListener(USER_ID2, TestEventA.class, listenerA2);

        service.raiseEvent(USER_ID1, EVENT_A1);
        service.raiseEvent(USER_ID2, EVENT_A2);
        service.raiseEvent(null, EVENT_A3);
        service.raiseEvent(USER_ID1, EVENT_A4);
        service.raiseEvent(USER_ID2, EVENT_A5);

        verify(listenerA1, timeout(300).times(2)).onEvent(eventCaptorA1.capture());
        assertEquals(List.of(EVENT_A1, EVENT_A4), eventCaptorA1.getAllValues());

        verify(listenerA2, timeout(300).times(2)).onEvent(eventCaptorA2.capture());
        assertEquals(List.of(EVENT_A2, EVENT_A5), eventCaptorA2.getAllValues());
    }

    @Test
    void extendedEventClass() {
        service.registerEventListener(USER_ID1, TestEventX.class, listenerX);
        service.registerEventListener(USER_ID1, TestEventY.class, listenerY);

        service.raiseEvent(USER_ID1, EVENT_X1);
        service.raiseEvent(EVENT_X2);
        service.raiseEvent(EVENT_Y1);
        service.raiseEvent(USER_ID1, EVENT_Y2);

        verify(listenerX, timeout(300).times(2)).onEvent(eventCaptorX.capture());
        assertEquals(List.of(EVENT_X1, EVENT_Y2), eventCaptorX.getAllValues());

        verify(listenerY, timeout(300).times(1)).onEvent(eventCaptorY.capture());
        assertEquals(List.of(EVENT_Y2), eventCaptorY.getAllValues());
    }

    record TestEventA(String value) implements Event {
    }

    record TestEventB(String value) implements Event {
    }

    static class TestEventX implements Event {
        final String value;

        TestEventX(final String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    static class TestEventY extends TestEventX {
        TestEventY(final String value) {
            super(value);
        }
    }
}
