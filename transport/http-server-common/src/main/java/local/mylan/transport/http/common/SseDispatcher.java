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
package local.mylan.transport.http.common;

import static java.nio.charset.StandardCharsets.UTF_8;
import static local.mylan.transport.http.common.ResponseUtils.allowResponse;
import static local.mylan.transport.http.common.ResponseUtils.simpleResponse;
import static local.mylan.transport.http.common.ResponseUtils.unsupportedMethodResponse;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.util.concurrent.TimeUnit;
import local.mylan.service.api.NotificationService;
import local.mylan.service.api.events.Event;
import local.mylan.service.api.events.EventListener;
import local.mylan.transport.http.api.ContextDispatcher;
import local.mylan.transport.http.api.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

public class SseDispatcher implements ContextDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(SseDispatcher.class);
    private static final String ALLOWED_METHODS = "GET, OPTIONS";

    private static final ByteBuf PING_MESSAGE =
        Unpooled.wrappedBuffer(": ping\r\n\r\n".getBytes(UTF_8)).asReadOnly();
    private static final String EVENT_MESSAGE_PATTERN = "event: %s\r\ndata: %s\r\n\r\n";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String contextPath;
    private final NotificationService notificationService;
    private final long pingIntervalMillis;

    public SseDispatcher(final String contextPath, final NotificationService notificationService,
        final long pingIntervalMillis) {

        this.contextPath = contextPath;
        this.notificationService = notificationService;
        this.pingIntervalMillis = pingIntervalMillis;
    }

    @Override
    public String contextPath() {
        return contextPath;
    }

    @Override
    public boolean dispatch(final RequestContext ctx) {
        switch (ctx.method().name()) {
            case "OPTIONS" -> ctx.sendResponse(allowResponse(ctx.protocolVersion(), ALLOWED_METHODS));
            case "GET" -> handleRquest(ctx);
            default -> ctx.sendResponse(unsupportedMethodResponse(ctx.protocolVersion()));
        }
        return true;
    }

    private void handleRquest(final RequestContext ctx) {

        final var userId = ctx.userContext() == null || ctx.userContext().currentUser() == null
            ? null : ctx.userContext().currentUser().getUserId();
        if (userId == null) {
            ctx.sendResponse(simpleResponse(ctx.protocolVersion(), HttpResponseStatus.UNAUTHORIZED));
        } else if (!ctx.headers().contains(HttpHeaderNames.ACCEPT, HttpHeaderValues.TEXT_EVENT_STREAM, true)) {
            ctx.sendResponse(simpleResponse(ctx.protocolVersion(), HttpResponseStatus.NOT_ACCEPTABLE));
        } else {
            new SseEventStreamer(ctx.channelHandlerContext(), ctx.protocolVersion(), userId).start();
        }
    }

    private class SseEventStreamer implements EventListener<Event> {

        private final ChannelHandlerContext channelCtx;
        private final HttpVersion protocolVersion;
        private final Integer userId;

        SseEventStreamer(final ChannelHandlerContext channelCtx, final HttpVersion protocolVersion,
            final Integer userId) {

            this.channelCtx = channelCtx;
            this.protocolVersion = protocolVersion;
            this.userId = userId;
        }

        boolean start() {
            final var registration = notificationService.registerEventListener(userId, Event.class, this);
            channelCtx.channel().closeFuture().addListener(future -> {
                LOG.debug("SSE stream closed for user {}", userId);
                registration.terminate();
            });
            // response OK with headers only, body chunks will be an event stream
            final var response = new DefaultHttpResponse(protocolVersion, HttpResponseStatus.OK);
            response.headers()
                .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_EVENT_STREAM)
                .set(HttpHeaderNames.CACHE_CONTROL, HttpHeaderValues.NO_CACHE)
                .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
                // chunked header prevents packet being delayed by Netty's HTTP Aggregator handler
                .set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
            channelCtx.writeAndFlush(response);
            LOG.debug("SSE stream started for user {}", userId);

            // schedule keep-alive events (no action required SSE 'ping' comment) if necessary
            if (pingIntervalMillis > 0) {
                schedulePing();
            }
            return true;
        }

        @Override
        public void onEvent(final Event event) {
            if (isChannelWritable()) {
                final var json = OBJECT_MAPPER.writeValueAsString(event);
                final var byteBuf = Unpooled.buffer();
                byteBuf.writeCharSequence(String.format(EVENT_MESSAGE_PATTERN, event.eventType(), json), UTF_8);
                channelCtx.writeAndFlush(new DefaultHttpContent(byteBuf));
            }
        }

        private void schedulePing() {
            channelCtx.executor().schedule(this::sendPing, pingIntervalMillis, TimeUnit.MILLISECONDS);
        }

        private void sendPing() {
            if (isChannelWritable()) {
                channelCtx.writeAndFlush(new DefaultHttpContent(PING_MESSAGE.retainedSlice()));
                schedulePing();
            }
        }

        private boolean isChannelWritable() {
            return !channelCtx.isRemoved() && channelCtx.channel().isActive();
        }
    }
}
