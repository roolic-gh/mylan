/*
 * Copyright 2026 Ruslan Kashapov
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
package local.mylan.service.rest.spi;

import java.util.List;
import local.mylan.service.api.NavigationService;
import local.mylan.service.api.UserContext;
import local.mylan.service.api.model.Device;
import local.mylan.service.api.model.DeviceAccount;
import local.mylan.service.api.model.NavResourceBookmark;
import local.mylan.service.api.model.NavResourceShare;
import local.mylan.service.rest.api.NavigationRestService;

public class DefaultNavigationRestService implements NavigationRestService {

    private final NavigationService navigationService;

    public DefaultNavigationRestService(final NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    @Override
    public List<Device> listDevices() {
        return navigationService.listDevices();
    }

    @Override
    public List<DeviceAccount> listDeviceAccounts(final UserContext userCtx) {
        return List.of();
    }

    @Override
    public DeviceAccount validateAccount(final DeviceAccount account, final UserContext userCtx) {
        return null;
    }

    @Override
    public DeviceAccount lockAccount(final UserContext userCtx) {
        return null;
    }

    @Override
    public DeviceAccount unlockAccount(final DeviceAccount account, final UserContext userCtx) {
        return null;
    }

    @Override
    public List<NavResourceShare> listShares(final UserContext userCtx) {
        return List.of();
    }

    @Override
    public List<NavResourceShare> listUserShares(final UserContext userCtx) {
        return List.of();
    }

    @Override
    public List<NavResourceBookmark> listUserBookmarks(final UserContext userCtx) {
        return List.of();
    }
}
