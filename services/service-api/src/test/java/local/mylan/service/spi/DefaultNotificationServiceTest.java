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
import java.util.function.Consumer;
import local.mylan.service.api.NotificationService;
import local.mylan.service.api.events.Event;
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

    private static final SomeTestEvent SOME_1 = new SomeTestEvent("some-1");
    private static final SomeTestEvent SOME_2 = new SomeTestEvent("some-2");
    private static final SomeTestEvent SOME_3 = new SomeTestEvent("some-3");
    private static final SomeTestEvent SOME_4 = new SomeTestEvent("some-4");
    private static final SomeTestEvent SOME_5 = new SomeTestEvent("some-5");
    private static final OtherTestEvent OTHER_1 = new OtherTestEvent("other-1");
    private static final OtherTestEvent OTHER_2 = new OtherTestEvent("other-1");
    private static final OtherTestEvent OTHER_3 = new OtherTestEvent("other-1");

    @Mock
    Consumer<SomeTestEvent> someConsumer1;
    @Mock
    Consumer<SomeTestEvent> someConsumer2;
    @Mock
    Consumer<OtherTestEvent> otherConsumer;
    @Captor
    ArgumentCaptor<SomeTestEvent> someEventCaptor1;
    @Captor
    ArgumentCaptor<SomeTestEvent> someEventCaptor2;
    @Captor
    ArgumentCaptor<OtherTestEvent> otherEventCaptor;

    NotificationService service;

    @BeforeEach
    void beforeEach(){
        service = new DefaultNotificationService();
    }

    @AfterEach
    void afterEach(){
        service.stop();
    }

    @Test
    void consumeEvent() throws IOException {
        final var regSome1 = service.registerEventListener(SomeTestEvent.class, someConsumer1);
        final var regSome2 = service.registerEventListener(SomeTestEvent.class, someConsumer2);
        final var regOther = service.registerEventListener(OtherTestEvent.class, otherConsumer);

        List.of(SOME_1, SOME_2, OTHER_1, SOME_3, SOME_4, OTHER_2).forEach(service::raiseEvent);
        regSome2.terminate();
        regOther.terminate();
        List.of(SOME_5, OTHER_3).forEach(service::raiseEvent);
        regSome1.terminate();

        verify(someConsumer1, timeout(500).times(5)).accept(someEventCaptor1.capture());
        assertEquals(List.of(SOME_1, SOME_2, SOME_3, SOME_4, SOME_5), someEventCaptor1.getAllValues());

        verify(someConsumer2, timeout(300).times(4)).accept(someEventCaptor2.capture());
        assertEquals(List.of(SOME_1, SOME_2, SOME_3, SOME_4), someEventCaptor2.getAllValues());

        verify(otherConsumer, timeout(300).times(2)).accept(otherEventCaptor.capture());
        assertEquals(List.of(OTHER_1, OTHER_2), otherEventCaptor.getAllValues());
    }

    record SomeTestEvent(String value) implements Event {
    }

    record OtherTestEvent(String value) implements Event {
    }
}
