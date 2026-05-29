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
package local.mylan.transport.http.ext;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static local.mylan.transport.http.common.HttpTestUtils.assertResponse;
import static local.mylan.transport.http.common.HttpTestUtils.executeRequest;
import static local.mylan.transport.http.common.HttpTestUtils.httpRequest;
import static local.mylan.transport.http.common.HttpTestUtils.setupChannel;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.base.Objects;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;
import local.mylan.service.api.NotificationService;
import local.mylan.service.api.UserContext;
import local.mylan.service.api.events.Event;
import local.mylan.service.api.model.User;
import local.mylan.service.spi.DefaultNotificationService;
import local.mylan.transport.http.common.api.ContextDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

public class SseDispatcherTest {

    private static final String CONTEXT_PATH = "/test";
    private static final long PING_INTERVAL_MILLIS = 1000L;
    private static final Integer USER_ID = Integer.valueOf(1001);
    private static final UserContext USER_CTX = new UserContext(new User(USER_ID, "user", "User", false), null);

    private static final Pattern EVENT_MESSAGE_PATTERN = Pattern.compile("event: (.+?)\\r\\ndata: (.+)\\r\\n\\r\\n");

    private NotificationService notificationService;
    private ContextDispatcher dispatcher;
    private Channel channel;

    @BeforeEach
    void beforeEach() {
        notificationService = new DefaultNotificationService();
        dispatcher = new SseDispatcher(CONTEXT_PATH, notificationService, PING_INTERVAL_MILLIS);
    }

    @Test
    void unauthorized() {
        final var channel = setupChannel(dispatcher);
        final var response = executeRequest(channel, sseRequest());
        assertResponse(response, HttpResponseStatus.UNAUTHORIZED);
    }

    @Test
    void unaccepted() {
        final var channel = setupChannel(dispatcher, USER_CTX);
        final var response = executeRequest(channel, httpRequest(GET, CONTEXT_PATH));
        assertResponse(response, HttpResponseStatus.NOT_ACCEPTABLE);
    }

    @Test
    void sse() throws Exception {
        final var channel = setupChannel(dispatcher, USER_CTX);
        channel.writeOneInbound(sseRequest());

        // initial response
        final var initial = assertInstanceOf(HttpResponse.class, channel.readOutbound());
        assertEquals(HttpResponseStatus.OK, initial.status());
        assertTrue(initial.headers().contains(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE, true));
        assertTrue(
            initial.headers().contains(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_EVENT_STREAM, true));

        // no pending data in channel
        assertFalse(channel.hasPendingTasks());

        // wait till keep-alive ping message appears via scheduled task
        await()
            .atMost(Duration.ofMillis(PING_INTERVAL_MILLIS + 100))
            .pollInterval(Duration.ofMillis(PING_INTERVAL_MILLIS / 4))
            .until(channel::hasPendingTasks);
        channel.runScheduledPendingTasks();
        final var pingMessage = assertInstanceOf(HttpContent.class, channel.readOutbound());
        final var pingContent = pingMessage.content().toString(StandardCharsets.UTF_8);
        assertEquals(": ping\r\n\r\n", pingContent);

        // event triggered
        final var originalEvent = new TestEvent(System.currentTimeMillis(), "test");
        notificationService.raiseEvent(USER_ID, originalEvent);
        // wait for event message appearence
        // NB check interval is less than ping to avoid ping message apper first
        await()
            .atMost(Duration.ofMillis(PING_INTERVAL_MILLIS - 100))
            .pollInterval(Duration.ofMillis(100))
            .until(() -> channel.flushOutbound().outboundMessages().peek() != null);
        final var eventMessage = assertInstanceOf(HttpContent.class, channel.readOutbound());
        final var eventContent = eventMessage.content().toString(StandardCharsets.UTF_8);
        assertNotNull(eventContent);
        final var matcher = EVENT_MESSAGE_PATTERN.matcher(eventContent);
        assertTrue(matcher.matches());
        assertEquals(originalEvent.eventType(), matcher.group(1));
        final var parcedEvent = new ObjectMapper().readValue(matcher.group(2), TestEvent.class);
        assertEquals(originalEvent, parcedEvent);
    }

    private static FullHttpRequest sseRequest() {
        return httpRequest(GET, CONTEXT_PATH, Map.of(HttpHeaderNames.ACCEPT, HttpHeaderValues.TEXT_EVENT_STREAM));
    }

    // test event POJO
    public static class TestEvent implements Event {
        private long time;
        private String name;

        TestEvent() {
            // default
        }

        TestEvent(final long time, final String name) {
            this.time = time;
            this.name = name;
        }

        public long getTime() {
            return time;
        }

        public void setTime(final long time) {
            this.time = time;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof final TestEvent testEvent)) {
                return false;
            }
            return time == testEvent.time && Objects.equal(name, testEvent.name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(time, name);
        }
    }
}
