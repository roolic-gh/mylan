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
package local.mylan.service.data.mappers;

import local.mylan.service.api.model.Device;
import local.mylan.service.api.model.DeviceCredentials;
import local.mylan.service.api.model.DeviceIpAddress;
import local.mylan.service.api.model.DeviceWithCredentials;
import local.mylan.service.data.entities.DeviceCredEntity;
import local.mylan.service.data.entities.DeviceEntity;
import local.mylan.service.data.entities.DeviceIpAddressEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = UserModelMapper.class)
public interface NavResourceModelMapper {

    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "username", source = "credentials.username")
    @Mapping(target = "keyLocked", source = "credentials")
    Device fromEntity(DeviceEntity entity);

    default boolean keyLocked(DeviceCredEntity credEntity) {
        return credEntity != null && credEntity.getKey() != null;
    }

    @Mapping(target = "user", ignore = true)
    DeviceEntity toEntity(Device model, DeviceCredentials credentials);

    DeviceWithCredentials fromEntityWithCredentials(DeviceEntity entity);

    @Mapping(target = "device", ignore = true)
    @Mapping(target = "deviceId", ignore = true)
    DeviceCredEntity toCredEntity(DeviceCredentials model);

    DeviceCredentials fromCredEntity(DeviceCredEntity entity);

    DeviceIpAddress fromIpAddressEntity(DeviceIpAddressEntity entity);

    @Mapping(target = "device", ignore = true)
    @Mapping(target = "deviceId", ignore = true)
    DeviceIpAddressEntity toIpAddressEntity(DeviceIpAddress model);

}
