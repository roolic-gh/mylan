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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import local.mylan.service.api.UserContext;
import local.mylan.service.api.UserService;
import local.mylan.service.api.exceptions.NoDataException;
import local.mylan.service.api.exceptions.UnauthenticatedException;
import local.mylan.service.api.exceptions.UnauthorizedException;
import local.mylan.service.api.model.User;
import local.mylan.service.api.model.UserCredentials;
import local.mylan.service.api.model.UserStatus;
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

    private static final Integer ADMIN_ID = 1;
    private static final Integer USER_ID = 2;
    private static final Integer USER2_ID = 3;
    private static final User ADMIN = new User(ADMIN_ID, "Admin", "Name", true);
    private static final User USER = new User(USER_ID, "user", "Name", false);
    private static final User USER2 = new User(USER2_ID, "user2", "Name", false);
    private static final User USER_NO_ID = new User("userx", "", false);
    private static final UserContext ADMIN_CTX = new UserContext(ADMIN, "a");
    private static final UserContext USER_CTX = new UserContext(USER, "u");
    private static final UserContext USER2_CTX = new UserContext(USER2, "u2");
    private static final List<User> USER_LIST = List.of(USER, USER2);
    private static final List<User> USER_LIST_ADM = List.of(ADMIN, USER, USER2);

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
        doReturn(true).when(userService).userMustChangePassword(USER_ID);
        doReturn(Optional.empty()).when(userService).getUserByCredentials(CREDENTIALS2);

        final var authResult = restService.authenticate(new UserCredentials(USERNAME, PASSWORD));
        assertNotNull(authResult);
        assertEquals(USER, authResult.getUser());
        assertTrue(authResult.isMustChangePassword());
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

    @Test
    void createUser() {
        doReturn(USER).when(userService).createUser(USER_NO_ID);
        assertEquals(USER, restService.createUser(USER_NO_ID, ADMIN_CTX));
        assertThrows(UnauthorizedException.class, () -> restService.createUser(USER_NO_ID, USER2_CTX));
        assertThrows(IllegalArgumentException.class, () -> restService.createUser(new User(), ADMIN_CTX));
    }

    @Test
    void updateUser() {
        doReturn(USER).when(userService).updateUser(USER);
        assertEquals(USER, restService.updateUser(USER, USER_CTX));
        assertEquals(USER, restService.updateUser(USER, ADMIN_CTX));
        verify(userService, times(2)).updateUser(USER);
        assertThrows(UnauthorizedException.class, () -> restService.updateUser(USER, USER2_CTX));
    }

    @Test
    void updateUserStatus() {
        doNothing().when(userService).updateUserStatus(any(UserStatus.class));
        final var userStatus = new UserStatus(USER_ID, true);
        restService.updateUserStatus(userStatus, ADMIN_CTX);
        verify(userService, times(1)).updateUserStatus(userStatus);
        assertThrows(UnauthorizedException.class, () -> restService.updateUserStatus(userStatus, USER_CTX));
        assertThrows(IllegalArgumentException.class,
            () -> restService.updateUserStatus(new UserStatus(null, false), ADMIN_CTX));
        assertThrows(IllegalArgumentException.class,
            () -> restService.updateUserStatus(new UserStatus(ADMIN_ID, false), ADMIN_CTX));
    }

    @Test
    void deleteUser() {
        doNothing().when(userService).deleteUser(USER_ID);
        restService.deleteUser(USER_ID, ADMIN_CTX);
        verify(userService, times(1)).deleteUser(USER_ID);
        assertThrows(IllegalArgumentException.class, () -> restService.deleteUser(ADMIN_ID, ADMIN_CTX));
        assertThrows(UnauthorizedException.class, () -> restService.deleteUser(USER_ID, USER2_CTX));
    }

    @Test
    void userById() {
        doReturn(Optional.empty()).when(userService).getUserById(USER_ID, false);
        doReturn(Optional.of(USER)).when(userService).getUserById(USER_ID, true);
        assertEquals(USER, restService.getUser(USER_ID, ADMIN_CTX));
        assertThrows(NoDataException.class, () -> restService.getUser(USER_ID, USER_CTX));
        assertThrows(UnauthorizedException.class, () -> restService.getUser(USER_ID, ANONIMOUS_CONTEXT));
    }

    @Test
    void userList() {
        doReturn(USER_LIST).when(userService).getUserList(false);
        doReturn(USER_LIST_ADM).when(userService).getUserList(true);
        assertEquals(List.of(), restService.getUserList(ANONIMOUS_CONTEXT));
        assertEquals(USER_LIST, restService.getUserList(USER_CTX));
        assertEquals(USER_LIST_ADM, restService.getUserList(ADMIN_CTX));
    }

    @Test
    void currentUser() {
        assertEquals(ANONIMOUS_CONTEXT.currentUser(), restService.getCurrentUser(ANONIMOUS_CONTEXT));
        assertEquals(USER, restService.getCurrentUser(USER_CTX));
        assertEquals(ADMIN, restService.getCurrentUser(ADMIN_CTX));
    }
}
