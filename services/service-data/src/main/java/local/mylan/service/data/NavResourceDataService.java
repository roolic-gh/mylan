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

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import local.mylan.service.api.NavResourceService;
import local.mylan.service.api.NotificationService;
import local.mylan.service.api.exceptions.DataCollisionException;
import local.mylan.service.api.exceptions.NoDataException;
import local.mylan.service.api.exceptions.UnauthorizedException;
import local.mylan.service.api.model.Device;
import local.mylan.service.api.model.DeviceCredentials;
import local.mylan.service.api.model.DeviceIpAddress;
import local.mylan.service.api.model.DeviceProtocol;
import local.mylan.service.api.model.DeviceWithCredentials;
import local.mylan.service.api.model.NavResource;
import local.mylan.service.api.model.NavResourceBookmark;
import local.mylan.service.api.model.NavResourceShare;
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

    public NavResourceDataService(final SessionFactory sessionFactory, final NotificationService notificationService) {
        super(sessionFactory);
        checkLocalDevice();
    }

    private void checkLocalDevice() {
        if (getLocalDeviceEntity() == null) {
            final var localDevice = new DeviceEntity();
            localDevice.setIdentifier(LOCAL_DEVICE_IDENTIFIER);
            localDevice.setProtocol(DeviceProtocol.LOCAL);
            inTransaction(session -> session.persist(localDevice));
        }
    }

    @Override
    public Device getLocalDevice() {
        return MAPPER.fromDeviceEntity(getLocalDeviceEntity());
    }

    private DeviceEntity getLocalDeviceEntity() {
        return fromSession(session ->
            session.createNamedQuery(Queries.GET_LOCAL_DEVICE, DeviceEntity.class).uniqueResult());
    }

    @Override
    public DeviceWithCredentials getDevice(final Integer deviceId) {
        final var entity = fromSession(session -> session.get(DeviceEntity.class, deviceId));
        return MAPPER.fromDeviceEntityWithCredentials(entity);
    }

    @Override
    public List<DeviceWithCredentials> getAllDevices() {
        return fromSession(session ->
            session.createNamedQuery(Queries.GET_ALL_DEVICES, DeviceEntity.class).getResultList()
        ).stream().map(MAPPER::fromDeviceEntityWithCredentials).toList();
    }

    @Override
    public List<Device> getUserDevices(final Integer userId) {
        return fromSession(session -> session.createNamedQuery(Queries.GET_DEVICES_BY_USER, DeviceEntity.class)
            .setParameter("userId", userId).getResultList()
        ).stream().map(MAPPER::fromDeviceEntity).toList();
    }

    @Override
    public Device createUserDevice(final Integer userId, final Device device,
        final DeviceCredentials credentials) {

        requireNonNull(userId, "userId is required");
        requireNonNull(credentials, "credentials device requred");
        checkArgument(device.getProtocol() != null && device.getProtocol() != DeviceProtocol.LOCAL,
            "illegal device protocol");

        final var entity = MAPPER.toDeviceEntity(device, credentials);
        final var userEntity = fromSession(session -> session.get(UserEntity.class, userId));
        if (userEntity == null) {
            throw new IllegalArgumentException("Unknown user ID " + userId);
        }
        entity.setUser(userEntity);
        // map device id for child entities
        entity.getCredentials().setDevice(entity);
        if (entity.getIpAddresses() != null) {
            entity.getIpAddresses().forEach(ip -> ip.setDevice(entity));
        }
        try {
            inTransaction(session -> {
                session.persist(entity);
            });
            return MAPPER.fromDeviceEntity(entity);

        } catch (ConstraintViolationException e) {
            throw new DataCollisionException("Device with identifier %s already exists."
                .formatted(device.getIdentifier()), e);
        }
    }

    @Override
    public void updateDeviceCredentials(final Integer deviceId, final DeviceCredentials credentials) {
        final var updated = fromTransaction(session ->
            session.createNamedMutationQuery(Queries.UPDATE_DEVICE_CREDENTIALS)
                .setParameter("deviceId", deviceId)
                .setParameter("username", credentials.getUsername())
                .setParameter("password", credentials.getPassword())
                .setParameter("key", credentials.getKey())
                .executeUpdate());
        if (updated < 1) {
            throw new IllegalArgumentException("No credentials exist for device with id " + deviceId);
        }
    }

    private DeviceEntity getValidDeviceEntity(final Integer deviceId) {
        requireNonNull(deviceId, "deviceId is required");
        final var deviceEntity = fromSession(session -> session.get(DeviceEntity.class, deviceId));
        if (deviceEntity == null) {
            throw new IllegalArgumentException("No device founf with id " + deviceId);
        }
        return deviceEntity;
    }

    @Override
    public void addDeviceAddress(final Integer deviceId, final DeviceIpAddress ipAddress) {
        final var deviceEntity = getValidDeviceEntity(deviceId);
        if (deviceEntity.getIpAddresses() != null) {
            for (var deviceIp : deviceEntity.getIpAddresses()) {
                if (deviceIp.getIpAddress().equals(ipAddress.getIpAddress())) {
                    // ip address already assigned to device. ignore
                    return;
                }
            }
        }
        final var addressEntity = MAPPER.toIpAddressEntity(ipAddress);
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
    public void removeDevice(final Integer userId, final Integer deviceId) {
        requireNonNull(userId, "userId is required");
        requireNonNull(deviceId, "deviceId is required");
        inTransaction(session -> {
            final var entity = session.get(DeviceEntity.class, deviceId);
            if (entity != null) {
                if (!userId.equals(entity.getUserId())) {
                    throw new UnauthorizedException("Device does not belong to request user");
                }
                session.remove(entity);
            }
        });
    }

    @Override
    public List<NavResourceShare> getAllSharedResources() {
        return fromSession(session ->
            session.createNamedQuery(Queries.GET_ALL_SHARED_RESOURCES, NavResourceShareEntity.class).getResultList()
        ).stream().map(MAPPER::fromShareEntity).toList();
    }

    @Override
    public List<NavResourceShare> getSharedResources(final Integer userId) {
        if (userId == null) {
            return fromSession(session ->
                session.createNamedQuery(Queries.GET_SHARED_RESOURCES_FOR_GUEST, NavResourceShareEntity.class)
                    .getResultList()
            ).stream().map(MAPPER::fromShareEntityMin).toList();
        }
        return fromSession(session ->
            session.createNamedQuery(Queries.GET_SHARED_RESOURCES_FOR_USER, NavResourceShareEntity.class)
                .setParameter("userId", userId).getResultList()
        ).stream().map(entity ->
            userId.equals(entity.getUserId()) ? MAPPER.fromShareEntity(entity) : MAPPER.fromShareEntityMin(entity)
        ).toList();
    }

    @Override
    public NavResourceShare getSharedResource(final Long shareId) {
        return MAPPER.fromShareEntity(
            fromSession(session -> session.get(NavResourceShareEntity.class, shareId))
        );
    }

    @Override
    public void syncLocalSharedResources(final List<NavResourceShare> shares) {
        requireNonNull(shares);
        inTransaction(session -> {
            final var deviceEntity = getLocalDeviceEntity();
            final var entitiesMap = session.createNamedQuery(Queries.GET_LOCAL_SHARED_RESOURCES,
                    NavResourceShareEntity.class)
                .stream().collect(Collectors.toMap(NavResourceShareEntity::getPath, Function.identity()));
            for (var share : shares) {
                final var entity = entitiesMap.remove(share.getPath());
                if (entity == null) {
                    // new shares
                    final var newEntity = MAPPER.toShareEntity(share);
                    newEntity.setDevice(deviceEntity);
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
    public NavResourceShare addSharedResource(final Integer userId, final NavResourceShare share) {
        requireNonNull(userId, "userId is required");
        requireNonNull(share, "Shared resource data is required");
        final var deviceEntity = getValidDeviceEntity(share.getDeviceId());
        if (!userId.equals(deviceEntity.getUserId())) {
            throw new UnauthorizedException("Resource can be shared by only device owner");
        }
        final var entity = MAPPER.toShareEntity(share);
        entity.setDevice(deviceEntity);
        entity.setUser(deviceEntity.getUser());
        inTransaction(session -> session.persist(entity));
        return MAPPER.fromShareEntity(entity);
    }

    @Override
    public void updateSharedResource(final Integer userId, final NavResourceShare share) {
        requireNonNull(userId, "userId is required");
        requireNonNull(share);
        requireNonNull(share.getShareId(), "shareId is required");
        inTransaction(session -> {
            final var entity = session.get(NavResourceShareEntity.class, share.getShareId());
            if (entity == null) {
                throw new NoDataException("No Shared resource found with shareId = " + share.getShareId());
            }
            if (!userId.equals(entity.getUserId())) {
                throw new UnauthorizedException("Shared resource can be edited by owner only");
            }
            entity.setDisplayName(share.getResourceName());
            entity.setShareType(share.getShareType());
            session.merge(entity);
        });
    }

    @Override
    public void removeSharedResource(final Integer userId, final Long shareId) {
        requireNonNull(userId, "userId is required");
        requireNonNull(shareId, "shareId is required");
        inTransaction(session -> {
            final var entity = session.get(NavResourceShareEntity.class, shareId);
            if (entity == null) {
                return;
            }
            if (!userId.equals(entity.getUserId())) {
                throw new UnauthorizedException("Shared resource can be edited by owner only");
            }
            session.remove(entity);
        });
    }

    @Override
    public List<NavResourceBookmark> getBookmarks(final Integer userId) {
        requireNonNull(userId, "userId is required");
        return fromSession(session ->
            session.createNamedQuery(Queries.GET_USER_BOOKMARKS, NavResourceBookmarkEntity.class)
                .setParameter("userId", userId).getResultList()
        ).stream().map(MAPPER::fromBookmarkEntity).toList();
    }

    @Override
    public NavResourceBookmark getBookmark(final Long bookmarkId) {
        return MAPPER.fromBookmarkEntity(
            fromSession(session -> session.get(NavResourceBookmarkEntity.class, bookmarkId))
        );
    }

    @Override
    public NavResourceBookmark addBookmark(final Integer userId, final NavResource resource) {
        requireNonNull(userId, "userId is required");
        requireNonNull(resource, "Bookmark resource data is required");
        checkArgument(resource.getDeviceId() != null || resource.getShareId() != null,
            "Either deviceId or shareId should be defined in bookmark resource");

        final var entity = new NavResourceBookmarkEntity();
        entity.setPath(resource.getPath());

        if (resource.getDeviceId() != null) {
            // by device
            final var deviceEntity = getValidDeviceEntity(resource.getDeviceId());
            if (!userId.equals(deviceEntity.getUserId())) {
                throw new UnauthorizedException("Resource can be shared by only device owner");
            }
            entity.setDevice(deviceEntity);
            entity.setUser(deviceEntity.getUser());
            inTransaction(session -> session.persist(entity));

        } else {
            // by share
            inTransaction(session -> {
                final var shareEntity = session.get(NavResourceShareEntity.class, resource.getShareId());
                if (shareEntity != null) {
                    throw new NoDataException("No Shared resource found with shareId = " + resource.getShareId());
                }
                final var userEntity = session.get(UserEntity.class, userId);
                if (userEntity == null) {
                    throw new IllegalArgumentException("No user found with id = " + userId);
                }
                entity.setShare(shareEntity);
                entity.setUser(userEntity);
                session.persist(entity);
            });
        }
        return MAPPER.fromBookmarkEntity(entity);
    }

    @Override
    public void removeBookmark(final Integer userId, final Long bookmarkId) {
        requireNonNull(userId, "userId is required");
        inTransaction(session -> {
            final var entity = session.get(NavResourceBookmarkEntity.class, bookmarkId);
            if (!userId.equals(entity.getUserId())) {
                throw new UnauthorizedException("Bookmark can be removed by owner only");
            }
            session.remove(entity);
        });
    }
}
