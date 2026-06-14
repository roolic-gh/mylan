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

import static local.mylan.service.api.model.DeviceProtocol.NFS;
import static local.mylan.service.api.model.DeviceProtocol.SMB;
import static local.mylan.service.test.NavResourceTestUtils.assertAccount;
import static local.mylan.service.test.NavResourceTestUtils.assertAccountList;
import static local.mylan.service.test.NavResourceTestUtils.assertDeviceList;
import static local.mylan.service.test.NavResourceTestUtils.device;
import static local.mylan.service.test.NavResourceTestUtils.deviceAccount;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import local.mylan.service.api.NavResourceService;
import local.mylan.service.api.UserContext;
import local.mylan.service.api.exceptions.NoDataException;
import local.mylan.service.api.exceptions.UnauthorizedException;
import local.mylan.service.api.model.Device;
import local.mylan.service.api.model.DeviceAccount;
import local.mylan.service.api.model.User;
import local.mylan.service.rest.api.NavResourceRestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultNavResourceRestServiceTest {

    private static final Integer DEVICE_ID1 = 101;
    private static final Integer DEVICE_ID2 = 102;
    private static final String DEVICE_NAME1 = "NAME1";
    private static final String DEVICE_NAME2 = "NAME2";
    private static final String IP1 = "192.168.1.101";
    private static final String IP2 = "192.168.1.102";
    private static final String IP3 = "192.168.1.103";
    private static final Device DEVICE1 = device(DEVICE_ID1, DEVICE_NAME1, SMB, List.of(IP1), null);
    private static final Device DEVICE2 = device(DEVICE_ID2, DEVICE_NAME2, NFS, List.of(IP2), null);

    private static final Integer USER_ID1 = 501;
    private static final Integer USER_ID2 = 502;
    private static final User USER1 = new User(USER_ID1, "user1", "USR1", false);
    private static final User USER2 = new User(USER_ID2, "user2", "USR2", false);
    private static final User GUEST = new User("", "", false);
    private static final UserContext USER_CTX1 = new UserContext(USER1, "u1");
    private static final UserContext USER_CTX2 = new UserContext(USER2, "u2");
    private static final UserContext GUEST_CTX = new UserContext(GUEST, null);

    private static final Integer ACCOUNT_ID1 = 601;
    private static final Integer ACCOUNT_ID2 = 602;
    private static final Integer ACCOUNT_ID_INAVLID = 606;

    private static final String USERNAME1 = "username-1";
    private static final String USERNAME2 = "username-1";
    private static final String PASSWORD1 = "PaS$W0rd1";
    private static final String PASSWORD2 = "Pa$SW0rd2";
    private static final String KEY1 = "k1";
    private static final String KEY2 = "k2";

    private static final DeviceAccount ACCOUNT1_IN =
        deviceAccount(ACCOUNT_ID1, USER_ID1, DEVICE_ID1, USERNAME1, PASSWORD1, KEY1);
    private static final DeviceAccount ACCOUNT1_OUT = deviceAccount(ACCOUNT_ID1, USER_ID1, DEVICE_ID1, USERNAME1);
    private static final DeviceAccount ACCOUNT2_IN =
        deviceAccount(ACCOUNT_ID2, USER_ID2, DEVICE_ID2, USERNAME2, PASSWORD2, KEY2);
    private static final DeviceAccount ACCOUNT2_OUT = deviceAccount(ACCOUNT_ID2, USER_ID2, DEVICE_ID2, USERNAME2);

    @Mock
    NavResourceService navResourceService;
    @Captor
    ArgumentCaptor<DeviceAccount> accountCaptor;

    NavResourceRestService restService;

    @BeforeEach
    void beforeEach() {
        restService = new DefaultNavResourceRestService(navResourceService);
    }

    @Test
    void listDevices() {
        final var devices = List.of(DEVICE1, DEVICE2);
        doReturn(devices).when(navResourceService).getAllDevices();
        assertDeviceList(devices, restService.getDevices());
    }

    @Test
    void listAccounts() {
        final var expected = List.of(ACCOUNT1_OUT);
        doReturn(expected).when(navResourceService).getUserAccounts(USER_ID1);
        assertAccountList(expected, restService.listDeviceAccounts(USER_CTX1));
    }

    @Test
    void getAccount() {
        doReturn(ACCOUNT1_OUT).when(navResourceService).getAccount(ACCOUNT_ID1);
        doReturn(ACCOUNT2_OUT).when(navResourceService).getAccount(ACCOUNT_ID2);
        doReturn(null).when(navResourceService).getAccount(ACCOUNT_ID_INAVLID);

        assertAccount(ACCOUNT1_OUT, restService.getDeviceAccount(ACCOUNT_ID1, USER_CTX1));
        assertAccount(ACCOUNT2_OUT, restService.getDeviceAccount(ACCOUNT_ID2, USER_CTX2));

        assertThrows(UnauthorizedException.class, () -> restService.getDeviceAccount(ACCOUNT_ID1, USER_CTX2));
        assertThrows(UnauthorizedException.class, () -> restService.getDeviceAccount(ACCOUNT_ID2, USER_CTX1));
        assertThrows(NoDataException.class, () -> restService.getDeviceAccount(ACCOUNT_ID_INAVLID, USER_CTX1));
    }

    @Test
    void createAccount() {
        final var newAccount = new DeviceAccount(DEVICE_ID1, USERNAME1, PASSWORD1, KEY1);
        doReturn(ACCOUNT1_OUT).when(navResourceService).createAccount(USER_ID1, newAccount);
        assertAccount(ACCOUNT1_OUT, restService.createDeviceAccount(newAccount, USER_CTX1));
    }

    @Test
    void updateAccount() {
        final var account = deviceAccount(null, null, DEVICE_ID1, USERNAME1, PASSWORD1, KEY1);
        doReturn(ACCOUNT1_OUT).when(navResourceService).updateAccount(USER_ID1, account);
        assertAccount(ACCOUNT1_OUT, restService.updateDeviceAccount(ACCOUNT_ID1, account, USER_CTX1));
        assertEquals(ACCOUNT_ID1, account.getAccountId()); // populated before invoking the service
    }

    @Test
    void deleteAccount() {
        restService.deleteDeviceAccount(ACCOUNT_ID1, USER_CTX1);
        verify(navResourceService, times(1)).removeAccount(eq(USER_ID1), eq(ACCOUNT_ID1));
    }
}
