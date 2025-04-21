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

import local.mylan.service.data.entities.UserCredEntity;
import local.mylan.service.data.entities.UserEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class UserEntityTest extends AbstractEntityTest {

    @BeforeAll
    static void beforeAll() {
        setupDatabase(UserEntity.class, UserCredEntity.class);
    }

    @Test
    void userCrud() {
        final var user = new UserEntity("test", false);
        final var userCred = new UserCredEntity("passw", user);
        sessionFactory.inTransaction(session -> {
            session.persist(user);
            session.persist(userCred);
        });

        final var persisted = sessionFactory.fromSession(session -> {
            return session.createQuery("select u from UserEntity u", UserEntity.class).getSingleResult();
        });

        Assertions.assertNotNull(persisted);
    }
}
