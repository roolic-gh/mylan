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
import local.mylan.service.api.UserService;
import local.mylan.service.api.model.User;
import local.mylan.service.api.model.UserCredentials;
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

    public UserDataService(final SessionFactory sessionFactory) {
        super(sessionFactory);
        checkAdminUserExists();
    }

    void checkAdminUserExists() {
        final var found = fromSession(session ->
            session.createNamedQuery(Queries.GET_ACTIVE_ADMINS, UserEntity.class).getSingleResultOrNull());
        if (found == null) {
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
                .setParameter("password", encryptPassword(credentials.getPassword()))
                .uniqueResultOptional()
        ).map(MAPPER::fromEntity);
    }

    @Override
    public void resetUserPassword(final Integer userId) {
        final var userEntity = fromSession(session -> session.get(UserEntity.class, userId));
        final int updated = fromTransaction(session ->
            session.createNamedMutationQuery(Queries.RESET_USER_PASSWORD)
                .setParameter("userId", userId)
                .setParameter("password", encryptPassword(userEntity.getUsername()))
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
                .setParameter("oldPassword", encryptPassword(oldPassword))
                .setParameter("newPassword", encryptPassword(newPassword))
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
        return !userEntity.isDeleted() || forAdminPurposes
            ? Optional.of(MAPPER.fromEntity(userEntity)) : Optional.empty();
    }

    @Override
    public User createUser(final User newUser) {
        final var entity = MAPPER.toEntity(newUser);
        final var cred = new UserCredEntity(encryptPassword(newUser.getUsername()), entity, true);
        inTransaction(session -> {
            session.persist(entity);
            session.persist(cred);
        });
        return MAPPER.fromEntity(entity);
    }

    @Override
    public User updateUser(final User user) {
        return null;
    }

    @Override
    public void deleteUser(final Integer userId) {

    }

    @Override
    public void restoreUser(final Integer userId) {

    }

    private static String encryptPassword(final String password) {
        // TODO encrypt password
        return password;
    }
}
