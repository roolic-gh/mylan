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
import local.mylan.service.api.model.DeviceState;
import local.mylan.service.api.model.NavDirectory;
import local.mylan.service.api.model.NavResourceBookmark;
import local.mylan.service.api.model.NavResourceShare;

public interface NavigationService {

    List<Device> getDevicesForUser(Integer userId);

    List<DeviceState> getDeviceStatesForUser(Integer userId);

    void unlockDevice(Integer userId, Integer deviceId, String key);

    void lockDevice(Integer userId, Integer deviceId);

    NavDirectory getDeviceDirectoryContent(Integer userId, Integer deviceId, String path);

    NavDirectory getSharedDirectoryContent(Integer userId, Long shareId, String path);

    List<NavResourceShare> getSharedResources(Integer userId);

    List<NavResourceBookmark> getBookmarks(Integer userId);
}
