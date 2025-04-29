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
import static org.junit.jupiter.api.Assertions.assertTrue;

import local.mylan.service.api.UserService;
import local.mylan.service.api.model.User;
import local.mylan.service.api.model.UserCredentials;
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
    private static final String USERNAME1 = "user1";
    private static final String USERNAME2 = "user2";
    private static final String USERNAME3 = "user3";
    private static final String PASSWORD = "pa$$W0rd";
    private static final String NAME1 = "name-1";
    private static final String NAME2 = "name-2";
    private static final String NAME3 = "name-3";

    static SessionFactory sessionFactory;
    static UserService userService;

    static User activeUser;
    static User deletedUser;

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
        final var newUser = new User(USERNAME1, NAME1, false);
        final var created = userService.createUser(newUser);
        assertNotNull(created);
        assertNotNull(created.getUserId());
        assertEquals(newUser.getUsername(), created.getUsername());
        assertEquals(newUser.getDisplayName(), created.getDisplayName());
        assertFalse(created.isAdmin());
        final var checkLogin =
            userService.getUserByCredentials(new UserCredentials(USERNAME1, USERNAME1)).orElseThrow();
        assertEquals(created.getUserId(), checkLogin.getUserId());
        activeUser = created;
    }

    @Test
    @Order(3)
    void changeUserPassword() {
        final var userId = activeUser.getUserId();
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
        final var userId = activeUser.getUserId();
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
        // TODO
    }
}
