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

import java.util.List;
import java.util.Optional;
import local.mylan.service.api.UserService;
import local.mylan.service.api.model.User;
import local.mylan.service.api.model.UserCredentials;
import org.hibernate.SessionFactory;

public class UserDataService implements UserService {

    private final SessionFactory sessionFactory;

    public UserDataService(final SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Optional<User> getUserByCredentials(final UserCredentials credentials) {
        return Optional.empty();
    }

    @Override
    public void resetUserPassword(final Integer userId) {

    }

    @Override
    public void changeUserPassword(final Integer userId, final String oldPassword, final String newPassword) {

    }

    @Override
    public List<User> getUserList(final boolean forAdminPurposes) {
        return List.of();
    }

    @Override
    public Optional<User> getUserById(final Integer userId, final boolean forAdminPurposes) {
        return Optional.empty();
    }

    @Override
    public User createUser(final User newUser) {
        return null;
    }

    @Override
    public User updateUser(final User user) {
        return null;
    }

    @Override
    public void deleteUser(final Integer userId) {

    }
}
