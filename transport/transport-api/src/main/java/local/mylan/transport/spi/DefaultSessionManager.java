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
package local.mylan.transport.spi;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.Hashing;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import local.mylan.common.api.ServiceUser;
import local.mylan.transport.api.SessionManager;
import local.mylan.transport.api.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSessionManager implements SessionManager {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultSessionManager.class);
    @VisibleForTesting
    static final ServiceUser GUEST_USER = guestUser();
    @VisibleForTesting
    static final String AUTH_BEARER = "Bearer ";
    @VisibleForTesting
    static final String COOKIE = "SID";
    @VisibleForTesting
    final Map<String, UserContext> sessions = new ConcurrentHashMap<>();

    public DefaultSessionManager() {
        // default
    }

    @Override
    public UserContext authenticateUser(final HttpHeaders headers, final String ipAddress) {
        final var authHeader = headers.get(HttpHeaderNames.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(AUTH_BEARER)) {
            final var context = sessions.get(authHeader.substring(AUTH_BEARER.length()));
            if (context != null && Objects.equals(context.ipAddress(), ipAddress)) {
                return context;
            }
            throw new IllegalArgumentException("Invalid session ID in Authorization header");
        }

        final var cookieHeader = headers.get(HttpHeaderNames.COOKIE);
        if (cookieHeader != null) {
            final var sessionId = ServerCookieDecoder.STRICT.decodeAll(cookieHeader)
                .stream()
                .filter(cookie -> COOKIE.equals(cookie.name())).findFirst()
                .map(Cookie::value).orElse(null);
            if (sessionId != null) {
                final var context = sessions.get(sessionId);
                if (context != null && Objects.equals(context.ipAddress(), ipAddress)) {
                    return context;
                }
            }
        }
        return new DefaultUserContext(GUEST_USER, null, ipAddress);
    }

    @Override
    public UserContext getOrCreateSession(final ServiceUser user, final String ipAddress, final boolean persist) {
        final var oldCtx = sessions.values().stream()
            .filter(ctx -> Objects.equals(user.getUserId(), ctx.userId())
                && Objects.equals(ipAddress, ctx.ipAddress())).findFirst().orElse(null);
        final var sessionId = oldCtx == null ? newSessionId(user, ipAddress) : oldCtx.sessionId();
        final var newCtx = new DefaultUserContext(user, sessionId, ipAddress);
        sessions.put(sessionId, newCtx);
        onStart(newCtx);
        LOG.debug("Session {} for user {} @ {}",
            oldCtx == null ? "started" : "restored", user.getUsername(), ipAddress);
        return newCtx;
    }

    protected String newSessionId(final ServiceUser user, final String ipAddress) {
        return Hashing.sha256().hashString(user.getUsername() + ipAddress + UUID.randomUUID(),
            StandardCharsets.UTF_8).toString();
    }

    protected void onStart(final UserContext userCtx) {
    }

    protected void onClose(final UserContext userCtx) {
    }

    @Override
    public void closeSession(final UserContext currentContext) {
        final var closed = sessions.remove(currentContext.sessionId());
        if (closed != null) {
            onClose(closed);
            LOG.debug("Session closed for user {} @ {}", closed.user().getUsername(), closed.ipAddress());
        }
    }

    @Override
    public void closeUserSessions(final Integer userId) {
        final var toClose = sessions.values().stream().filter(ctx -> Objects.equals(userId, ctx.userId())).toList();
        for (var ctx : toClose) {
            closeSession(ctx);
        }
    }

    @Override
    public void updateUser(final ServiceUser user) {
        final var toUpdate = sessions.values().stream()
            .filter(ctx -> Objects.equals(user.getUserId(), ctx.userId())).toList();
        for (var ctx : toUpdate) {
            final var updated = new DefaultUserContext(user, ctx.sessionId(), ctx.ipAddress());
            sessions.put(ctx.sessionId(), updated);
        }
    }

    private static ServiceUser guestUser() {
        return new ServiceUser() {
            @Override
            public Integer getUserId() {
                return null;
            }

            @Override
            public String getUsername() {
                return "Guest";
            }

            @Override
            public boolean isAdmin() {
                return false;
            }
        };
    }
}
