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
package local.mylan.service.rest.spi;

import static local.mylan.service.rest.spi.DefaultRestUserService.ANONIMOUS_CONTEXT;
import static local.mylan.service.rest.spi.DefaultRestUserService.AUTH_BASIC;
import static local.mylan.service.rest.spi.DefaultRestUserService.AUTH_BEARER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import local.mylan.service.api.UserService;
import local.mylan.service.api.exceptions.UnauthenticatedException;
import local.mylan.service.api.model.User;
import local.mylan.service.api.model.UserCredentials;
import local.mylan.service.rest.api.RestUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefaultRestUserServiceTest {

    private static final String USERNAME = "username";
    private static final String PASSWORD = "pa$$w0Rd";
    private static final String PASSWORD2 = "pa55w0Rd";
    private static final UserCredentials CREDENTIALS = new UserCredentials(USERNAME, PASSWORD);
    private static final UserCredentials CREDENTIALS2 = new UserCredentials(USERNAME, PASSWORD2);
    private static final User ADMIN = new User(1, "Admin", "Name", true);
    private static final User USER = new User(2, "user", "Name", false);

    @Mock
    UserService userService;

    private RestUserService restService;

    @BeforeEach
    void beforeEach() {
        restService = new DefaultRestUserService(userService);
    }

    @Test
    void authBasic() {
        doReturn(Optional.of(USER)).when(userService).getUserByCredentials(CREDENTIALS);
        doReturn(Optional.empty()).when(userService).getUserByCredentials(CREDENTIALS2);

        final var context = restService.authenticate(basicAuthHeader(USERNAME, PASSWORD));
        assertNotNull(context);
        assertEquals(USER, context.currentUser());
        assertNotNull(context.currentSessionId());
        assertFalse(context.currentSessionId().isEmpty());

        assertThrows(UnauthenticatedException.class,
            () -> restService.authenticate(basicAuthHeader(USERNAME, PASSWORD2)));
    }

    private static String basicAuthHeader(final String username, final String password) {
        return AUTH_BASIC + Base64.getEncoder().encodeToString(
            (username + ':' + password).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void authBearer() {
        doReturn(Optional.of(USER)).when(userService).getUserByCredentials(CREDENTIALS);
        doReturn(Optional.empty()).when(userService).getUserByCredentials(CREDENTIALS2);

        final var authResult = restService.authenticate(new UserCredentials(USERNAME, PASSWORD));
        assertNotNull(authResult);
        assertEquals(USER, authResult.getUser());
        final var authToken = authResult.getAuthToken();
        assertNotNull(authToken);

        final var context = restService.authenticate(AUTH_BEARER + authToken);
        assertNotNull(context);
        assertEquals(USER, context.currentUser());

        assertThrows(UnauthenticatedException.class, () -> restService.authenticate(CREDENTIALS2));
        assertThrows(UnauthenticatedException.class, () -> restService.authenticate(AUTH_BEARER + "unknown-token"));
    }

    @Test
    void authGuest() {
        assertEquals(ANONIMOUS_CONTEXT, restService.authenticate((String) null));
    }

}
