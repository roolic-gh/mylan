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
import local.mylan.service.api.model.DeviceAccount;
import local.mylan.service.api.model.DeviceIpAddress;
import local.mylan.service.api.model.NavResource;
import local.mylan.service.api.model.NavResourceBookmark;
import local.mylan.service.api.model.NavResourceShare;

public interface NavResourceService {

    Device getDevice(Integer deviceId);

    List<Device> getAllDevices();

    Device createDevice(Device device);

    void removeDevice(Integer deviceId);

    void addDeviceAddress(Integer deviceId, DeviceIpAddress ipAddress);

    void removeDeviceAddress(Integer deviceId, String ipAddress);

    DeviceAccount getLocalAccount();

    DeviceAccount getAccount(Integer accountId);

    List<DeviceAccount> getAllAccounts();

    List<DeviceAccount> getUserAccounts(Integer userId);

    DeviceAccount createAccount(Integer userId, DeviceAccount account);

    void updateAccount(Integer userId, DeviceAccount account);

    void removeAccount(Integer userId, Integer accountId);

    List<NavResourceShare> getAllSharedResources();

    List<NavResourceShare> getSharedResources(Integer userId);

    NavResourceShare getSharedResource(Long shareId);

    void syncLocalSharedResources(List<NavResourceShare> shares);

    NavResourceShare createSharedResource(Integer userId, NavResourceShare share);

    void updateSharedResource(Integer userId, NavResourceShare share);

    void removeSharedResource(Integer userId, Long shareId);

    List<NavResourceBookmark> getBookmarks(Integer userId);

    NavResourceBookmark getBookmark(Long bookmarkId);

    NavResourceBookmark createBookmark(Integer userId, NavResource resource);

    void removeBookmark(Integer userId, Long bookmarkId);
}
