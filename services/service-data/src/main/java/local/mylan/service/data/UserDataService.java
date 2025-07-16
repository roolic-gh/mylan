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

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Optional;
import local.mylan.service.api.EncryptionService;
import local.mylan.service.api.NotificationService;
import local.mylan.service.api.UserService;
import local.mylan.service.api.model.User;
import local.mylan.service.api.model.UserCredentials;
import local.mylan.service.api.model.UserStatus;
import local.mylan.service.data.entities.Queries;
import local.mylan.service.data.entities.UserCredEntity;
import local.mylan.service.data.entities.UserEntity;
import local.mylan.service.data.mappers.UserModelMapper;
import org.hibernate.SessionFactory;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserDataService extends AbstractDataService implements UserService {
    private static final Logger LOG = LoggerFactory.getLogger(UserDataService.class);
    private static final UserModelMapper MAPPER = Mappers.getMapper(UserModelMapper.class);

    @VisibleForTesting
    static final String ADMIN_USERNAME = "admin";
    @VisibleForTesting
    static final String ADMIN_DISPLAYNAME = "Administrator";

    private final EncryptionService encryptionService;
    private final NotificationService notificationService;

    public UserDataService(final SessionFactory sessionFactory, final EncryptionService encryptionService,
            final NotificationService notificationService) {
        super(sessionFactory);
        this.encryptionService = encryptionService;
        this.notificationService = notificationService;
        checkAdminUserExists();
    }

    void checkAdminUserExists() {
        final var found = fromSession(session ->
            session.createNamedQuery(Queries.GET_ACTIVE_ADMINS_COUNT, Long.class).uniqueResult());
        if (found <1) {
            // TODO handle default admin is disabled case
            LOG.warn("No Admin user found in database, creating default...");
            createUser(new User(ADMIN_USERNAME, ADMIN_DISPLAYNAME, true));
            LOG.warn("Default Admin user created. Password change is required.");
        }
    }

    @Override
    public Optional<User> getUserByCredentials(final UserCredentials credentials) {
        return fromSession(session ->
            session.createNamedQuery(Queries.GET_USER_BY_CREDENTIALS, UserEntity.class)
                .setParameter("username", credentials.getUsername())
                .setParameter("password", toHash(credentials.getPassword()))
                .uniqueResultOptional()
        ).map(MAPPER::fromEntity);
    }

    @Override
    public void resetUserPassword(final Integer userId) {
        final var userEntity = fromSession(session -> session.get(UserEntity.class, userId));
        final int updated = fromTransaction(session ->
            session.createNamedMutationQuery(Queries.RESET_USER_PASSWORD)
                .setParameter("userId", userId)
                .setParameter("password", toHash(userEntity.getUsername()))
                .executeUpdate());
        if (updated < 1) {
            // TODO throw dedicated exception if no entries updated
        }
    }

    @Override
    public void changeUserPassword(final Integer userId, final String oldPassword, final String newPassword) {
        final int updated = fromTransaction(session ->
            session.createNamedMutationQuery(Queries.UPDATE_USER_PASSWORD)
                .setParameter("userId", userId)
                .setParameter("oldPassword", toHash(oldPassword))
                .setParameter("newPassword", toHash(newPassword))
                .executeUpdate());
        if (updated < 1) {
            // TODO throw dedicated exception if no entries updated
        }
    }

    @Override
    public boolean userMustChangePassword(final Integer userId) {
        return fromSession(session ->
            session.createNamedQuery(Queries.GET_USER_MUST_CHANGE_PASSWORD, Boolean.class)
                .setParameter("userId", userId).getSingleResult());
    }

    @Override
    public List<User> getUserList(final boolean forAdminPurposes) {
        final var query = forAdminPurposes ? Queries.GET_ALL_USERS : Queries.GET_ACTIVE_USERS;
        return fromSession(
            session -> session.createNamedQuery(query, UserEntity.class).list()
        ).stream().map(MAPPER::fromEntity).toList();
    }

    @Override
    public Optional<User> getUserById(final Integer userId, final boolean forAdminPurposes) {
        final var userEntity = fromSession(session -> session.get(UserEntity.class, userId));
        return userEntity != null && (!userEntity.isDisabled() || forAdminPurposes)
            ? Optional.of(MAPPER.fromEntity(userEntity)) : Optional.empty();
    }

    @Override
    public User createUser(final User newUser) {
        final var entity = MAPPER.toEntity(newUser);
        final var cred = new UserCredEntity(toHash(newUser.getUsername()), entity, true);
        inTransaction(session -> {
            session.persist(entity);
            session.persist(cred);
        });
        return MAPPER.fromEntity(entity);
    }

    @Override
    public User updateUser(final User user) {
        final var entity = fromTransaction(session -> {
            final var existing = session.get(UserEntity.class, user.getUserId());
            if (existing != null && !existing.isDisabled()) {
                existing.setDisplayName(user.getDisplayName());
                session.persist(existing);
            };
            return existing;
        });
        return MAPPER.fromEntity(entity);
    }

    @Override
    public void updateUserStatus(final UserStatus status) {
        final int updated = fromTransaction(session ->
            session.createNamedMutationQuery(Queries.UPDATE_USER_STATUS)
                .setParameter("userId", status.getUserId())
                .setParameter("disabled", status.isDisabled())
                .executeUpdate());
        if (updated < 1) {
            // TODO throw dedicated exception if no entries updated
        }
    }

    @Override
    public void deleteUser(final Integer userId) {
        final boolean deleted = fromTransaction(session -> {
                final var existing = session.get(UserEntity.class, userId);
                if (existing != null && existing.isDisabled()) {
                    session.remove(existing);
                    return true;
                }
                return false;
            });
        if (!deleted) {
            // TODO throw dedicated exception if no entries deeted
        }
    }

    private String toHash(final String password) {
        return encryptionService.buildHash(password);
    }
}
