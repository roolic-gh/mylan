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

import static io.netty.buffer.Unpooled.EMPTY_BUFFER;
import static io.netty.handler.codec.http.HttpHeaderNames.ALLOW;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;
import static io.netty.handler.codec.http.HttpResponseStatus.FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslHandler;
import java.nio.charset.StandardCharsets;

public final class ResponseUtils {

    private ResponseUtils() {
        // utility class
    }

    public static FullHttpResponse simpleResponse(final HttpVersion version, final HttpResponseStatus status) {
        return new DefaultFullHttpResponse(version, status, EMPTY_BUFFER);
    }

    public static FullHttpResponse simpleResponse(final HttpVersion version, final HttpResponseStatus status,
        final CharSequence headerName, final String headerValue) {
        final var response = simpleResponse(version, status);
        response.headers().set(headerName, headerValue);
        return response;
    }

    public static FullHttpResponse responseWithContent(final HttpVersion version, final HttpResponseStatus status,
        final ByteBuf content, final CharSequence mediaType) {
        final var response = new DefaultFullHttpResponse(version, status, content);
        response.headers()
            .set(CONTENT_TYPE, mediaType)
            .setInt(CONTENT_LENGTH, response.content().readableBytes());
        return response;
    }

    public static FullHttpResponse responseWithContent(final HttpVersion version, final ByteBuf content,
        final CharSequence mediaType) {
        return responseWithContent(version, OK, content, mediaType);
    }

    public static FullHttpResponse redirectResponse(final HttpVersion version, final String redirectUrl) {
        return simpleResponse(version, FOUND, LOCATION, redirectUrl);
    }

    public static FullHttpResponse notFoundResponse(final HttpVersion version) {
        return simpleResponse(version, NOT_FOUND);
    }

    public static FullHttpResponse unsupportedMethodResponse(final HttpVersion version) {
        return simpleResponse(version, METHOD_NOT_ALLOWED);
    }

    static FullHttpResponse allowResponse(final HttpVersion version, final String allowHeader) {
        return simpleResponse(version, OK, ALLOW, allowHeader);
    }

    public static ByteBuf contentOf(final String content) {
        return content == null || content.isEmpty()
            ? EMPTY_BUFFER : Unpooled.wrappedBuffer(content.getBytes(StandardCharsets.UTF_8));
    }

    public static String fullUrl(final ChannelHandlerContext ctx, final FullHttpRequest request, final String newUri) {
        final var protocol = ctx.pipeline().get(SslHandler.class) != null ? "https://" : "http://";
        final var host = request.headers().get(HttpHeaderNames.HOST);
        return protocol + host + newUri;
    }
}
