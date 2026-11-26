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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import java.util.List;
import java.util.Map;
import local.mylan.common.api.ServiceUser;
import local.mylan.transport.api.SessionManager;
import local.mylan.transport.api.UserContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DefaultSessionManagerTest {

    private static final Integer USER_ID1 = 101;
    private static final Integer USER_ID2 = 102;
    private static final String USERNAME1 = "username-1";
    private static final String USERNAME2 = "username-2";
    private static final String USERNAME_UPD = "username-upd";
    private static final ServiceUser USER1 = new TestUser(USER_ID1, USERNAME1);
    private static final ServiceUser USER1_UPD = new TestUser(USER_ID1, USERNAME_UPD);
    private static final ServiceUser USER2 = new TestUser(USER_ID2, USERNAME2);
    private static final String IP1 = "192.168.1.101";
    private static final String IP2 = "192.168.1.102";
    private static final String IP3 = "192.168.1.103";

    static SessionManager sessionManager;
    static Map<String, UserContext> sessions;

    static UserContext ctx11;
    static UserContext ctx12;
    static UserContext ctx13;
    static UserContext ctx21;
    static UserContext ctx22;

    @BeforeAll
    static void beforeAll() {
        final var sm = new DefaultSessionManager();
        sessionManager = sm;
        sessions = sm.sessions;
    }

    @Test
    @Order(10)
    void getOrCreateSession() {
        ctx11 = createAndAssertContext(USER1, IP1);
        ctx12 = createAndAssertContext(USER1, IP2);
        ctx13 = createAndAssertContext(USER1, IP3);
        assertEquals(3, sessions.size());

        ctx21 = createAndAssertContext(USER2, IP1);
        ctx22 = createAndAssertContext(USER2, IP2);
        assertEquals(5, sessions.size());

        // reused sessionId
        final var reused = createAndAssertContext(USER2, IP1);
        assertNotSame(ctx21, reused);
        assertEquals(ctx21, reused);
    }

    private static UserContext createAndAssertContext(ServiceUser user, String ipAddress) {
        final var ctx = sessionManager.getOrCreateSession(user, ipAddress, true); // persisn is not used
        assertNotNull(ctx);
        assertNotNull(ctx.sessionId());
        assertEquals(ctx.user(), user);
        assertEquals(ctx.ipAddress(), ipAddress);
        return ctx;
    }

    @Test
    @Order(20)
    void authenticateUser() {
        for (var ctx : List.of(ctx11, ctx12, ctx13, ctx21, ctx22)) {
            assertEquals(ctx, sessionManager.authenticateUser(bearerHeaders(ctx.sessionId()), ctx.ipAddress()));
            assertEquals(ctx, sessionManager.authenticateUser(cookieHeaders(ctx.sessionId()), ctx.ipAddress()));
        }
        // no match by ip address
        assertThrows(IllegalArgumentException.class,
            () -> sessionManager.authenticateUser(bearerHeaders(ctx22.sessionId()), IP3));
        // guest
        assertAuthGuest(new DefaultHttpHeaders(), IP1);
        assertAuthGuest(cookieHeaders(ctx22.sessionId()), IP3); // fall to guest if bad cookie
    }

    private static void assertAuthGuest(final HttpHeaders headers, final String ipAddress) {
        final var ctx = sessionManager.authenticateUser(headers, ipAddress);
        assertNotNull(ctx);
        assertNull(ctx.sessionId());
        assertEquals(DefaultSessionManager.GUEST_USER, ctx.user());
        assertEquals(ipAddress, ctx.ipAddress());
    }

    private static HttpHeaders bearerHeaders(final String sessionId) {
        final var headers = new DefaultHttpHeaders();
        headers.set(HttpHeaderNames.AUTHORIZATION, DefaultSessionManager.AUTH_BEARER + sessionId);
        return headers;
    }

    private static HttpHeaders cookieHeaders(final String sessionId) {
        final var headers = new DefaultHttpHeaders();
        headers.set(HttpHeaderNames.COOKIE,
            ClientCookieEncoder.STRICT.encode(DefaultSessionManager.COOKIE, sessionId));
        return headers;
    }

    @Test
    @Order(30)
    void updateUser() {
        sessionManager.updateUser(USER1_UPD);
        for (var ctx : List.of(ctx11, ctx12, ctx13)) {
            final var updated = sessions.get(ctx.sessionId());
            assertNotNull(updated);
            assertEquals(USER1_UPD, updated.user());
            assertEquals(ctx.sessionId(), updated.sessionId());
            assertEquals(ctx.ipAddress(), updated.ipAddress());
        }
    }

    @Test
    @Order(40)
    void closeSession() {
        sessionManager.closeSession(ctx22);
        assertEquals(4, sessions.size());
        assertFalse(sessions.containsKey(ctx22.sessionId()));

        sessionManager.closeUserSessions(USER_ID1);
        assertEquals(1, sessions.size());
        assertTrue(sessions.containsKey(ctx21.sessionId())); // the obnly remaining

    }

    private record TestUser(Integer userId, String username) implements ServiceUser {
        @Override
        public Integer getUserId() {
            return userId;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public boolean isAdmin() {
            return false;
        }
    }

}
