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

import static local.mylan.service.api.UserContext.userIdFrom;

import java.util.List;
import java.util.Objects;
import local.mylan.service.api.NavResourceService;
import local.mylan.service.api.UserContext;
import local.mylan.service.api.exceptions.NoDataException;
import local.mylan.service.api.exceptions.UnauthorizedException;
import local.mylan.service.api.model.Device;
import local.mylan.service.api.model.DeviceAccount;
import local.mylan.service.api.model.NavResourceBookmark;
import local.mylan.service.api.model.NavResourceShare;
import local.mylan.service.rest.api.NavResourceRestService;

public class DefaultNavResourceRestService implements NavResourceRestService {

    private final NavResourceService navResoourceService;

    public DefaultNavResourceRestService(final NavResourceService navResoourceService) {
        this.navResoourceService = navResoourceService;
    }

    @Override
    public List<Device> getDevices() {
        return navResoourceService.getAllDevices();
    }

    @Override
    public List<DeviceAccount> listDeviceAccounts(final UserContext userCtx) {
        return navResoourceService.getUserAccounts(userIdFrom(userCtx));
    }

    @Override
    public DeviceAccount getDeviceAccount(final Integer accountId, final UserContext userCtx) {
        final var account = navResoourceService.getAccount(accountId);
        if (account == null) {
            throw new NoDataException("Account with requested id does not exists");
        }
        if (!Objects.equals(account.getUserId(), userIdFrom(userCtx))) {
            throw new UnauthorizedException("Account belongs to other user");
        }
        return account;
    }

    @Override
    public DeviceAccount createDeviceAccount(final DeviceAccount account, final UserContext userCtx) {
        return navResoourceService.createAccount(userIdFrom(userCtx), account);
    }

    @Override
    public DeviceAccount updateDeviceAccount(final Integer accountId, final DeviceAccount account,
        final UserContext userCtx) {

        account.setAccountId(accountId);
        return navResoourceService.updateAccount(userIdFrom(userCtx), account);
    }

    @Override
    public void deleteDeviceAccount(final Integer accountId, final UserContext userCtx) {
        navResoourceService.removeAccount(userIdFrom(userCtx), accountId);
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
