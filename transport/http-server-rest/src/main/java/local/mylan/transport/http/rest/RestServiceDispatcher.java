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

import io.netty.handler.codec.http.HttpMethod;
import java.util.ArrayList;
import java.util.List;
import local.mylan.common.annotations.rest.RequestMapping;
import local.mylan.transport.http.api.ContextDispatcher;
import local.mylan.transport.http.api.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RestServiceDispatcher implements ContextDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(RestServiceDispatcher.class);

    private final String contextPath;
    private final List<RestRequestHandler> handlers;

    public RestServiceDispatcher(final String contextPath, final Object... serviceInstances) {
        this.contextPath = contextPath;
        handlers = buildHandlers(serviceInstances);
    }

    @Override
    public String contextPath() {
        return contextPath;
    }

    @Override
    public boolean dispatch(final RequestContext ctx) {
        for (var handler : handlers) {
            if(handler.httpMethodMatches(ctx.method())){
            final var matcher = handler.pathMatcher();
            if (matcher.matches(ctx.contextPath())) {
                handler.processRequest(ctx, matcher.pathParameters());
                return true;
            }}
        }
        return false;
    }

    private static List<RestRequestHandler> buildHandlers(final Object[] serviceInstances) {
        final var result = new ArrayList<RestRequestHandler>();
        for (var serviceInstance : serviceInstances) {
            final var cls = serviceInstance.getClass();
            for (var classMethod : AnnotationUtils.getAnnotatedMethods(cls, RequestMapping.class)) {
                final var mapping = classMethod.getAnnotation(RequestMapping.class);
                try {
                    final var httpMethod = HttpMethod.valueOf(mapping.method());
                    final var pathMatcher = RestPathUtils.getMatcher(mapping.path());
                    result.add(new RestRequestHandler(httpMethod, pathMatcher, classMethod, serviceInstance));
                } catch (IllegalArgumentException e) {
                    LOG.error("Exception on building handler for {}.{}()", serviceInstance.getClass(), classMethod.getName(), e);
                }
            }
        }
        return List.copyOf(result);
    }
}
