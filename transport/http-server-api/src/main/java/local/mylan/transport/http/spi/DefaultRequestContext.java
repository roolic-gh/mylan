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
package local.mylan.transport.http.spi;

import static java.util.Objects.requireNonNull;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import java.util.Map;
import local.mylan.service.api.UserContext;
import local.mylan.transport.http.api.RequestContext;

public record DefaultRequestContext(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullRequest,
    Map<String, String> requestParameters, String contextPath, UserContext userContext) implements RequestContext {

    public DefaultRequestContext {
        requireNonNull(channelHandlerContext);
        requireNonNull(fullRequest);
        requireNonNull(requestParameters);
        requireNonNull(contextPath);
    }

    @Override
    public String userId() {
        return userContext == null ? null : userContext.userId();
    }

    @Override
    public HttpVersion protocolVersion() {
        return fullRequest.protocolVersion();
    }

    @Override
    public HttpMethod method() {
        return fullRequest.method();
    }

    @Override
    public HttpHeaders headers() {
        return fullRequest.headers();
    }

    @Override
    public void sendResponse(final FullHttpResponse response) {
        channelHandlerContext.writeAndFlush(response);
    }
}
