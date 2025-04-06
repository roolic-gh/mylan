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
package local.mylan.transport.http.common;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static local.mylan.transport.http.common.RequestUtils.buildRequestContext;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import local.mylan.transport.http.api.ContextDispatcher;
import local.mylan.transport.http.api.UserContext;
import org.junit.jupiter.api.Assertions;

public final class HttpTestUtils {
    public static final String DEFAUT_HOST = "192.168.1.100:8080";

    private HttpTestUtils() {
        // utility class
    }

    public static String uriOf(final String path, final Map<String, String> params) {
        final var encoder = new QueryStringEncoder(path, StandardCharsets.UTF_8);
        params.forEach(encoder::addParam);
        return encoder.toString();
    }

    public static FullHttpRequest httpRequest(final HttpMethod method, final String uri) {
        final var request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri) ;
        request.headers().set(HttpHeaderNames.HOST, DEFAUT_HOST);
        return request;
    }

    public static FullHttpRequest httpRequest(final HttpMethod method, final String uri,
        final Map<CharSequence, CharSequence> headers) {
        final var request = httpRequest(method, uri);
        headers.forEach(request.headers()::set);
        return request;
    }

    public static void assertResponse(final FullHttpResponse response, final HttpResponseStatus expectedStatus) {
        assertNotNull(response);
        assertEquals(expectedStatus, response.status());
    }

    public static void assertResponse(final FullHttpResponse response, final HttpResponseStatus expectedStatus,
            final String expectedMediaType, final byte[] expectedContent) {
        assertResponse(response, expectedStatus);
        assertEquals(expectedMediaType, response.headers().get(CONTENT_TYPE));
        assertEquals(expectedContent.length, response.headers().getInt(CONTENT_LENGTH));
        assertArrayEquals(expectedContent, ByteBufUtil.getBytes(response.content()));
    }

    public static EmbeddedChannel setupChannel(final ContextDispatcher dispatcher) {
        return setupChannel(dispatcher, null);
    }

    public static EmbeddedChannel setupChannel(final ContextDispatcher dispatcher, final UserContext userCtx) {
        final var channel = new EmbeddedChannel();
        final var contextPath = dispatcher.contextPath();
        channel.pipeline().addLast(new SimpleChannelInboundHandler<FullHttpRequest>() {
            @Override
            protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest request)
                throws Exception {
                try {
                    if (request.uri().startsWith(contextPath)) {
                        final var requestCtx = buildRequestContext(ctx, request, contextPath, userCtx);
                        if (!dispatcher.dispatch(requestCtx)){
                            ctx.writeAndFlush(ResponseUtils.notFoundResponse(request.protocolVersion()));
                        }
                    }
                } catch (RuntimeException e) {
                     ctx.writeAndFlush(ResponseUtils.notFoundResponse(request.protocolVersion()));
                }
            }
        });
        return channel;
    }

    public static FullHttpResponse executeRequest(final EmbeddedChannel channel,
        final FullHttpRequest request) {
        channel.writeOneInbound(request);
        channel.checkException();
        final var response = channel.readOutbound();
        return Assertions.assertInstanceOf(FullHttpResponse.class, response);
    }

}
