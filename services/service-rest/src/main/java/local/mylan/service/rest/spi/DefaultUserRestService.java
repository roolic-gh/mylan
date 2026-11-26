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

import java.util.List;
import java.util.Objects;
import local.mylan.service.api.UserService;
import local.mylan.service.api.exceptions.NoDataException;
import local.mylan.service.api.exceptions.UnauthenticatedException;
import local.mylan.service.api.exceptions.UnauthorizedException;
import local.mylan.service.api.model.User;
import local.mylan.service.api.model.UserCredentials;
import local.mylan.service.api.model.UserStatus;
import local.mylan.service.rest.api.ChangePassword;
import local.mylan.service.rest.api.UserAuthResult;
import local.mylan.service.rest.api.UserRestService;
import local.mylan.transport.api.SessionManager;
import local.mylan.transport.api.UserContext;

public final class DefaultUserRestService implements UserRestService {
    private final UserService service;
    private final SessionManager sessionMgr;

    public DefaultUserRestService(final UserService service, final SessionManager sessionMgr) {
        this.service = service;
        this.sessionMgr = sessionMgr;
    }

    @Override
    public UserAuthResult signIn(final UserCredentials credentials, final boolean persist,
        final UserContext userCtx) {

        final var user = service.getUserByCredentials(credentials).orElse(null);
        if (user != null) {
            final var newCtx = sessionMgr.getOrCreateSession(user, userCtx.ipAddress(), persist);
            final var passwordChangeRequired = service.userMustChangePassword(user.getUserId());
            return new UserAuthResult(user, newCtx.sessionId(), passwordChangeRequired);
        }
        throw new UnauthenticatedException("Invalid username and/or password.");
    }

    @Override
    public void signOut(final UserContext userCtx) {
        sessionMgr.closeSession(userCtx);
    }

    @Override
    public void resetPassword(final Integer userId, final UserContext userCtx) {
        if (!userCtx.isAdmin()) {
            throw new UnauthorizedException("Password reset action is only allowed to administrator.");
        }
        service.resetUserPassword(userId);
        sessionMgr.closeUserSessions(userId);
    }

    @Override
    public void changePassword(final ChangePassword passwordChange, final UserContext userCtx) {
        if (!Objects.equals(userCtx.userId(), passwordChange.getUserId())) {
            throw new UnauthorizedException("Password change is only allowed to owner.");
        }
        service.changeUserPassword(passwordChange.getUserId(), passwordChange.getOldPassword(),
            passwordChange.getNewPassword());
    }

    @Override
    public User createUser(final User newUser, final UserContext userCtx) {
        if (!userCtx.isAdmin()) {
            throw new UnauthorizedException("User creation is only allowed to administrator.");
        }
        if (newUser.getUsername() == null) {
            throw new IllegalArgumentException("Username is required");
        }
        return service.createUser(newUser);
    }

    @Override
    public User updateUser(final User user, final UserContext userCtx) {
        if (user.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (!userCtx.isAdmin() && !Objects.equals(user.getUserId(), userCtx.userId())) {
            throw new UnauthorizedException("User update is only allowed to owner or adminiastrator.");
        }
        final var updated = service.updateUser(user);
        sessionMgr.updateUser(updated);
        return updated;
    }

    @Override
    public void updateUserStatus(final UserStatus status, final UserContext userCtx) {
        if (status.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (!userCtx.isAdmin()) {
            throw new UnauthorizedException("User status update is only allowed to administrator.");
        }
        if (Objects.equals(userCtx.userId(), status.getUserId())) {
            throw new IllegalArgumentException("Administrator cannot update own status.");
        }
        service.updateUserStatus(status);
        if (status.isDisabled()) {
            sessionMgr.closeUserSessions(status.getUserId());
        }
    }

    @Override
    public void deleteUser(final Integer userId, final UserContext userCtx) {
        if (!userCtx.isAdmin()) {
            throw new UnauthorizedException("User deletion is only allowed to administrator.");
        }
        if (Objects.equals(userCtx.userId(), userId)) {
            throw new IllegalArgumentException("Administrator cannot delete himself.");
        }
        service.deleteUser(userId);
        sessionMgr.closeUserSessions(userId);
    }

    @Override
    public User getCurrentUser(final UserContext userCtx) {
        final var user = userCtx.user();
        return user instanceof User casted
            ? casted : new User(user.getUserId(), user.getUsername(), user.getUsername(), user.isAdmin());
    }

    @Override
    public User getUser(final Integer userId, final UserContext userCtx) {
        if (userCtx.userId() == null) {
            throw new UnauthorizedException("User info is not eligible for guests");
        }
        return service.getUserById(userId, userCtx.isAdmin()).orElseThrow(NoDataException::new);
    }

    @Override
    public List<User> getUserList(final UserContext userCtx) {
        return userCtx.userId() == null ? List.of() : service.getUserList(userCtx.isAdmin());
    }
}
