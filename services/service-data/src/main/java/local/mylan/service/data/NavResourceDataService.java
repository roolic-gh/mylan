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
import local.mylan.service.api.NavResourceService;
import local.mylan.service.api.NotificationService;
import local.mylan.service.api.exceptions.DataCollisionException;
import local.mylan.service.api.exceptions.UnauthorizedException;
import local.mylan.service.api.model.Device;
import local.mylan.service.api.model.DeviceCredentials;
import local.mylan.service.api.model.DeviceIpAddress;
import local.mylan.service.api.model.DeviceProtocol;
import local.mylan.service.api.model.DeviceWithCredentials;
import local.mylan.service.api.model.NavBookmark;
import local.mylan.service.api.model.NavResource;
import local.mylan.service.api.model.NavResourceId;
import local.mylan.service.api.model.ShareType;
import local.mylan.service.data.entities.DeviceEntity;
import local.mylan.service.data.entities.DeviceIpAddressEntity;
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
        if (getLocalDevice() == null) {
            final var localDevice = new DeviceEntity();
            localDevice.setIdentifier(LOCAL_DEVICE_IDENTIFIER);
            localDevice.setProtocol(DeviceProtocol.LOCAL);
            inTransaction(session -> session.persist(localDevice));
        }
    }

    @Override
    public Device getLocalDevice() {
        final var localDevice = fromSession(session ->
            session.createNamedQuery(Queries.GET_LOCAL_DEVICE, DeviceEntity.class).uniqueResult());
        return MAPPER.fromEntity(localDevice);
    }

    @Override
    public DeviceWithCredentials getDevice(final Integer deviceId) {
        final var entity = fromSession(session -> session.get(DeviceEntity.class, deviceId));
        return MAPPER.fromEntityWithCredentials(entity);
    }

    @Override
    public List<DeviceWithCredentials> getAllDevices() {
        return fromSession(session ->
            session.createNamedQuery(Queries.GET_ALL_DEVICES, DeviceEntity.class).getResultList()
        ).stream().map(MAPPER::fromEntityWithCredentials).toList();
    }

    @Override
    public List<Device> getUserDevices(final Integer userId) {
        return fromSession(session ->
            session.createNamedQuery(Queries.GET_DEVICES_BY_USER, DeviceEntity.class)
                .setParameter("userId", userId).getResultList()
        ).stream().map(MAPPER::fromEntity).toList();
    }

    @Override
    public Device createUserDevice(final Integer userId, final Device device,
        final DeviceCredentials credentials) {

        requireNonNull(userId, "userId is required");
        requireNonNull(credentials, "credentials device requred");
        checkArgument(device.getProtocol() != null && device.getProtocol() != DeviceProtocol.LOCAL,
            "illegal device protocol");

        final var entity = MAPPER.toEntity(device, credentials);
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
            return MAPPER.fromEntity(entity);

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
    public List<NavResource> getSharedResources(final Integer userId) {
        return List.of();
    }

    @Override
    public void setResourceShare(final NavResourceId resourceId, final ShareType shareType) {

    }

    @Override
    public void deleteResourceShare(final NavResourceId resourceId) {

    }

    @Override
    public List<NavResource> getBookmarkedResources(final Integer userId) {
        return List.of();
    }

    @Override
    public NavBookmark createBookmark(final NavResourceId resourceId, final Integer userId, final String comment) {
        return null;
    }

    @Override
    public void deleteBookmark(final Long bookmarkId) {

    }
}
