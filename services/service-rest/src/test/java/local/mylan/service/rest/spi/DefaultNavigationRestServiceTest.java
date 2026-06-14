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

import static local.mylan.service.api.model.DeviceAccountLockState.LOCKED;
import static local.mylan.service.api.model.DeviceAccountLockState.UNLOCKED;
import static local.mylan.service.api.model.DeviceAccountState.INVALID;
import static local.mylan.service.api.model.DeviceAccountState.VALID;
import static local.mylan.service.api.model.DeviceProtocol.NFS;
import static local.mylan.service.api.model.DeviceProtocol.SMB;
import static local.mylan.service.api.model.DeviceState.OFFLINE;
import static local.mylan.service.api.model.DeviceState.ONLINE;
import static local.mylan.service.test.NavResourceTestUtils.assertAccount;
import static local.mylan.service.test.NavResourceTestUtils.assertAccountList;
import static local.mylan.service.test.NavResourceTestUtils.assertDeviceList;
import static local.mylan.service.test.NavResourceTestUtils.device;
import static local.mylan.service.test.NavResourceTestUtils.deviceAccount;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

import java.util.List;
import local.mylan.service.api.NavigationService;
import local.mylan.service.api.UserContext;
import local.mylan.service.api.exceptions.UnauthenticatedException;
import local.mylan.service.api.exceptions.UnauthorizedException;
import local.mylan.service.api.model.Device;
import local.mylan.service.api.model.DeviceAccount;
import local.mylan.service.api.model.User;
import local.mylan.service.rest.api.NavigationRestService;
import local.mylan.service.rest.api.UnlockRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultNavigationRestServiceTest {

    private static final Integer DEVICE_ID1 = 1;
    private static final Integer DEVICE_ID2 = 2;
    private static final String DEVICE_NAME1 = "NAME1";
    private static final String DEVICE_NAME2 = "NAME2";
    private static final String IP1 = "192.168.1.101";
    private static final String IP2 = "192.168.1.102";
    private static final String IP3 = "192.168.1.103";

    private static final Device DEVICE1 = device(DEVICE_ID1, DEVICE_NAME1, SMB, List.of(IP1), ONLINE);
    private static final Device DEVICE2 = device(DEVICE_ID2, DEVICE_NAME2, NFS, List.of(IP2), OFFLINE);

    private static final String USERNAME1 = "username1";
    private static final String USERNAME2 = "username2";
    private static final String PASSWORD1 = "PaS$W0rd";
    private static final String PASSWORD2 = "Pa$SW0rd";
    private static final String KEY = "12345";

    private static final Integer USER_ID1 = 1001;
    private static final Integer USER_ID2 = 1002;
    private static final UserContext USER_CTX1 = new UserContext(new User(USER_ID1, "A", "User1", false), null);
    private static final UserContext USER_CTX2 = new UserContext(new User(USER_ID2, "B", "User2", false), null);
    private static final UserContext GUEST_CTX = new UserContext(new User("G", "Guest", false), null);

    private static final Integer ACCOUNT_ID1 = 2001;
    private static final Integer ACCOUNT_ID2 = 2002;
    private static final Integer ACCOUNT_INVALID = 2006;

    @Mock
    NavigationService navigationService;

    NavigationRestService restService;

    @BeforeEach
    void beforeEach() {
        restService = new DefaultNavigationRestService(navigationService);
    }

    @Test
    void listDevices() {
        final var devices = List.of(DEVICE1, DEVICE2);
        doReturn(devices).when(navigationService).listDevices();
        assertDeviceList(devices, restService.listDevices());
    }

    @Test
    void listAccounts() {
        final var accounts = List.of(
            deviceAccount(ACCOUNT_ID1, USER_ID1, DEVICE_ID1, USERNAME1),
            deviceAccount(ACCOUNT_ID2, USER_ID1, DEVICE_ID1, USERNAME2));
        doReturn(accounts).when(navigationService).listUserDeviceAccounts(USER_ID1);
        assertAccountList(accounts, restService.listDeviceAccounts(USER_CTX1));
        assertEquals(List.of(), restService.listDeviceAccounts(GUEST_CTX));
    }

    @Test
    void validateAccount() {
        final var reqAccount1 = new DeviceAccount(DEVICE_ID1, USERNAME1, PASSWORD1);
        final var respAccount1 = deviceAccount(null, null, DEVICE_ID1, USERNAME1, VALID, null);
        final var reqAccount2 = new DeviceAccount(DEVICE_ID2, USERNAME2, PASSWORD2);
        final var respAccount2 = deviceAccount(null, null, DEVICE_ID2, USERNAME2, INVALID, null);
        doReturn(respAccount1).when(navigationService).validateAccount(reqAccount1);
        doReturn(respAccount2).when(navigationService).validateAccount(reqAccount2);

        assertAccount(respAccount1, restService.validateAccount(reqAccount1, USER_CTX1));
        assertThrows(UnauthenticatedException.class, () -> restService.validateAccount(reqAccount1, GUEST_CTX));
        assertThrows(UnauthorizedException.class, () -> restService.validateAccount(reqAccount2, USER_CTX1));
    }

    @Test
    void unlockAccount() {
        final var account1 = deviceAccount(ACCOUNT_ID1, USER_ID1, DEVICE_ID1, USERNAME1, VALID, LOCKED);
        final var account2 = deviceAccount(ACCOUNT_ID2, USER_ID1, DEVICE_ID1, USERNAME2, VALID, UNLOCKED);
        doReturn(account1).when(navigationService).unlockAccount(USER_ID1, ACCOUNT_ID1, KEY);
        doReturn(account2).when(navigationService).unlockAccount(USER_ID1, ACCOUNT_ID2, KEY);

        assertThrows(IllegalArgumentException.class,
            () -> restService.unlockAccount(ACCOUNT_ID1, new UnlockRequest(), USER_CTX1)); // no key
        assertThrows(IllegalArgumentException.class,
            () -> restService.unlockAccount(ACCOUNT_ID1, new UnlockRequest(KEY), USER_CTX1)); // unlock failed
        assertEquals(account2, restService.unlockAccount(ACCOUNT_ID2, new UnlockRequest(KEY), USER_CTX1));
    }

    @Test
    void lockAccount() {
        final var account = deviceAccount(ACCOUNT_ID1, USER_ID1, DEVICE_ID1, USERNAME1, VALID, UNLOCKED);
        doReturn(account).when(navigationService).lockAccount(USER_ID1, ACCOUNT_ID1);
        assertEquals(account, restService.lockAccount(ACCOUNT_ID1, USER_CTX1));
    }
}
