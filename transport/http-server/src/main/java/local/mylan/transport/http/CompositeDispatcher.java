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

import static local.mylan.transport.http.common.RequestUtils.buildRequestContext;
import static local.mylan.transport.http.common.ResponseUtils.fullUrl;
import static local.mylan.transport.http.common.ResponseUtils.redirectResponse;
import static local.mylan.transport.http.common.ResponseUtils.simpleResponse;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import local.mylan.service.api.UserContext;
import local.mylan.transport.http.api.ContextDispatcher;
import local.mylan.transport.http.api.RequestAuthenticator;
import local.mylan.transport.http.api.RequestDispatcher;
import local.mylan.transport.http.common.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompositeDispatcher implements RequestDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(CompositeDispatcher.class);

    private final List<ContextDispatcher> contextDispatchers;
    private final RequestAuthenticator authenticator;
    private final String rootRedirectUri;

    private CompositeDispatcher(final List<ContextDispatcher> contextDispatchers,
        final RequestAuthenticator authenticator, final String rootRedirectUri) {
        this.contextDispatchers = contextDispatchers;
        this.authenticator = authenticator;
        this.rootRedirectUri = rootRedirectUri;
    }

    @Override
    public boolean dispatch(final ChannelHandlerContext ctx, final FullHttpRequest request) {
        final var uri = request.uri();
        if (rootRedirectUri != null && RequestUtils.isRootUri(uri)) {
            final var redirectUrl = fullUrl(ctx, request, rootRedirectUri);
            LOG.debug("redirect to: {}", redirectUrl);
            final var response = redirectResponse(request.protocolVersion(), redirectUrl);
            ctx.writeAndFlush(response);
            return true;
        }
        final UserContext userContext;
        try {
            userContext = authenticator == null
                ? null : authenticator.authenticateUser(request.headers().get(HttpHeaderNames.AUTHORIZATION));
        } catch (Exception e) {
            ctx.writeAndFlush(simpleResponse(request.protocolVersion(), HttpResponseStatus.UNAUTHORIZED));
            return true;
        }
        for (var dispatcher : contextDispatchers) {
            if (uri.startsWith(dispatcher.contextPath())) {
                final var requestContext = buildRequestContext(ctx, request, dispatcher.contextPath(), userContext);
                return dispatcher.dispatch(requestContext);
            }
        }
        return false;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Set<ContextDispatcher> dispatchers = new HashSet<>();
        private RequestAuthenticator authenticator;
        private String rootRedirectUri;

        private Builder() {
            // instantiate through static method above
        }

        public Builder authenticator(RequestAuthenticator authenticator) {
            this.authenticator = authenticator;
            return this;
        }

        public Builder rootRedirectUri(final String rootRedirectUri) {
            this.rootRedirectUri = rootRedirectUri;
            return this;
        }

        public Builder dispatchers(final ContextDispatcher... dispatchers) {
            this.dispatchers.addAll(Arrays.asList(dispatchers));
            return this;
        }

        public Builder dispatcher(final ContextDispatcher dispatcher) {
            dispatchers.add(dispatcher);
            return this;
        }

        public Builder defaultDispatcher(final ContextDispatcher dispatcher) {
            dispatchers.add(dispatcher);
            rootRedirectUri = dispatcher.contextPath();
            return this;
        }

        public RequestDispatcher build() {
            return new CompositeDispatcher(List.copyOf(dispatchers), authenticator, rootRedirectUri);
        }
    }
}
