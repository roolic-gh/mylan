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
import local.mylan.service.api.model.DeviceAccount;
import local.mylan.service.api.model.DeviceIpAddress;
import local.mylan.service.api.model.NavResourceBookmark;
import local.mylan.service.api.model.NavResourceShare;
import local.mylan.service.data.entities.DeviceAccountEntity;
import local.mylan.service.data.entities.DeviceEntity;
import local.mylan.service.data.entities.DeviceIpAddressEntity;
import local.mylan.service.data.entities.NavResourceBookmarkEntity;
import local.mylan.service.data.entities.NavResourceShareEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = UserModelMapper.class)
public interface NavResourceModelMapper {

    DeviceEntity toEntity(Device model);

    Device fromEntity(DeviceEntity entity);

    @Mapping(target = "deviceId", ignore = true)
    @Mapping(target = "device", ignore = true)
    DeviceIpAddressEntity toEntity(DeviceIpAddress model);

    DeviceIpAddress fromEntity(DeviceIpAddressEntity entity);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "device", ignore = true)
    DeviceAccountEntity toEntity(DeviceAccount model);

    DeviceAccount fromEntity(DeviceAccountEntity entity);

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "key", ignore = true)
    DeviceAccount fromEntityMin(DeviceAccountEntity entity);

    @Mapping(target = "displayName", source = "resourceName")
    @Mapping(target = "accountId", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "user", ignore = true)
    NavResourceShareEntity toEntity(NavResourceShare model);

    @Mapping(target = "resourceName", source = "displayName")
    NavResourceShare fromEntity(NavResourceShareEntity entity);

    default NavResourceShare fromEntityMin(NavResourceShareEntity entity) {
        return entity == null ? null : new NavResourceShare(entity.getShareId(), entity.getDisplayName());
    }

    @Mapping(target = "resourceName", ignore = true)
    NavResourceBookmark fromEntity(NavResourceBookmarkEntity entity);
}
