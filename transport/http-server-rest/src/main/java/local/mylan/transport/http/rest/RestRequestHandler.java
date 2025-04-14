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
package local.mylan.transport.http.rest;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static local.mylan.transport.http.common.ResponseUtils.responseWithContent;
import static local.mylan.transport.http.common.ResponseUtils.simpleResponse;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import local.mylan.service.api.rest.ErrorMessage;
import local.mylan.transport.http.api.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RestRequestHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RestRequestHandler.class);

    private final String id;
    private final HttpMethod httpMethod;
    private final RestPathMatcher pathMatcher;
    private final Method method;
    private final Object serviceInstance;
    private final List<RestArgBuilder> argBuilders;

    RestRequestHandler(final HttpMethod httpMethod, final RestPathMatcher pathMatcher, final Method method,
        final Object serviceInstance) {
        this.httpMethod = httpMethod;
        this.pathMatcher = pathMatcher;
        this.method = method;
        this.serviceInstance = serviceInstance;
        id = "%s.%s".formatted(serviceInstance.getClass().getSimpleName(), method.getName());
        argBuilders = Arrays.stream(method.getParameters()).map(RestArgUtils::getArgBuilder).toList();
        LOG.debug("{} handler initialized", id);
        System.out.println("+" + id);
    }

    boolean httpMethodMatches(final HttpMethod httpMethod) {
        return this.httpMethod.equals(httpMethod);
    }

    RestPathMatcher pathMatcher() {
        return pathMatcher.newInstance();
    }

    void processRequest(final RequestContext ctx, final Map<String, String> pathParameters) {
        final var responseMediaType = RestConverter.mediaTypeFrom(ctx.headers().get(HttpHeaderNames.ACCEPT));
        try {
            final var args = argBuilders.stream()
                .map(builder -> builder.buildArgObject(ctx, pathParameters))
                .toArray(Object[]::new);
            final var result = method.invoke(serviceInstance, args);
            if (result == null) {
                ctx.sendResponse(simpleResponse(ctx.protocolVersion(), NO_CONTENT));
            } else {
                final var content = RestConverter.toResponseBody(result, responseMediaType);
                ctx.sendResponse(responseWithContent(ctx.protocolVersion(), OK, content, responseMediaType));
            }
        } catch (Exception e) {
            LOG.error("Exception processing request {}", ctx.contextPath(), e);
            final var content = RestConverter.toResponseBody(new ErrorMessage(e.getMessage()), responseMediaType);
            // todo response code by exception type
            ctx.sendResponse(responseWithContent(ctx.protocolVersion(), INTERNAL_SERVER_ERROR,
                content, responseMediaType));
        }
    }

}
