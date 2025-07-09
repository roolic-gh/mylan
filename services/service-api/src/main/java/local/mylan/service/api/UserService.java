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
package local.mylan.service.api;

import java.util.List;
import java.util.Optional;
import local.mylan.service.api.model.User;
import local.mylan.service.api.model.UserCredentials;
import local.mylan.service.api.model.UserStatus;

public interface UserService {

    Optional<User> getUserByCredentials(UserCredentials credentials);

    void resetUserPassword(Integer userId);

    void changeUserPassword(Integer userId, String oldPassword, String newPassword);

    boolean userMustChangePassword(Integer userId);

    List<User> getUserList(boolean forAdminPurposes);

    Optional<User> getUserById(Integer userId, boolean forAdminPurposes);

    User createUser(User newUser);

    User updateUser(User user);

    void deleteUser(Integer userId);

    void updateUserStatus(UserStatus status);
}
