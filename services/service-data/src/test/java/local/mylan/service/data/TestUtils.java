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

import java.util.function.Consumer;
import java.util.stream.Stream;
import local.mylan.service.api.EncryptionService;
import local.mylan.service.api.NotificationService;
import local.mylan.service.api.events.Event;
import local.mylan.service.api.events.Registration;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.JdbcSettings;

final class TestUtils {

    private TestUtils() {
        // utility class
    }

    static SessionFactory setupSessionFactory(final Class<?>... entityClasses) {
        final var builder = new Configuration()
            .setProperty(JdbcSettings.JAKARTA_JDBC_URL, "jdbc:h2:mem:test")
            .setProperty(JdbcSettings.JAKARTA_JDBC_USER, "sa")
            .setProperty(JdbcSettings.JAKARTA_JDBC_PASSWORD, "")
            .setProperty(JdbcSettings.SHOW_SQL, true);
        Stream.of(entityClasses).forEach(builder::addAnnotatedClass);
        final var sessionFactory = builder.buildSessionFactory();
        sessionFactory.getSchemaManager().exportMappedObjects(true);
        return sessionFactory;
    }

    static void tearDownSessionFactory(SessionFactory sessionFactory) {
        if (sessionFactory != null && sessionFactory.isOpen()) {
            sessionFactory.close();
        }
    }

    static NotificationService notificationService() {
        return new NotificationService() {

            @Override
            public <T extends Event> Registration registerEventListener(final Class<T> eventType,
                final Consumer<T> listener) {
                return null;
            }

            @Override
            public void raiseEvent(final Event event) {
            }
        };
    }

    static EncryptionService encryptionService() {
        return new EncryptionService() {
            @Override
            public String encrypt(final String input) {
                return input;
            }

            @Override
            public String encrypt(final String input, final String password) {
                return input;
            }

            @Override
            public String decrypt(final String input) {
                return input;
            }

            @Override
            public String decrypt(final String input, final String password) {
                return input;
            }

            @Override
            public String buildHash(final String input) {
                return input;
            }
        };
    }
}
