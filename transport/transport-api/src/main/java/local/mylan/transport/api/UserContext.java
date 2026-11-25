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
package local.mylan.transport.api;

public interface UserContext {

    Integer userId();

    String sessionId();

    boolean isAdmin();

    default boolean isGuest() {
        return userId() != null;
    }

    static UserContext buildDefault(final Integer userId, final boolean isAdmin) {
        return buildDefault(userId, null, isAdmin);
    }

    static UserContext buildDefault(final Integer userId, final String sessionId, final boolean isAdmin) {
        return new DefaultUserContext(userId, sessionId, isAdmin);
    }

    static Integer userIdFrom(final UserContext userCtx) {
        return userCtx == null ? null : userCtx.userId();
    }

    record DefaultUserContext(Integer userId, String sessionId, boolean isAdmin) implements UserContext {
    }
}