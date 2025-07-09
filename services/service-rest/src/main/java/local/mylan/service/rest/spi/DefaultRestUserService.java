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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import local.mylan.service.api.UserContext;
import local.mylan.service.api.UserService;
import local.mylan.service.api.exceptions.NoDataException;
import local.mylan.service.api.exceptions.UnauthenticatedException;
import local.mylan.service.api.exceptions.UnauthorizedException;
import local.mylan.service.api.model.User;
import local.mylan.service.api.model.UserCredentials;
import local.mylan.service.api.model.UserStatus;
import local.mylan.service.rest.api.ChangePassword;
import local.mylan.service.rest.api.RestUserService;
import local.mylan.service.rest.api.UserAuthResult;

public final class DefaultRestUserService implements RestUserService {

    private static final Duration SESSION_EXPIRES = Duration.ofHours(24);
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    @VisibleForTesting
    static final UserContext ANONIMOUS_CONTEXT = new UserContext(new User("anonimous", "Guest", false), "");
    @VisibleForTesting
    static final String AUTH_BASIC = "Basic ";
    @VisibleForTesting
    static final String AUTH_BEARER = "Bearer ";

    private final UserService service;
    private final Cache<String, UserContext> contextCache;

    public DefaultRestUserService(final UserService service) {
        this.service = service;
        contextCache = CacheBuilder.newBuilder().expireAfterAccess(SESSION_EXPIRES).build();
    }

    @Override
    public UserContext authenticate(final String authHeader) {
        if (authHeader == null) {
            return ANONIMOUS_CONTEXT;
        }
        if (authHeader.startsWith(AUTH_BASIC)) {
            final var bytes = Base64.getDecoder().decode(authHeader.substring(AUTH_BASIC.length()));
            final var key = Hashing.sha256().hashBytes(bytes).toString();
            final var context = contextCache.getIfPresent(key);
            if (context != null) {
                return context;
            }
            final var creds = new String(bytes, StandardCharsets.UTF_8).split(":", 2);
            final var user = creds.length == 2 ?
                service.getUserByCredentials(new UserCredentials(creds[0], creds[1])).orElse(null) : null;
            if (user != null) {
                final var newContext = new UserContext(user, key);
                contextCache.put(key, newContext);
                return newContext;
            }
            throw new UnauthenticatedException();
        }
        if (authHeader.startsWith(AUTH_BEARER)) {
            final var context = contextCache.getIfPresent(authHeader.substring(AUTH_BEARER.length()));
            if (context != null) {
                return context;
            }
            throw new UnauthenticatedException();
        }
        return ANONIMOUS_CONTEXT;
    }

    @Override
    public UserAuthResult authenticate(final UserCredentials credentials) {
        final var user = service.getUserByCredentials(credentials).orElse(null);
        if (user != null) {
            final var key = Hashing.sha256().hashString(credentials.getUsername() + COUNTER.incrementAndGet(),
                StandardCharsets.UTF_8).toString();
            final var newContext = new UserContext(user, key);
            contextCache.put(key, newContext);
            final var passwordChangeRequired = service.userMustChangePassword(user.getUserId());
            return new UserAuthResult(user, key, passwordChangeRequired);
        }
        throw new UnauthenticatedException();
    }

    @Override
    public void resetPassword(final Integer userId, final UserContext userCtx) {
        if (!userCtx.currentUser().isAdmin()) {
            throw new UnauthorizedException("Password reset action is only allowed to administrator.");
        }
        service.resetUserPassword(userId);
    }

    @Override
    public void changePassword(final ChangePassword passwordChange, final UserContext userCtx) {
        if (!Objects.equals(userCtx.currentUser().getUserId(), passwordChange.getUserId())) {
            throw new UnauthorizedException("Password change is only allowed to owner.");
        }
        service.changeUserPassword(passwordChange.getUserId(), passwordChange.getOldPassword(),
            passwordChange.getNewPassword());
    }

    @Override
    public User createUser(final User newUser, final UserContext userCtx) {
        if (!userCtx.currentUser().isAdmin()) {
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
        final var currentUser = userCtx.currentUser();
        if (!currentUser.isAdmin() && !Objects.equals(user.getUserId(), currentUser.getUserId())) {
            throw new UnauthorizedException("User update is only allowed to owner or adminiastrator.");
        }
        return service.updateUser(user);
    }

    @Override
    public void updateUserStatus(final UserStatus status, final UserContext userCtx) {
        if (status.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        final var currentUser = userCtx.currentUser();
        if (!currentUser.isAdmin()) {
            throw new UnauthorizedException("User status cabe only updated by administrator.");
        }
        if (Objects.equals(currentUser.getUserId(), status.getUserId())) {
            throw new IllegalArgumentException("Administrator cannot update own status.");
        }
        service.updateUserStatus(status);
    }

    @Override
    public void deleteUser(final Integer userId, final UserContext userCtx) {
        if (!userCtx.currentUser().isAdmin()) {
            throw new UnauthorizedException("User deletion is only allowed to administrator.");
        }
        if (Objects.equals(userCtx.currentUser().getUserId(), userId)) {
            throw new IllegalArgumentException("Administrator cannot delete self.");
        }
        service.deleteUser(userId);
    }

    @Override
    public User getCurrentUser(final UserContext userCtx) {
        return userCtx.currentUser();
    }

    @Override
    public User getUser(final Integer userId, final UserContext userCtx) {
        final var currentUser = userCtx.currentUser();
        if (currentUser.getUserId() == null) {
            throw new UnauthorizedException("User info is not eligible for guests");
        }
        return service.getUserById(userId, currentUser.isAdmin()).orElseThrow(NoDataException::new);
    }

    @Override
    public List<User> getUserList(final UserContext userCtx) {
        final var currentUser = userCtx.currentUser();
        return currentUser.getUserId() == null ? List.of() : service.getUserList(currentUser.isAdmin());
    }
}
