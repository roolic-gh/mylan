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

import static java.util.Objects.requireNonNull;
import static local.mylan.transport.http.utils.RequestUtils.buildRequestContext;
import static local.mylan.transport.http.utils.ResponseUtils.fullUrl;
import static local.mylan.transport.http.utils.ResponseUtils.redirectResponse;
import static local.mylan.transport.http.utils.ResponseUtils.simpleResponse;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import local.mylan.transport.api.ContextDispatcher;
import local.mylan.transport.api.RequestDispatcher;
import local.mylan.transport.api.SessionManager;
import local.mylan.transport.api.UserContext;
import local.mylan.transport.http.utils.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompositeDispatcher implements RequestDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(CompositeDispatcher.class);

    private final List<ContextDispatcher> contextDispatchers;
    private final SessionManager sessionMgr;
    private final String rootRedirectUri;

    private CompositeDispatcher(final List<ContextDispatcher> contextDispatchers,
        final SessionManager sessionMgr, final String rootRedirectUri) {

        this.contextDispatchers = requireNonNull(contextDispatchers);
        this.sessionMgr = requireNonNull(sessionMgr);
        this.rootRedirectUri = requireNonNull(rootRedirectUri);
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
            final var ipAddress = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
            userContext = sessionMgr.authenticateUser(request.headers(), ipAddress);
        } catch (Exception e) {
            LOG.debug("User auth failed --> {}", e.getMessage());
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
        private SessionManager sessionMgr;
        private String rootRedirectUri;

        private Builder() {
            // instantiate through static method above
        }

        public Builder sessionManager(final SessionManager sessionMgr) {
            this.sessionMgr = sessionMgr;
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
            return new CompositeDispatcher(List.copyOf(dispatchers), sessionMgr, rootRedirectUri);
        }
    }
}
