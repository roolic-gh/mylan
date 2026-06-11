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
package local.mylan.service.rest.api;

import java.util.List;
import local.mylan.common.annotations.rest.RequestBody;
import local.mylan.common.annotations.rest.RequestMapping;
import local.mylan.common.annotations.rest.ServiceDescriptor;
import local.mylan.service.api.UserContext;
import local.mylan.service.api.model.Device;
import local.mylan.service.api.model.DeviceAccount;
import local.mylan.service.api.model.NavResourceBookmark;
import local.mylan.service.api.model.NavResourceShare;

@ServiceDescriptor(id = "NavigationService", description = "Navigation service")
public interface NavigationRestService {

    @RequestMapping(method = "GET", path = "/nav/devices")
    List<Device> listDevices();

    @RequestMapping(method = "GET", path = "/nav/accounts")
    List<DeviceAccount> listDeviceAccounts(UserContext userCtx);

    @RequestMapping(method = "POST", path = "/nav/account/validate")
    DeviceAccount validateAccount(@RequestBody DeviceAccount account, UserContext userCtx);

    @RequestMapping(method = "POST", path = "/nav/account/{id}/unlock")
    DeviceAccount lockAccount(UserContext userCtx);

    @RequestMapping(method = "POST", path = "/nav/account/{id}/unlock")
    DeviceAccount unlockAccount(@RequestBody DeviceAccount account, UserContext userCtx);

    @RequestMapping(method = "GET", path = "/nav/shares/all")
    List<NavResourceShare> listShares(UserContext userCtx);

    @RequestMapping(method = "GET", path = "/nav/shares")
    List<NavResourceShare> listUserShares(UserContext userCtx);

    @RequestMapping(method = "GET", path = "/nav/bookmarks")
    List<NavResourceBookmark> listUserBookmarks(UserContext userCtx);

}
