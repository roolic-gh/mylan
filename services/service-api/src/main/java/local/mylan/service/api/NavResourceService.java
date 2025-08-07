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
import local.mylan.service.api.model.Device;
import local.mylan.service.api.model.DeviceCredentials;
import local.mylan.service.api.model.DeviceIpAddress;
import local.mylan.service.api.model.DeviceWithCredentials;
import local.mylan.service.api.model.NavBookmark;
import local.mylan.service.api.model.NavResourceBookmark;
import local.mylan.service.api.model.NavResourceShare;

public interface NavResourceService {

    Device getLocalDevice();

    DeviceWithCredentials getDevice(Integer deviceId);

    List<DeviceWithCredentials> getAllDevices();

    List<Device> getUserDevices(Integer userId);

    Device createUserDevice(Integer userId, Device deviceInfo, DeviceCredentials credentials);

    void updateDeviceCredentials(Integer deviceId, DeviceCredentials credentials);

    void addDeviceAddress(Integer deviceId, DeviceIpAddress ipAddress);

    void removeDeviceAddress(Integer deviceId, String ipAddress);

    void removeDevice(Integer userId, Integer deviceId);

    List<NavResourceShare> getSharedResources(Integer userId);

    void addSharedResource(NavResourceShare share);

    void removeSharedResource(Long shareId);

    List<NavResourceBookmark> getBookmarkedResources(Integer userId);

    NavBookmark addBookmark(Integer userId, Long shareId, String path);

    void removeBookmark(Long bookmarkId);
}
