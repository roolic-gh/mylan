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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import local.mylan.service.api.UserService;
import local.mylan.service.api.exceptions.NoDataException;
import local.mylan.service.api.exceptions.UnauthorizedException;
import local.mylan.service.api.model.User;
import local.mylan.service.api.model.UserStatus;
import local.mylan.service.rest.api.UserRestService;
import local.mylan.transport.api.SessionManager;
import local.mylan.transport.api.UserContext;
import local.mylan.transport.spi.DefaultUserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultUserRestServiceTest {

    private static final Integer ADMIN_ID = 101;
    private static final Integer USER_ID = 102;
    private static final Integer USER2_ID = 103;
    private static final User GUEST = new User(null, "", "Guest", false);
    private static final User ADMIN = new User(ADMIN_ID, "Admin", "Name", true);
    private static final User USER = new User(USER_ID, "user", "Name", false);
    private static final User USER2 = new User(USER2_ID, "user2", "Name", false);
    private static final User USER_NO_ID = new User("userx", "", false);
    private static final UserContext GUEST_CTX = new DefaultUserContext(GUEST, null, "192.168.1.100");
    private static final UserContext ADMIN_CTX = new DefaultUserContext(ADMIN, "a", "192.168.1.101");
    private static final UserContext USER_CTX = new DefaultUserContext(USER, "u", "192.168.1.102");
    private static final UserContext USER2_CTX = new DefaultUserContext(USER2, "u2", "192.168.1.103");
    private static final List<User> USER_LIST = List.of(USER, USER2);
    private static final List<User> USER_LIST_ADM = List.of(ADMIN, USER, USER2);

    @Mock
    UserService userService;
    @Mock
    SessionManager sessionMgr;

    private UserRestService restService;

    @BeforeEach
    void beforeEach() {
        restService = new DefaultUserRestService(userService, sessionMgr);
    }

    @Test
    void signIn() {

    }

    @Test
    void signOut() {

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
        final var updated = new User(USER_ID, "user", "Updated", false);
        doReturn(updated).when(userService).updateUser(USER);
        assertEquals(updated, restService.updateUser(USER, USER_CTX));
        assertEquals(updated, restService.updateUser(USER, ADMIN_CTX));
        verify(userService, times(2)).updateUser(USER);
        verify(sessionMgr, times(2)).updateUser(updated);
        assertThrows(UnauthorizedException.class, () -> restService.updateUser(USER, USER2_CTX));
    }

    @Test
    void updateUserStatus() {
        doNothing().when(userService).updateUserStatus(any(UserStatus.class));
        final var userStatus = new UserStatus(USER_ID, true);
        restService.updateUserStatus(userStatus, ADMIN_CTX);
        verify(userService, times(1)).updateUserStatus(userStatus);
        verify(sessionMgr, times(1)).closeUserSessions(USER_ID);

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
        verify(sessionMgr, times(1)).closeUserSessions(USER_ID);
        assertThrows(IllegalArgumentException.class, () -> restService.deleteUser(ADMIN_ID, ADMIN_CTX));
        assertThrows(UnauthorizedException.class, () -> restService.deleteUser(USER_ID, USER2_CTX));
    }

    @Test
    void userById() {
        doReturn(Optional.empty()).when(userService).getUserById(USER_ID, false);
        doReturn(Optional.of(USER)).when(userService).getUserById(USER_ID, true);
        assertEquals(USER, restService.getUser(USER_ID, ADMIN_CTX));
        assertThrows(NoDataException.class, () -> restService.getUser(USER_ID, USER_CTX));
        assertThrows(UnauthorizedException.class, () -> restService.getUser(USER_ID, GUEST_CTX));
    }

    @Test
    void userList() {
        doReturn(USER_LIST).when(userService).getUserList(false);
        doReturn(USER_LIST_ADM).when(userService).getUserList(true);
        assertEquals(List.of(), restService.getUserList(GUEST_CTX));
        assertEquals(USER_LIST, restService.getUserList(USER_CTX));
        assertEquals(USER_LIST_ADM, restService.getUserList(ADMIN_CTX));
    }

    @Test
    void currentUser() {
        assertEquals(GUEST, restService.getCurrentUser(GUEST_CTX));
        assertEquals(USER, restService.getCurrentUser(USER_CTX));
        assertEquals(ADMIN, restService.getCurrentUser(ADMIN_CTX));
    }
}
