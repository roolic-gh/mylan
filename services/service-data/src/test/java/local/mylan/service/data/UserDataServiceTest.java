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
package local.mylan.service.data;

import static local.mylan.service.data.TestUtils.setupSessionFactory;
import static local.mylan.service.data.TestUtils.tearDownSessionFactory;
import static local.mylan.service.data.UserDataService.ADMIN_USERNAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import local.mylan.service.api.UserService;
import local.mylan.service.api.model.User;
import local.mylan.service.api.model.UserCredentials;
import local.mylan.service.api.model.UserStatus;
import local.mylan.service.data.entities.UserCredEntity;
import local.mylan.service.data.entities.UserEntity;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserDataServiceTest {
    private static final String USERNAME1 = "username1";
    private static final String USERNAME2 = "username2";
    private static final String PASSWORD = "pa$$W0rd";
    private static final String NAME1 = "name-1";
    private static final String NAME2 = "name-2";
    private static final boolean IS_ADMIN = true;
    private static final boolean NOT_ADMIN = false;
    private static final boolean FOR_ADMIN_PURPOSES = true;
    private static final boolean IS_DISABLED = true;
    private static final boolean IS_ENABLED = false;

    static SessionFactory sessionFactory;
    static UserService userService;
    static User persistedUser;

    @BeforeAll
    static void beforeAll() {
        sessionFactory = setupSessionFactory(UserEntity.class, UserCredEntity.class);
        userService = new UserDataService(sessionFactory);
    }

    @AfterAll
    static void afterAll() {
        tearDownSessionFactory(sessionFactory);
    }

    @Test
    @Order(1)
    void veriyAdminUser() {
        final var admin =
            userService.getUserByCredentials(new UserCredentials(ADMIN_USERNAME, ADMIN_USERNAME)).orElseThrow();
        assertEquals(ADMIN_USERNAME, admin.getUsername());
        assertEquals(UserDataService.ADMIN_DISPLAYNAME, admin.getDisplayName());
        assertTrue(admin.isAdmin());
        assertTrue(userService.userMustChangePassword(admin.getUserId()));
    }

    @Test
    @Order(2)
    void createUser() {
        final var newUser = new User(USERNAME1, NAME1, NOT_ADMIN);
        final var created = userService.createUser(newUser);
        assertNotNull(created);
        assertNotNull(created.getUserId());
        assertEquals(newUser.getUsername(), created.getUsername());
        assertEquals(newUser.getDisplayName(), created.getDisplayName());
        assertFalse(created.isAdmin());
        final var checkLogin =
            userService.getUserByCredentials(new UserCredentials(USERNAME1, USERNAME1)).orElseThrow();
        assertEquals(created.getUserId(), checkLogin.getUserId());
        persistedUser = created;
    }

    @Test
    @Order(3)
    void changeUserPassword() {
        final var userId = persistedUser.getUserId();
        assertTrue(userService.userMustChangePassword(userId));
        userService.changeUserPassword(userId, USERNAME1, PASSWORD);
        final var checkLogin =
            userService.getUserByCredentials(new UserCredentials(USERNAME1, PASSWORD)).orElseThrow();
        assertEquals(userId, checkLogin.getUserId());
        assertFalse(userService.userMustChangePassword(userId));
    }

    @Test
    @Order(4)
    void resetUserPassword() {
        final var userId = persistedUser.getUserId();
        assertFalse(userService.userMustChangePassword(userId));
        userService.resetUserPassword(userId);
        final var checkLogin =
            userService.getUserByCredentials(new UserCredentials(USERNAME1, USERNAME1)).orElseThrow();
        assertEquals(userId, checkLogin.getUserId());
        assertTrue(userService.userMustChangePassword(userId));
    }

    @Test
    @Order(5)
    void updateUser() {
        final var userId = persistedUser.getUserId();
        final var updated = userService.updateUser(new User(userId, USERNAME2, NAME2, IS_ADMIN));
        // only display name updated, other changes ignored
        assertEquals(userId, updated.getUserId());
        assertEquals(USERNAME1, updated.getUsername());
        assertEquals(NAME2, updated.getDisplayName());
        assertFalse(updated.isAdmin());
        // nonexistent entry is not updated
        assertNull(userService.updateUser(new User(userId + 100, USERNAME2, NAME2, IS_ADMIN)));
    }

    @Test
    @Order(6)
    void updateUserStatus() {
        final var userId = persistedUser.getUserId();
        // disable user
        assertFalse(userService.getUserById(userId, FOR_ADMIN_PURPOSES).orElseThrow().isDisabled());
        userService.updateUserStatus(new UserStatus(userId, IS_DISABLED));
        assertTrue(userService.getUserById(userId, FOR_ADMIN_PURPOSES).orElseThrow().isDisabled());
        // ensure diabled user is not allowed for update
        final var updated = userService.updateUser(new User(userId, USERNAME1, NAME1, IS_ADMIN));
        assertNotNull(updated);
        assertEquals(NAME2, updated.getDisplayName());
        // enable user
        userService.updateUserStatus(new UserStatus(userId, IS_ENABLED));
        assertFalse(userService.getUserById(userId, FOR_ADMIN_PURPOSES).orElseThrow().isDisabled());
    }

    @Test
    @Order(7)
    void deleteUser() {
        final var userId = persistedUser.getUserId();
        // active user cannot be deleted
        userService.deleteUser(userId);
        assertTrue(userService.getUserById(userId, FOR_ADMIN_PURPOSES).isPresent());
        // disable then delete user
        userService.updateUserStatus(new UserStatus(userId, IS_DISABLED));
        userService.deleteUser(userId);
        assertTrue(userService.getUserById(userId, FOR_ADMIN_PURPOSES).isEmpty());
    }
}
