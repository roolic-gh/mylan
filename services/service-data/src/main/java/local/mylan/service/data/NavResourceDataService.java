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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import local.mylan.service.api.NavResourceService;
import local.mylan.service.api.NotificationService;
import local.mylan.service.api.exceptions.DataCollisionException;
import local.mylan.service.api.exceptions.UnauthorizedException;
import local.mylan.service.api.model.Device;
import local.mylan.service.api.model.DeviceAccount;
import local.mylan.service.api.model.DeviceIpAddress;
import local.mylan.service.api.model.DeviceProtocol;
import local.mylan.service.api.model.NavResource;
import local.mylan.service.api.model.NavResourceBookmark;
import local.mylan.service.api.model.NavResourceShare;
import local.mylan.service.data.entities.DeviceAccountEntity;
import local.mylan.service.data.entities.DeviceEntity;
import local.mylan.service.data.entities.DeviceIpAddressEntity;
import local.mylan.service.data.entities.NavResourceBookmarkEntity;
import local.mylan.service.data.entities.NavResourceShareEntity;
import local.mylan.service.data.entities.Queries;
import local.mylan.service.data.entities.UserEntity;
import local.mylan.service.data.mappers.NavResourceModelMapper;
import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.mapstruct.factory.Mappers;

public class NavResourceDataService extends AbstractDataService implements NavResourceService {
    private static final NavResourceModelMapper MAPPER = Mappers.getMapper(NavResourceModelMapper.class);

    @VisibleForTesting
    static final String LOCAL_DEVICE_IDENTIFIER = "mylan.server.local";
    @VisibleForTesting
    static final String LOCAL_ACCOUNT_USERNAME = "local";

    final Integer localDeviceId;
    final Integer localAccountId;

    public NavResourceDataService(final SessionFactory sessionFactory, final NotificationService notificationService) {
        super(sessionFactory);
        final var localDevice = getLocalDeviceEntity();
        localDeviceId = localDevice.getDeviceId();
        final var localAccount = getLocalAccountEntity(localDevice);
        localAccountId = localAccount.getAccountId();
    }

    private DeviceEntity getLocalDeviceEntity() {
        return fromTransaction(session -> {
            final var curLocalDevice =
                session.createNamedQuery(Queries.GET_LOCAL_DEVICE, DeviceEntity.class).uniqueResult();
            if (curLocalDevice != null) {
                return curLocalDevice;
            }
            final var newLocalDevice = new DeviceEntity();
            newLocalDevice.setIdentifier(LOCAL_DEVICE_IDENTIFIER);
            newLocalDevice.setProtocol(DeviceProtocol.LOCAL);
            session.persist(newLocalDevice);
            return newLocalDevice;
        });
    }

    private DeviceAccountEntity getLocalAccountEntity(final DeviceEntity localDevice) {
        return fromTransaction(session -> {
            final var curLocalAccount =
                session.createNamedQuery(Queries.GET_LOCAL_ACCOUNT, DeviceAccountEntity.class).uniqueResult();
            if (curLocalAccount != null) {
                return curLocalAccount;
            }
            final var newLocalAccount = new DeviceAccountEntity();
            newLocalAccount.setDevice(localDevice);
            newLocalAccount.setUsername(LOCAL_ACCOUNT_USERNAME);
            session.persist(newLocalAccount);
            return newLocalAccount;
        });
    }

    @Override
    public Device getDevice(final Integer deviceId) {
        return entityById(DeviceEntity.class, deviceId).map(MAPPER::fromEntity).orElse(null);
    }

    @Override
    public List<Device> getAllDevices() {
        return fromSession(session ->
            session.createNamedQuery(Queries.GET_ALL_DEVICES, DeviceEntity.class).getResultList()
        ).stream().map(MAPPER::fromEntity).toList();
    }

    @Override
    public Device createDevice(final Device device) {
        requireNonNull(device);
        checkArgument(device.getProtocol() != null && device.getProtocol() != DeviceProtocol.LOCAL,
            "illegal device protocol");
        final var entity = MAPPER.toEntity(device);
        if (entity.getIpAddresses() != null) {
            entity.getIpAddresses().forEach(ip -> ip.setDevice(entity));
        }
        try {
            inTransaction(session -> session.persist(entity));
        } catch (ConstraintViolationException e) {
            throw new DataCollisionException("Device with identifier %s already exists."
                .formatted(device.getIdentifier()), e);
        }
        return MAPPER.fromEntity(entity);
    }

    @Override
    public void removeDevice(final Integer deviceId) {
        requireNonNull(deviceId, "deviceId is required");
        checkArgument(!deviceId.equals(localDeviceId),
            "Requested device is for internal use only. It cannot be deleted");
        inTransaction(session -> {
            final var entity = session.get(DeviceEntity.class, deviceId);
            if (entity != null) {
                session.remove(entity);
            }
        });
    }

    @Override
    public void addDeviceAddress(final Integer deviceId, final DeviceIpAddress ipAddress) {
        final var deviceEntity = validDeviceEntity(deviceId);
        if (deviceEntity.getIpAddresses() != null) {
            for (var deviceIp : deviceEntity.getIpAddresses()) {
                if (deviceIp.getIpAddress().equals(ipAddress.getIpAddress())) {
                    // ip address already assigned to device. ignore
                    return;
                }
            }
        }
        final var addressEntity = MAPPER.toEntity(ipAddress);
        addressEntity.setDevice(deviceEntity);
        try {
            inTransaction(session -> session.persist(addressEntity));
        } catch (ConstraintViolationException e) {
            throw new DataCollisionException("IP Address %s is already assigned."
                .formatted(ipAddress.getIpAddress()), e);
        }
    }

    @Override
    public void removeDeviceAddress(final Integer deviceId, final String ipAddress) {
        final var entity = fromSession(session -> session.get(DeviceIpAddressEntity.class, ipAddress));
        if (entity == null) {
            return; // already absent
        }
        if (!Objects.equals(deviceId, entity.getDeviceId())) {
            throw new DataCollisionException("IP Address %s does not belong to device with id %d"
                .formatted(ipAddress, deviceId));
        }
        inTransaction(session -> session.remove(entity));
    }

    @Override
    public DeviceAccount getLocalAccount() {
        return getAccount(localAccountId);
    }

    @Override
    public DeviceAccount getAccount(final Integer accountId) {
        return entityById(DeviceAccountEntity.class, accountId).map(MAPPER::fromEntity).orElse(null);
    }

    @Override
    public List<DeviceAccount> getAllAccounts() {
        return fromSession(session ->
            session.createNamedQuery(Queries.GET_ALL_ACCOUNTS, DeviceAccountEntity.class).getResultList()
        ).stream().map(MAPPER::fromEntity).toList();
    }

    @Override
    public List<DeviceAccount> getUserAccounts(final Integer userId) {
        requireNonNull(userId);
        return fromSession(session ->
            session.createNamedQuery(Queries.GET_USER_ACCOUNTS, DeviceAccountEntity.class)
                .setParameter("userId", userId).getResultList()
        ).stream().map(MAPPER::fromEntityMin).toList();
    }

    @Override
    public DeviceAccount createAccount(final Integer userId, final DeviceAccount account) {
        requireNonNull(account, "account data is requred");
        final var deviceEntity = validDeviceEntity(account.getDeviceId());
        final var userEntity = validUserEntity(userId);
        final var entity = MAPPER.toEntity(account);
        entity.setDevice(deviceEntity);
        entity.setUser(userEntity);
        // direct setting foreign key ids allows skipping post-persitence refresh to ensure the entity is up-to-date
        entity.setDeviceId(deviceEntity.getDeviceId());
        entity.setUserId(userEntity.getUserId());
        try {
            inTransaction(session -> session.persist(entity));
            return MAPPER.fromEntity(entity);
        } catch (ConstraintViolationException e) {
            throw new DataCollisionException("Account for user %s at device %s is already defined."
                .formatted(account.getUsername(), deviceEntity.getIdentifier()), e);
        }
    }

    @Override
    public void updateAccount(final Integer userId, final DeviceAccount account) {
        final var entity = validAccountEntity(account.getAccountId());
        validateOwner(userId, entity.getUserId(), "Account can be updated by owner only");
        entity.setUsername(account.getUsername());
        entity.setPassword(account.getPassword());
        entity.setKey(account.getKey());
        inTransaction(session -> session.merge(entity));
    }

    @Override
    public void removeAccount(final Integer userId, final Integer accountId) {
        requireNonNull(accountId, "accountId is required");
        checkArgument(!accountId.equals(localAccountId),
            "Requested account is for internal use only. It cannot be deleted");
        inTransaction(session -> {
            final var entity = session.get(DeviceAccountEntity.class, accountId);
            if (entity != null) {
                validateOwner(userId, entity.getUserId(), "Account can be removed by owner only");
                session.remove(entity);
            }
        });
    }

    @Override
    public List<NavResourceShare> getAllSharedResources() {
        return fromSession(session ->
            session.createNamedQuery(Queries.GET_ALL_SHARED_RESOURCES, NavResourceShareEntity.class).getResultList()
        ).stream().map(MAPPER::fromEntity).toList();
    }

    @Override
    public List<NavResourceShare> getSharedResources(final Integer userId) {
        if (userId == null) {
            return fromSession(session ->
                session.createNamedQuery(Queries.GET_SHARED_RESOURCES_FOR_GUEST, NavResourceShareEntity.class)
                    .getResultList()
            ).stream().map(MAPPER::fromEntityMin).toList();
        }
        return fromSession(session ->
            session.createNamedQuery(Queries.GET_SHARED_RESOURCES_FOR_USER, NavResourceShareEntity.class)
                .setParameter("userId", userId).getResultList()
        ).stream().map(entity ->
            userId.equals(entity.getUserId()) ? MAPPER.fromEntity(entity) : MAPPER.fromEntityMin(entity)
        ).toList();
    }

    @Override
    public NavResourceShare getSharedResource(final Long shareId) {
        return entityById(NavResourceShareEntity.class, shareId).map(MAPPER::fromEntity).orElse(null);
    }

    @Override
    public void syncLocalSharedResources(final List<NavResourceShare> shares) {
        requireNonNull(shares);
        // ensure no paths repeated
        final var uniquePaths =
            shares.stream().collect(toMap(NavResourceShare::getPath, Function.identity())).values();
        inTransaction(session -> {
            final var localAccount = session.get(DeviceAccountEntity.class, localAccountId);
            final var entitiesMap = session
                    .createNamedQuery(Queries.GET_LOCAL_SHARED_RESOURCES, NavResourceShareEntity.class)
                    .stream().collect(toMap(NavResourceShareEntity::getPath, Function.identity()));
            for (var share : uniquePaths) {
                final var entity = entitiesMap.remove(share.getPath());
                if (entity == null) {
                    // new shares
                    final var newEntity = MAPPER.toEntity(share);
                    newEntity.setAccount(localAccount);
                    session.persist(newEntity);
                    continue;
                }
                if (!entity.getDisplayName().equals(share.getResourceName())
                    || entity.getShareType() != share.getShareType()) {
                    // update properties
                    entity.setDisplayName(share.getResourceName());
                    entity.setShareType(share.getShareType());
                    session.merge(entity);
                }
            }
            // remove unlisted
            entitiesMap.values().forEach(session::remove);
        });
    }

    @Override
    public NavResourceShare createSharedResource(final Integer userId, final NavResourceShare share) {
        requireNonNull(share, "Shared resource data is required");
        final var accountEntity = validAccountEntity(share.getAccountId());
        final var userEntity = validUserEntity(userId);
        validateOwner(userId, accountEntity.getUserId(), "Resouurce can be shared by account owner only");
        final var entity = MAPPER.toEntity(share);
        entity.setAccount(accountEntity);
        entity.setUser(userEntity);
        // direct setting foreign key ids allows skipping post-persitence refresh to ensure the entity is up-to-date
        entity.setAccountId(accountEntity.getAccountId());
        entity.setUserId(userEntity.getUserId());
        inTransaction(session -> session.persist(entity));
        return MAPPER.fromEntity(entity);
    }

    @Override
    public void updateSharedResource(final Integer userId, final NavResourceShare share) {
        requireNonNull(share);
        final var entity = validShareEntity(share.getShareId());
        validateOwner(userId, entity.getUserId(), "Shared resource can be edited by owner only");
        entity.setDisplayName(share.getResourceName());
        entity.setShareType(share.getShareType());
        inTransaction(session -> session.merge(entity));
    }

    @Override
    public void removeSharedResource(final Integer userId, final Long shareId) {
        requireNonNull(shareId, "shareId is required");
        inTransaction(session -> {
            final var entity = session.get(NavResourceShareEntity.class, shareId);
            if (entity != null) {
                validateOwner(userId, entity.getUserId(), "Shared resource can be removed by owner only");
                session.remove(entity);
            }
        });
    }

    @Override
    public List<NavResourceBookmark> getBookmarks(final Integer userId) {
        requireNonNull(userId, "userId is required");
        return fromSession(session ->
            session.createNamedQuery(Queries.GET_USER_BOOKMARKS, NavResourceBookmarkEntity.class)
                .setParameter("userId", userId).getResultList()
        ).stream().map(MAPPER::fromEntity).toList();
    }

    @Override
    public NavResourceBookmark getBookmark(final Long bookmarkId) {
        return entityById(NavResourceBookmarkEntity.class, bookmarkId).map(MAPPER::fromEntity).orElse(null);
    }

    @Override
    public NavResourceBookmark createBookmark(final Integer userId, final NavResource resource) {
        requireNonNull(resource, "Bookmark resource data is required");
        checkArgument(resource.getAccountId() != null || resource.getShareId() != null,
            "Either accountId or shareId should be defined in bookmark resource");

        final var entity = new NavResourceBookmarkEntity();
        entity.setUser(validUserEntity(userId));
        entity.setUserId(userId);
        entity.setPath(resource.getPath());
        if (resource.getAccountId() != null) {
            // by account
            final var accountEntity = validAccountEntity(resource.getAccountId());
            validateOwner(userId, accountEntity.getUserId(), "Resource can be shared by account owner only");
            entity.setAccount(accountEntity);
            entity.setAccountId(accountEntity.getAccountId());
        } else {
            // by share
            final var shareEntity = validShareEntity(resource.getShareId());
            entity.setShare(shareEntity);
            entity.setShareId(shareEntity.getShareId());
        }
        inTransaction(session -> session.persist(entity));
        return MAPPER.fromEntity(entity);
    }

    @Override
    public void removeBookmark(final Integer userId, final Long bookmarkId) {
        requireNonNull(bookmarkId, "bookmarkId is required");
        inTransaction(session -> {
            final var entity = session.get(NavResourceBookmarkEntity.class, bookmarkId);
            if (entity != null) {
                validateOwner(userId, entity.getUserId(), "Bookmark can be removed by owner only");
                session.remove(entity);
            }
        });
    }

    private UserEntity validUserEntity(final Integer userId) {
        requireNonNull(userId, "userId is required");
        return entityById(UserEntity.class, userId)
            .orElseThrow(() -> new IllegalArgumentException("No user found with userId = " + userId));
    }

    private DeviceEntity validDeviceEntity(final Integer deviceId) {
        requireNonNull(deviceId, "deviceId is required");
        checkArgument(!deviceId.equals(localDeviceId), "Requested device is for internal use only.");
        return entityById(DeviceEntity.class, deviceId)
            .orElseThrow(() -> new IllegalArgumentException("No device found with deviceId = " + deviceId));
    }

    private DeviceAccountEntity validAccountEntity(final Integer accountId) {
        requireNonNull(accountId, "accountId is required");
        checkArgument(!accountId.equals(localAccountId), "Requested account is for internal use only.");
        return entityById(DeviceAccountEntity.class, accountId)
            .orElseThrow(() -> new IllegalArgumentException("No account found with accountId = " + accountId));
    }

    private NavResourceShareEntity validShareEntity(final Long shareId) {
        requireNonNull(shareId, "shareId is required");
        return entityById(NavResourceShareEntity.class, shareId)
            .orElseThrow(() -> new IllegalArgumentException("No shared resource found with shareId = " + shareId));
    }

    private static void validateOwner(final Integer userId, final Integer ownerId, final String errorMessage) {
        requireNonNull(userId, "userId is required");
        if (!Objects.equals(userId, ownerId)) {
            throw new UnauthorizedException(errorMessage);
        }
    }
}
