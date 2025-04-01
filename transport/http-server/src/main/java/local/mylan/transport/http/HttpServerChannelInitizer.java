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
package local.mylan.transport.http;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import com.google.common.net.HttpHeaders;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;
import io.netty.handler.ssl.SslContext;
import java.nio.charset.StandardCharsets;
import local.mylan.transport.http.api.RequestDispatcher;

final class HttpServerChannelInitizer extends ChannelInitializer<Channel> {

    private final SslContext sslContext;
    private final RequestDispatcher dispatcher;
    final int maxContentLength;

    HttpServerChannelInitizer(final SslContext sslContext, final RequestDispatcher dispatcher,
        final int maxContentLength) {
        this.sslContext = sslContext;
        this.dispatcher = dispatcher;
        this.maxContentLength = maxContentLength;
    }

    @Override
    protected void initChannel(final Channel channel) throws Exception {
        if (sslContext != null) {
            channel.pipeline().addLast(sslContext.newHandler(channel.alloc()));
        }
        channel.pipeline().addLast(
            new HttpServerCodec(),
            new HttpObjectAggregator(maxContentLength),
            new HttpServerKeepAliveHandler(),

            new SimpleChannelInboundHandler<FullHttpRequest>() {
                @Override
                protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest request) {
                    try {
                        if (!dispatcher.dispatch(ctx, request)) {
                            ctx.writeAndFlush(new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND));
                        }
                    } catch (Exception e) {
                        final var error = e.getMessage().getBytes(StandardCharsets.UTF_8);
                        final var response = new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR,
                            Unpooled.wrappedBuffer(error));
                        response.headers().set(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8");
                        ctx.writeAndFlush(response);
                    }
                }
            });

    }
}
