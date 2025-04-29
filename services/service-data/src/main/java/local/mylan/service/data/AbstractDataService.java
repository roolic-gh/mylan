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
import java.util.function.Function;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

class AbstractDataService {
    private final SessionFactory sessionFactory;

    AbstractDataService(final SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    protected void inSession(final Consumer<Session> action) {
        sessionFactory.inSession(action);
    }

    protected <R> R fromSession(final Function<Session, R> action) {
        return sessionFactory.fromSession(action);
    }

    protected void inTransaction(final Consumer<Session> action) {
        sessionFactory.inTransaction(action);
    }

    protected <R> R fromTransaction(final Function<Session, R> action) {
        return sessionFactory.fromTransaction(action);
    }
}
