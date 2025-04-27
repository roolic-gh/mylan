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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import local.mylan.service.api.UserService;
import local.mylan.service.api.model.User;
import local.mylan.service.api.model.UserCredentials;
import local.mylan.service.data.entities.UserCredEntity;
import local.mylan.service.data.entities.UserEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserDataServiceTest extends AbstractEntityTest {

    static UserService userService;

    private User activeUser;
    private User deletedUser;

    @BeforeAll
    static void beforeAll() {
        setupDatabase(UserEntity.class, UserCredEntity.class);
        userService  = new UserDataService(sessionFactory);
    }

    @Test
    @Order(1)
    void veriyAdminUser(){
        final var admin = userService.getUserByCredentials(new UserCredentials("admin", "admin")).orElseThrow();
        assertEquals(UserDataService.ADMIN_USERNAME, admin.getUsername());
        assertEquals(UserDataService.ADMIN_DISPLAYNAME, admin.getDisplayName());
        assertTrue(admin.isAdmin());
    }

    @Test
    @Order(2)
    void createUser(){
        final var newUser = new User("user", "User", false);
        final var created = userService.createUser(newUser);
        assertNotNull(created);
        assertNotNull(created.getUserId());
        assertEquals(newUser.getUsername(), created.getUsername());
        assertEquals(newUser.getDisplayName(), created.getDisplayName());
        assertFalse(created.isAdmin());
        activeUser = created;
    }

    @Test
    @Order(3)
    void updateUser() {
        // TODO
    }
}
